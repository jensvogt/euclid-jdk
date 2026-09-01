package de.jensvogt.euclid.module.ees;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.jensvogt.euclid.auth.SigV4;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.dto.ees.AckEventsResponse;
import de.jensvogt.euclid.dto.ees.ListSubscriptionsResponse;
import de.jensvogt.euclid.dto.ees.ReceiveEventsResponse;
import de.jensvogt.euclid.dto.ees.SubscribeEventsResponse;
import de.jensvogt.euclid.dto.ees.UnsubscribeEventsResponse;
import de.jensvogt.euclid.dto.ees.model.Event;
import de.jensvogt.euclid.dto.ees.model.EventSubscription;
import de.jensvogt.euclid.exception.EuclidServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms EuclidEes authenticates the way it claims to (SigV4-signed when an access key is
 * configured, bearer token otherwise, mirroring euclid-cli's HttpClient.cpp), routes every event
 * action to the right request with a correctly-shaped body, parses the corresponding response, and
 * surfaces non-2xx responses as {@link EuclidServiceException}.
 */
class EuclidEesTest {

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
    void subscribeSignsWithSigV4WhenAccessKeyConfigured() throws Exception {
        String accessKeyId = "AKIDEXAMPLE";
        String secretAccessKey = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";

        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"subscriptions\":[" + subscriptionJson() + "]}");
        });

        EuclidEes ees = new EuclidEes(baseUrl(), "unused-token", "eu-central-1", "863459426936", "alice",
                accessKeyId, secretAccessKey, null, null);
        ees.subscribeEvents("billing", List.of("esm.object.modified"));

        SignableRequest req = received.get();
        assertTrue(req.header("authorization").startsWith("AWS4-HMAC-SHA256 "));

        Optional<SigV4.VerifyResult> result = SigV4.verify(req,
                id -> id.equals(accessKeyId) ? Optional.of(secretAccessKey) : Optional.empty());
        assertTrue(result.isPresent(), "server-side verification of the client's own signature must succeed");
        assertEquals(accessKeyId, result.get().accessKeyId());
    }

    @Test
    void subscribeUsesBearerTokenWhenNoAccessKeyConfigured() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"subscriptions\":[]}");
        });

        newClient().subscribeEvents("billing", List.of("esm.object.modified"));

        assertEquals("Bearer test-token", received.get().header("authorization"));
    }

    // Watching one bucket is a filter on that bucket's ERN - the filter is matched at publish time,
    // so it decides what the subscriber accumulates rather than what it sees on receive.
    @Test
    void subscribeSendsTheEventTypesAndFilter() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"subscriptions\":[" + subscriptionJson() + "]}");
        });

        SubscribeEventsResponse response = newClient().subscribeEvents("billing",
                List.of("esm.object.modified", "esm.object.deleted"),
                Map.of("bucketErn", "ern:esm:eu-central-1:863459426936:bucket/invoices"));

        assertEquals("subscribe-events", received.get().header("x-euclid-action"));
        assertEquals("ees", received.get().header("x-euclid-target"));
        assertBodyContains(received.get().body(), "\"name\":\"billing\"",
                "\"eventTypes\":[\"esm.object.modified\",\"esm.object.deleted\"]",
                "\"filter\":{\"bucketErn\":\"ern:esm:eu-central-1:863459426936:bucket/invoices\"}");

        EventSubscription subscription = response.subscriptions().getFirst();
        assertEquals("billing", subscription.subscriber());
        assertEquals("esm.object.modified", subscription.eventType());
        assertEquals("ern:esm:eu-central-1:863459426936:bucket/invoices", subscription.filter().get("bucketErn"));
    }

    // An empty filter really does mean every event of these types, so it still has to be sent.
    @Test
    void subscribeWithoutAFilterSendsAnEmptyOne() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"subscriptions\":[]}");
        });

        newClient().subscribeEvents("billing", List.of("esm.object.modified"));

        assertBodyContains(received.get().body(), "\"filter\":{}");
    }

    @Test
    void unsubscribeWithoutAnEventTypeRemovesTheSubscriberEntirely() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"subscriber\":\"billing\",\"removed\":2}");
        });

        UnsubscribeEventsResponse response = newClient().unsubscribeEvents("billing");

        assertEquals("unsubscribe-events", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"name\":\"billing\"", "\"eventType\":\"\"");
        assertEquals(2, response.removed());
    }

    @Test
    void unsubscribeWithAnEventTypeNarrowsTheSubscription() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"subscriber\":\"billing\",\"removed\":1}");
        });

        newClient().unsubscribeEvents("billing", "esm.object.deleted");

        assertBodyContains(received.get().body(), "\"eventType\":\"esm.object.deleted\"");
    }

    @Test
    void listSubscriptionsParsesSubscriptionsAndWaitingCount() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"subscriptions\":[" + subscriptionJson() + "],\"waiting\":7}");
        });

        ListSubscriptionsResponse response = newClient().listSubscriptions("billing");

        assertEquals("list-subscriptions", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"name\":\"billing\"");
        assertEquals(1, response.subscriptions().size());
        assertEquals(7, response.waiting());
        assertEquals("863459426936", response.subscriptions().getFirst().accountId());
    }

    @Test
    void receiveEventsUsesDefaultsAndParsesEnvelopes() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"events\":[" + eventJson() + "],\"total\":1}");
        });

        ReceiveEventsResponse response = newClient().receiveEvents("billing");

        assertEquals("receive-events", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"name\":\"billing\"", "\"maxEvents\":10",
                "\"waitTime\":0", "\"visibilityTimeout\":300");

        assertEquals(1, response.total());
        Event event = response.events().getFirst();
        assertEquals("evt-1", event.eventId());
        assertEquals("esm.object.modified", event.eventType());
        assertEquals("esm", event.sourceModule());
        assertEquals(1, event.attempts());
        // The payload's shape is decided by the event type, so it stays a map of raw JSON values.
        assertEquals("ern:esm:eu-central-1:863459426936:bucket/invoices", event.payload().get("bucketErn"));
        assertEquals("invoice-1.pdf", event.payload().get("key"));
        assertEquals(1024, event.payload().get("size"));
    }

    @Test
    void receiveEventsWithLongPollingSendsTheWaitAndVisibility() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"events\":[],\"total\":0}");
        });

        ReceiveEventsResponse response = newClient().receiveEvents("billing", 50, 20, 60);

        assertBodyContains(received.get().body(), "\"maxEvents\":50", "\"waitTime\":20",
                "\"visibilityTimeout\":60");
        assertTrue(response.events().isEmpty());
    }

    // A redelivery is visible in the envelope: attempts above one means an earlier claim was never
    // acknowledged and the visibility timeout brought the event back.
    @Test
    void receiveEventsSurfacesTheAttemptCount() throws Exception {
        server = startServer(exchange -> {
            captureRequest(exchange);
            sendResponse(exchange, 200, "{\"events\":[" + eventJson(3) + "],\"total\":1}");
        });

        assertEquals(3, newClient().receiveEvents("billing").events().getFirst().attempts());
    }

    @Test
    void ackEventsSendsTheIdsAndParsesCounts() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"subscriber\":\"billing\",\"acknowledged\":2,\"waiting\":5}");
        });

        AckEventsResponse response = newClient().ackEvents("billing", List.of("evt-1", "evt-2"));

        assertEquals("ack-events", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"name\":\"billing\"",
                "\"eventIds\":[\"evt-1\",\"evt-2\"]");
        assertEquals(2, response.acknowledged());
        assertEquals(5, response.waiting());
    }

    // Acknowledging one event is the common case after processing it.
    @Test
    void ackEventSendsASingleId() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"subscriber\":\"billing\",\"acknowledged\":1,\"waiting\":0}");
        });

        assertEquals(1, newClient().ackEvent("billing", "evt-1").acknowledged());
        assertBodyContains(received.get().body(), "\"eventIds\":[\"evt-1\"]");
    }

    // An event that is already gone is not an error, so acknowledged can be lower than the number
    // of IDs passed - a redelivery acked twice and an expired event mean the same thing here.
    @Test
    void ackEventsToleratesEventsThatAreAlreadyGone() throws Exception {
        server = startServer(exchange -> {
            captureRequest(exchange);
            sendResponse(exchange, 200, "{\"subscriber\":\"billing\",\"acknowledged\":1,\"waiting\":0}");
        });

        assertEquals(1, newClient().ackEvents("billing", List.of("evt-1", "evt-gone")).acknowledged());
    }

    @Test
    void namespaceIsSentWhenTheSessionIsScoped() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"subscriptions\":[],\"waiting\":0}");
        });

        new EuclidEes(baseUrl(), "test-token", "eu-central-1", "863459426936", "alice", null, null, null, "prod")
                .listSubscriptions("billing");

        assertEquals("prod", received.get().header("x-euclid-namespace"));
    }

    @Test
    void nonSuccessResponseThrowsEuclidServiceException() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 400, "{\"error\":\"eventTypes is required\"}");
        });

        EuclidEes ees = newClient();
        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> ees.subscribeEvents("billing", List.of()));

        assertEquals("ees", exception.service());
        assertEquals("subscribe-events", exception.action());
        assertEquals(400, exception.statusCode());
        assertTrue(exception.responseBody().contains("eventTypes is required"));
    }

    private static String subscriptionJson() {
        return "{\"subscriber\":\"billing\",\"eventType\":\"esm.object.modified\","
                + "\"filter\":{\"bucketErn\":\"ern:esm:eu-central-1:863459426936:bucket/invoices\"},"
                + "\"accountId\":\"863459426936\",\"created\":\"2026-01-01\",\"lastSeen\":\"2026-01-02\"}";
    }

    private static String eventJson() {
        return eventJson(1);
    }

    private static String eventJson(int attempts) {
        return "{\"eventId\":\"evt-1\",\"eventType\":\"esm.object.modified\",\"sourceModule\":\"esm\","
                + "\"payload\":{\"ern\":\"ern:esm:eu-central-1:863459426936:object/invoice-1.pdf\","
                + "\"bucketErn\":\"ern:esm:eu-central-1:863459426936:bucket/invoices\","
                + "\"key\":\"invoice-1.pdf\",\"size\":1024},"
                + "\"attempts\":" + attempts + ",\"created\":\"2026-01-01\"}";
    }

    private EuclidEes newClient() {
        return new EuclidEes(baseUrl(), "test-token", "eu-central-1", "863459426936", "alice", null, null, null, null);
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
