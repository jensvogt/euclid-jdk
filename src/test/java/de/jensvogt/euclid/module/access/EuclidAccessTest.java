package de.jensvogt.euclid.module.access;

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

class EuclidAccessTest {

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

        EuclidSession session = EuclidAccess.forServer(baseUrl())
                .username("jens")
                .password("s3cret")
                .login();

        assertEquals("abc123", session.token());
        assertTrue(receivedBody.get().contains("\"username\":\"jens\""));
        assertTrue(receivedBody.get().contains("\"password\":\"s3cret\""));
    }

    @Test
    void loginThrowsAuthenticationExceptionForRejectedCredentials() throws Exception {
        server = startServer("/login", exchange -> sendResponse(exchange, 401, "{\"error\":\"invalid credentials\"}"));

        EuclidAccess access = EuclidAccess.forServer(baseUrl())
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
