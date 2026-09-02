package de.jensvogt.euclid.auth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.jensvogt.euclid.module.eap.EuclidEap;
import de.jensvogt.euclid.module.ees.EuclidEes;
import de.jensvogt.euclid.module.ekm.EuclidEkm;
import de.jensvogt.euclid.module.ens.EuclidEns;
import de.jensvogt.euclid.module.eqs.EuclidEqs;
import de.jensvogt.euclid.module.esm.EuclidEsm;
import de.jensvogt.euclid.module.ets.EuclidEts;
import de.jensvogt.euclid.ws.EuclidEventStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Replacing a client's bearer token while it is in use.
 *
 * <p>A token expires; a client built with one holds it for as long as it lives. That is fine for a
 * process shorter-lived than its token and wrong for a server, which is why a client can be handed
 * a supplier instead - it is asked per request, so a caller renewing the token in the background
 * needs no new clients.
 */
class TokenRefreshableTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void theSupplierIsAskedOnEveryRequest() throws Exception {
        List<String> authorizations = new ArrayList<>();
        server = startServer(exchange -> {
            authorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
            sendResponse(exchange, "{\"queues\":[],\"total\":0}");
        });

        AtomicReference<String> current = new AtomicReference<>("first-token");
        EuclidEqs eqs = new EuclidEqs(baseUrl(), "constructed-with", "eu-central-1", "863459426936", "alice",
                                      null, null, null, null);
        eqs.token(current::get);

        eqs.listQueues();
        current.set("renewed-token");
        eqs.listQueues();

        // The second call carries the renewed token although nothing was rebuilt - the point of
        // the whole exercise, and what an application whose token file is rewritten hourly needs.
        assertEquals(List.of("Bearer first-token", "Bearer renewed-token"), authorizations);
    }

    @Test
    void aFixedTokenReplacesTheOneTheClientWasBuiltWith() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        server = startServer(exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            sendResponse(exchange, "{\"queues\":[],\"total\":0}");
        });

        EuclidEqs eqs = new EuclidEqs(baseUrl(), "stale-token", "eu-central-1", "863459426936", "alice",
                                      null, null, null, null);
        eqs.token("renewed-token");
        eqs.listQueues();

        assertEquals("Bearer renewed-token", authorization.get());
    }

    @Test
    void aClientWithAnAccessKeyNeverAsksForATokenAtAll() throws Exception {
        server = startServer(exchange -> sendResponse(exchange, "{\"queues\":[],\"total\":0}"));

        AtomicInteger asked = new AtomicInteger();
        EuclidEqs eqs = new EuclidEqs(baseUrl(), null, "eu-central-1", "863459426936", "alice",
                                      "AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY", null, null);
        eqs.token(() -> {
            asked.incrementAndGet();
            return "never-used";
        });

        eqs.listQueues();

        // Signing is the stronger scheme and the client prefers it. A supplier that reads a file
        // or calls a server must not be woken up for a request that was never going to use it.
        assertEquals(0, asked.get());
    }

    @Test
    void aSupplierIsRequired() {
        EuclidEqs eqs = new EuclidEqs("http://localhost:1", "token", "eu-central-1", "863459426936", "alice",
                                      null, null, null, null);

        assertThrows(NullPointerException.class, () -> eqs.token((java.util.function.Supplier<String>) null));
    }

    @Test
    void everyClientThatPresentsATokenCanHaveItReplaced() {
        // Nothing here asserts behaviour the tests above do not; it asserts coverage. A client
        // left out of the interface is one an application euclid deployed cannot keep using past
        // its first token, and that is not visible from the client's own tests.
        List<Object> clients = List.of(
                new EuclidEqs(null, "t", null, null, null, null, null, null, null),
                new EuclidEsm(null, "t", null, null, null, null, null, null, null),
                new EuclidEns(null, "t", null, null, null, null, null, null, null),
                new EuclidEes(null, "t", null, null, null, null, null, null, null),
                new EuclidEkm(null, "t", null, null, null, null, null, null, null),
                new EuclidEts(null, "t", null, null, null, null, null, null, null),
                new EuclidEap(null, "t", null, null, null, null, null, null, null),
                new EuclidEventStream(null, "t", null, null, null, null, null, null, "ees"));

        for (Object client : clients) {
            assertTrue(client instanceof TokenRefreshable, client.getClass().getSimpleName() + " cannot be refreshed");
        }
    }

    private HttpServer startServer(HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", handler);
        server.start();
        return server;
    }

    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    private static void sendResponse(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (var os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
