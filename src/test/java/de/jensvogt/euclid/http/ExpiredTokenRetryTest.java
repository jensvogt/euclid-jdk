package de.jensvogt.euclid.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.jensvogt.euclid.exception.EuclidServiceException;
import de.jensvogt.euclid.module.ens.EuclidEns;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * An application euclid runs authenticates with a bearer token out of a credentials file the
 * manager rewrites before it expires. Between the moment a client reads that file and the moment
 * the server reads the header, the token can go stale - and what the application sees is not a
 * token problem but a lost business operation: "ens publish-message failed with status 401:
 * Bearer token expired".
 *
 * <p>These cover the narrow recovery for that: one more attempt, with the credentials built again,
 * and only when there is reason to believe the second attempt would differ.
 */
class ExpiredTokenRetryTest {

    private HttpServer server;
    private final List<String> authorizations = new CopyOnWriteArrayList<>();

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void aTokenThatExpiredInFlightIsReadAgainAndTheCallSucceeds() throws Exception {
        server = startServer(List.of(
                new Answer(401, "{\"error\":\"Bearer token expired\"}"),
                new Answer(200, "{\"messageId\":\"msg-1\"}")));

        // What CredentialsFileTokens does when the manager has rewritten the file underneath it.
        Iterator<String> tokens = List.of("expired-token", "fresh-token").iterator();
        EuclidEns ens = client();
        ens.token(tokens::next);

        assertEquals("msg-1", ens.publishMessage("ern:topic", "hello").messageId());

        assertEquals(List.of("Bearer expired-token", "Bearer fresh-token"), authorizations,
                     "the retry has to carry the token read after the rejection, not the one that was rejected");
    }

    @Test
    void aTokenNobodyRefreshedIsNotSentTwice() throws Exception {
        server = startServer(List.of(
                new Answer(401, "{\"error\":\"Bearer token expired\"}"),
                new Answer(200, "{\"messageId\":\"msg-1\"}")));

        EuclidEns ens = client();
        ens.token(() -> "expired-token");

        // Nothing rewrote the file - most likely because the manager that writes it is gone - so a
        // second attempt would carry the same expired token and fail identically. Better to report
        // the failure once than to double the load of an installation already in trouble.
        assertThrows(EuclidServiceException.class, () -> ens.publishMessage("ern:topic", "hello"));
        assertEquals(1, authorizations.size(), "the same token was sent twice");
    }

    @Test
    void anOrdinaryRejectionIsReportedRatherThanRetried() throws Exception {
        server = startServer(List.of(
                new Answer(401, "{\"error\":\"Access denied\"}"),
                new Answer(200, "{\"messageId\":\"msg-1\"}")));

        Iterator<String> tokens = List.of("token-one", "token-two").iterator();
        EuclidEns ens = client();
        ens.token(tokens::next);

        // A caller without permission gets that answer, not two attempts at it: retrying would
        // turn every rejection into two, and a repeated rejection is how an account gets locked.
        assertThrows(EuclidServiceException.class, () -> ens.publishMessage("ern:topic", "hello"));
        assertEquals(1, authorizations.size());
    }

    private EuclidEns client() {
        return new EuclidEns(baseUrl(), "unused", "eu-central-1", "000000000000", "alice", null, null, null, null);
    }

    /**
     * Answers requests from a fixed script, so a test can say what the second attempt sees, and
     * records the credentials each attempt carried.
     */
    private HttpServer startServer(List<Answer> answers) throws IOException {
        List<Answer> remaining = new ArrayList<>(answers);
        HttpHandler handler = exchange -> {
            authorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
            Answer answer = remaining.isEmpty() ? new Answer(500, "{\"error\":\"unscripted request\"}")
                                                : remaining.remove(0);
            sendResponse(exchange, answer.status(), answer.body());
        };

        HttpServer httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        httpServer.createContext("/", handler);
        httpServer.start();
        return httpServer;
    }

    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    private static void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private record Answer(int status, String body) {
    }
}
