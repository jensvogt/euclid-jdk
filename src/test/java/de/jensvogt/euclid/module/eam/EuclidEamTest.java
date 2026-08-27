package de.jensvogt.euclid.module.eam;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.jensvogt.euclid.exception.EuclidAuthenticationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EuclidEamTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
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
