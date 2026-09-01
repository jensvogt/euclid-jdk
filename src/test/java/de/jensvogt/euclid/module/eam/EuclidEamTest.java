package de.jensvogt.euclid.module.eam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.jensvogt.euclid.exception.EuclidAuthenticationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EuclidEamTest {

    private HttpServer server;

    // A successful login writes $HOME/.euclid/credentials - the very file a real euclid-cli or SDK
    // login on this machine lives in. Pointing "user.home" at a temp directory for the duration of
    // each test keeps the suite from overwriting it with a test token.
    @TempDir
    Path fakeHome;

    private String realUserHome;

    @BeforeEach
    void redirectHome() {
        realUserHome = System.getProperty("user.home");
        System.setProperty("user.home", fakeHome.toString());
    }

    @AfterEach
    void stopServer() {
        System.setProperty("user.home", realUserHome);
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void loginCarriesIsAdminFromTheServer() throws Exception {
        server = startServer("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, "{\"token\":\"abc123\",\"isAdmin\":true,"
                    + "\"metadata\":{\"region\":\"eu-central-1\",\"accountId\":\"863459426936\",\"user\":\"jens\"}}");
        });

        EuclidSession session = EuclidEam.forServer(baseUrl()).username("jens").password("s3cret").login();

        assertTrue(session.isAdmin());
        assertEquals("jens", session.userId());
        assertEquals("863459426936", session.accountId());
        assertEquals("eu-central-1", session.region());
    }

    // The credentials file is shared with euclid-cli, which reads the namespace from "namespace"
    // and expects "isAdmin" alongside the token - a mismatch here silently loses the namespace when
    // the other client picks the session up.
    @Test
    void loginWritesCredentialsInTheFormatTheCliReads() throws Exception {
        server = startServer("/", exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            exchange.getRequestBody().readAllBytes();
            if ("login".equals(action)) {
                sendResponse(exchange, 200, "{\"token\":\"abc123\",\"isAdmin\":true,\"accessKeyId\":\"AKIA1\","
                        + "\"secretAccessKey\":\"s3cr3t\",\"metadata\":{\"region\":\"eu-central-1\","
                        + "\"accountId\":\"863459426936\",\"user\":\"jens\"}}");
            } else {
                sendResponse(exchange, 200, "{}");
            }
        });

        EuclidEam.forServer(baseUrl()).username("jens").password("s3cret").namespace("prod").login();

        JsonNode stored = new ObjectMapper().readTree(Files.readString(fakeHome.resolve(".euclid/credentials")));
        assertEquals("prod", stored.path("namespace").asText(), "the CLI reads the namespace from \"namespace\"");
        assertTrue(stored.path("isAdmin").asBoolean(), "the CLI expects isAdmin alongside the token");
        assertEquals("abc123", stored.path("token").asText());
        assertEquals("AKIA1", stored.path("accessKeyId").asText());
        assertEquals("jens", stored.path("userId").asText());
    }

    @Test
    void loginReturnsSessionWithTokenForValidCredentials() throws Exception {
        AtomicReference<String> receivedBody = new AtomicReference<>();
        server = startServer("/", exchange -> {
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendResponse(exchange, 200, "{\"token\":\"abc123\"}");
        });

        EuclidSession session = EuclidEam.forServer(baseUrl())
                .username("jens")
                .password("s3cret")
                .login();

        assertEquals("abc123", session.token());
        assertTrue(receivedBody.get().contains("\"userId\":\"jens\""));
        assertTrue(receivedBody.get().contains("\"password\":\"s3cret\""));
    }

    @Test
    void loginWithNamespaceAppliesActiveNamespace() throws Exception {
        AtomicReference<String> changeNamespaceBody = new AtomicReference<>();
        server = startServer("/", exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            if ("login".equals(action)) {
                exchange.getRequestBody().readAllBytes();
                sendResponse(exchange, 200, "{\"token\":\"abc123\"}");
            } else if ("change-namespace".equals(action)) {
                changeNamespaceBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                sendResponse(exchange, 200, "{}");
            } else {
                sendResponse(exchange, 500, "{\"error\":\"unexpected action " + action + "\"}");
            }
        });

        EuclidSession session = EuclidEam.forServer(baseUrl())
                .username("jens")
                .password("s3cret")
                .namespace("prod")
                .login();

        assertEquals("prod", session.nameSpace());
        assertTrue(changeNamespaceBody.get().contains("\"namespace\":\"prod\""));
    }

    // The server resolves the user by ID first and only falls back to the email, so a login that
    // identifies the account by email must not also send a user ID.
    @Test
    void loginWithEmailSendsTheEmailAndNoUserId() throws Exception {
        AtomicReference<String> receivedBody = new AtomicReference<>();
        server = startServer("/", exchange -> {
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendResponse(exchange, 200, "{\"token\":\"abc123\"}");
        });

        EuclidSession session = EuclidEam.forServer(baseUrl())
                .email("jens@example.com")
                .password("s3cret")
                .login();

        assertEquals("abc123", session.token());
        assertTrue(receivedBody.get().contains("\"email\":\"jens@example.com\""));
        assertTrue(receivedBody.get().contains("\"userId\":\"\""), "a userId would win over the email");
    }

    // A username set alongside an email wins, so the email is left out rather than sent and ignored.
    @Test
    void loginWithUsernameLeavesTheEmailOut() throws Exception {
        AtomicReference<String> receivedBody = new AtomicReference<>();
        server = startServer("/", exchange -> {
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendResponse(exchange, 200, "{\"token\":\"abc123\"}");
        });

        EuclidEam.forServer(baseUrl()).username("jens").email("jens@example.com").password("s3cret").login();

        assertTrue(receivedBody.get().contains("\"userId\":\"jens\""));
        assertTrue(receivedBody.get().contains("\"email\":\"\""));
    }

    @Test
    void loginRequiresAUsernameOrAnEmail() {
        EuclidEam access = EuclidEam.forServer("http://localhost:1").password("s3cret");

        NullPointerException exception = assertThrows(NullPointerException.class, access::login);
        assertTrue(exception.getMessage().contains("username or email"));
    }

    @Test
    void loginThrowsAuthenticationExceptionForRejectedCredentials() throws Exception {
        server = startServer("/login", exchange -> sendResponse(exchange, 401, "{\"error\":\"invalid credentials\"}"));

        EuclidEam access = EuclidEam.forServer(baseUrl())
                .loginPath("/login")
                .username("jens")
                .password("wrong");

        EuclidAuthenticationException exception = assertThrows(EuclidAuthenticationException.class, access::login);
        assertEquals(401, exception.statusCode());
    }

    private HttpServer startServer(String path, HttpHandler handler) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        httpServer.createContext(path, handler);
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
}
