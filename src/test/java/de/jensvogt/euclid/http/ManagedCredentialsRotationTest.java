package de.jensvogt.euclid.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.jensvogt.euclid.auth.CredentialsFileTokens;
import de.jensvogt.euclid.module.ens.EuclidEns;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The reported failure, end to end: an application euclid deployed publishes to a topic, euclid
 * rotates the token in its credentials file, and the application goes on publishing.
 *
 * <p>What was happening instead was
 * {@code EuclidServiceException: ens publish-message failed with status 401: Bearer token expired}
 * about an hour into a busy period - long enough that the pool had not been scaled down and
 * restarted with fresh credentials, which is why it looked like a load problem rather than a clock
 * one. The client held the token string it was constructed with, and nothing could replace it.
 *
 * <p>These install the reader explicitly, the way a caller would outside a deployed application.
 * The decision a client makes for itself - follow the file, or keep the token it was given - is
 * covered in {@code CredentialsFileTokensTest}, since it turns on an environment variable a test
 * JVM cannot set for itself.
 */
class ManagedCredentialsRotationTest {

    @TempDir
    Path directory;

    private HttpServer server;
    private final List<String> authorizations = new CopyOnWriteArrayList<>();

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void aRotationBetweenCallsIsPickedUpWithoutTheApplicationEverSeeingA401() throws Exception {
        server = startServer(List.of(
                new Answer(200, "{\"messageId\":\"msg-1\"}"),
                new Answer(200, "{\"messageId\":\"msg-2\"}")));

        final Path credentials = writeCredentials("token-before-rotation");
        final EuclidEns ens = client();
        ens.token(CredentialsFileTokens.forFile(credentials));

        assertEquals("msg-1", ens.publishMessage("ern:topic", "first").messageId());

        // Half an hour later, as far as the manager is concerned.
        writeCredentials("token-after");

        assertEquals("msg-2", ens.publishMessage("ern:topic", "second").messageId());

        assertEquals(List.of("Bearer token-before-rotation", "Bearer token-after"), authorizations,
                     "the call after a rotation has to carry the rotated token, not the one the client was built with");
    }

    @Test
    void aTokenThatExpiredInFlightIsReadAgainFromTheFileAndTheCallSucceeds() throws Exception {
        final Path credentials = writeCredentials("token-before-rotation");

        // The rotation happens while the request is in flight, which is the only way this failure
        // arises: the header was built with a token that was still good, and by the time the
        // gateway read it, it was not. Rotating before the call instead would mean both attempts
        // read the same token - and the client would rightly decline to send it twice.
        server = startServer(List.of(
                new Answer(401, "{\"error\":\"Bearer token expired\"}", () -> writeCredentialsUnchecked("token-after")),
                new Answer(200, "{\"messageId\":\"msg-1\"}")));

        final EuclidEns ens = client();
        ens.token(CredentialsFileTokens.forFile(credentials));

        assertEquals("msg-1", ens.publishMessage("ern:topic", "hello").messageId());

        assertEquals(List.of("Bearer token-before-rotation", "Bearer token-after"), authorizations,
                     "the retry has to read the file again rather than resend what was just rejected");
    }

    /**
     * Writes the credentials file the way euclid does: to a temporary name, then renamed over the
     * target, so a reader never sees half of one.
     *
     * <p>The tokens differ in length as well as content, so the file's size changes and the test
     * does not have to wait out the one-second modification-time granularity of some filesystems.
     */
    private Path writeCredentials(String token) throws IOException {
        final Path path = directory.resolve("credentials");
        final Path temporary = directory.resolve("credentials.new");
        Files.writeString(temporary, """
                {"token":"%s","expiresAt":"2026-09-02T12:00:00Z","userId":"alice",\
                "accountId":"000000000000","region":"eu-central-1"}""".formatted(token));
        Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        return path;
    }

    private void writeCredentialsUnchecked(String token) {
        try {
            writeCredentials(token);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private EuclidEns client() {
        return new EuclidEns(baseUrl(), "unused", "eu-central-1", "000000000000", "alice", null, null, null, null);
    }

    private HttpServer startServer(List<Answer> answers) throws IOException {
        final List<Answer> remaining = new ArrayList<>(answers);
        final HttpHandler handler = exchange -> {
            authorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
            final Answer answer = remaining.isEmpty() ? new Answer(500, "{\"error\":\"unscripted request\"}")
                                                      : remaining.remove(0);
            // Runs before the answer goes out, so a test can make the world change underneath a
            // request that is already on its way.
            answer.before().run();
            sendResponse(exchange, answer.status(), answer.body());
        };

        final HttpServer httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        httpServer.createContext("/", handler);
        httpServer.start();
        return httpServer;
    }

    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    private static void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private record Answer(int status, String body, Runnable before) {

        Answer(int status, String body) {
            this(status, body, () -> {});
        }
    }
}
