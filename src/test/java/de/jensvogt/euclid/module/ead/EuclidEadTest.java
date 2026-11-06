package de.jensvogt.euclid.module.ead;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.jensvogt.euclid.auth.SigV4;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.dto.ead.CountEventsRequest;
import de.jensvogt.euclid.dto.ead.ListEventsRequest;
import de.jensvogt.euclid.dto.ead.ListEventsResponse;
import de.jensvogt.euclid.dto.ead.model.AuditEvent;
import de.jensvogt.euclid.exception.EuclidServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms EuclidEad authenticates the way it claims to (SigV4-signed when an access key is
 * configured, bearer token otherwise), addresses every action to the {@code ead} target with a
 * correctly-shaped body, parses the responses, and surfaces non-2xx responses as
 * {@link EuclidServiceException}.
 * <p>
 * The filter cases matter most: EAD reads an absent filter as "no filter", so a client that sent
 * its unset filters as null or empty strings would be asking a different question.
 */
class EuclidEadTest {

    private static final List<String> SIGNED_HEADERS = List.of("host", "x-amz-content-sha256", "x-amz-date",
            "x-euclid-account-id", "x-euclid-action", "x-euclid-region", "x-euclid-target", "x-euclid-user-id",
            "x-euclid-namespace");

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void listEventsSignsWithSigV4WhenAccessKeyConfigured() throws Exception {
        String accessKeyId = "AKIDEXAMPLE";
        String secretAccessKey = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";

        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"events\":[],\"total\":0,\"totalExact\":true}");
        });

        new EuclidEad(baseUrl(), "unused-token", "eu-central-1", "863459426936", "alice",
                accessKeyId, secretAccessKey, null, null).listEvents();

        SignableRequest req = received.get();
        assertTrue(req.header("authorization").startsWith("AWS4-HMAC-SHA256 "));
        Optional<SigV4.VerifyResult> result = SigV4.verify(req,
                id -> id.equals(accessKeyId) ? Optional.of(secretAccessKey) : Optional.empty());
        assertTrue(result.isPresent(), "server-side verification of the client's own signature must succeed");
        assertEquals("ead", req.header("x-euclid-target"));
    }

    @Test
    void listEventsUsesTheBearerTokenAndDefaultPaging() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"events\":[],\"total\":0,\"totalExact\":true}");
        });

        newClient().listEvents();

        SignableRequest req = received.get();
        assertEquals("Bearer test-token", req.header("authorization"));
        assertEquals("ead", req.header("x-euclid-target"));
        assertEquals("list-events", req.header("x-euclid-action"));
        assertBodyContains(req.body(), "\"pageSize\":10", "\"pageIndex\":0");
        // No filter given, so none is sent - an empty string would be a filter.
        assertFalse(req.body().contains("userId"), req.body());
        assertFalse(req.body().contains("module"), req.body());
        assertFalse(req.body().contains("command"), req.body());
    }

    @Test
    void listEventsSendsFiltersAndParsesTheEvents() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"events\":[" + eventJson() + "],\"total\":100000,\"totalExact\":false}");
        });

        ListEventsResponse response = newClient().listEvents(ListEventsRequest.builder().userId("bob")
                .module("esm").command("delete-bucket").pageSize(50).pageIndex(2).build());

        assertBodyContains(received.get().body(), "\"userId\":\"bob\"", "\"module\":\"esm\"",
                "\"command\":\"delete-bucket\"", "\"pageSize\":50", "\"pageIndex\":2");
        assertEquals(100000, response.total());
        assertFalse(response.totalExact());
        assertEquals(1, response.events().size());
        AuditEvent event = response.events().get(0);
        assertEquals("863459426936", event.accountId());
        assertEquals("prod", event.namespace());
        assertEquals("bob", event.userId());
        assertEquals("esm", event.module());
        assertEquals("delete-bucket", event.command());
        assertEquals("{\"bucket\":\"b1\",\"password\":\"***\"}", event.parameters());
        assertEquals(403, event.status());
        assertEquals("2026-11-01T10:00:00Z", event.created());
    }

    @Test
    void countEventsSendsTheFiltersAndReturnsTheCount() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"count\":4711}");
        });

        long count = newClient().countEvents(CountEventsRequest.builder().module("eqs").build());

        assertEquals("count-events", received.get().header("x-euclid-action"));
        assertEquals("{\"module\":\"eqs\"}", received.get().body());
        assertEquals(4711, count);
    }

    @Test
    void countEventsWithoutFiltersSendsAnEmptyObject() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"count\":3}");
        });

        assertEquals(3, newClient().countEvents());
        assertEquals("{}", received.get().body());
    }

    @Test
    void purgeEventsSendsTheAgeAndReturnsHowManyWereRemoved() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"removed\":12}");
        });

        long removed = newClient().purgeEvents(30);

        assertEquals("purge-events", received.get().header("x-euclid-action"));
        assertEquals("{\"olderThanDays\":30}", received.get().body());
        assertEquals(12, removed);
    }

    // A purge with no window would remove the whole trail; EAD refuses it.
    @Test
    void purgeEventsSurfacesTheRefusalOfANonPositiveAge() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 400, "{\"error\":\"olderThanDays is required and must be positive\"}");
        });

        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> newClient().purgeEvents(0));

        assertEquals("ead", exception.service());
        assertEquals("purge-events", exception.action());
        assertEquals(400, exception.statusCode());
    }

    @Test
    void aCallerWithoutThePermissionGetsA403() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 403, "{\"error\":\"not permitted\"}");
        });

        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> newClient().listEvents());

        assertEquals("list-events", exception.action());
        assertEquals(403, exception.statusCode());
    }

    @Test
    void namespaceIsSentWhenTheSessionIsScoped() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"count\":0}");
        });

        new EuclidEad(baseUrl(), "test-token", "eu-central-1", "863459426936", "alice", null, null, null, "prod")
                .countEvents();

        assertEquals("prod", received.get().header("x-euclid-namespace"));
    }

    private static String eventJson() {
        return "{\"accountId\":\"863459426936\",\"namespace\":\"prod\",\"userId\":\"bob\",\"module\":\"esm\","
                + "\"command\":\"delete-bucket\",\"parameters\":\"{\\\"bucket\\\":\\\"b1\\\",\\\"password\\\":\\\"***\\\"}\","
                + "\"status\":403,\"created\":\"2026-11-01T10:00:00Z\"}";
    }

    private EuclidEad newClient() {
        return new EuclidEad(baseUrl(), "test-token", "eu-central-1", "863459426936", "alice", null, null, null, null);
    }

    private static void assertBodyContains(String body, String... fragments) {
        for (String fragment : fragments) {
            assertTrue(body.contains(fragment), "expected body to contain " + fragment + " but was " + body);
        }
    }

    private static SignableRequest captureRequest(HttpExchange exchange) throws IOException {
        SignableRequest req = new SignableRequest(exchange.getRequestMethod(), exchange.getRequestURI().toString());
        Headers requestHeaders = exchange.getRequestHeaders();
        for (String name : SIGNED_HEADERS) {
            String value = requestHeaders.getFirst(name);
            if (value != null) {
                req.header(name, value);
            }
        }
        String authorization = requestHeaders.getFirst("Authorization");
        if (authorization != null) {
            req.header("authorization", authorization);
        }
        req.body(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        return req;
    }

    private HttpServer startServer(HttpHandler handler) throws IOException {
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
}
