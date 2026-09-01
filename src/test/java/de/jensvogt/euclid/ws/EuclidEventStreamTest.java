package de.jensvogt.euclid.ws;

import de.jensvogt.euclid.auth.SigV4;
import de.jensvogt.euclid.auth.SignableRequest;
import com.fasterxml.jackson.databind.JsonNode;
import de.jensvogt.euclid.testutil.FakeGatewayServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms EuclidEventStream authenticates its websocket handshake the way it claims to
 * (Bearer token or SigV4, mirroring the HTTP action-call path but signed as a GET), that
 * {@link EuclidEventStream#subscribe}/{@link EuclidEventStream#unsubscribe} actually send and
 * wait for acked subscribe/unsubscribe frames (the gateway's opt-in delivery mechanism - see
 * {@code GatewayWsRegistry::Broadcast()}), that {@link EuclidEventStream#awaitEvent} wakes as
 * soon as a matching event arrives, and that it times out cleanly when nothing matches or the
 * handshake is rejected.
 */
class EuclidEventStreamTest {

    @Test
    void awaitEventSubscribesAndWakesOnMatchingEventUsingBearerToken() throws Exception {
        try (FakeGatewayServer server = new FakeGatewayServer((headers, body) -> "{}")) {
            AtomicReference<Map<String, String>> handshakeHeaders = new AtomicReference<>();
            Thread pusher = new Thread(() -> {
                try {
                    handshakeHeaders.set(server.awaitHandshake(5));
                    server.awaitSubscription("eqs.message.sent", 5);
                    server.sendEventFrame("eqs.message.sent", Map.of("queueErn", "queue-1", "messageId", "msg-1"));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            pusher.start();

            EuclidEventStream stream = newClient(server, null, null);
            boolean matched = stream.awaitEvent("eqs.message.sent", Map.of("queueErn", "queue-1"), 5000);
            pusher.join();

            assertTrue(matched);
            assertEquals("Bearer test-token", handshakeHeaders.get().get("authorization"));
            stream.close();
        }
    }

    @Test
    void subscribeIsIdempotentAndDoesNotResendOnRepeatedAwaitEvent() throws Exception {
        try (FakeGatewayServer server = new FakeGatewayServer((headers, body) -> "{}")) {
            EuclidEventStream stream = newClient(server, null, null);

            // First call subscribes for real; the second, for the same topic/filter, must be a
            // no-op - proven by it still completing well within the timeout even though nothing
            // pushes an event, i.e. it isn't blocked waiting on a second subscribe ack that the
            // (single-subscription) fake server would never send.
            stream.awaitEvent("eqs.message.sent", Map.of("queueErn", "queue-1"), 200);
            boolean matched = stream.awaitEvent("eqs.message.sent", Map.of("queueErn", "queue-1"), 200);

            assertFalse(matched);
            stream.close();
        }
    }

    @Test
    void awaitEventReturnsFalseOnTimeoutWhenNoEventArrives() throws Exception {
        try (FakeGatewayServer server = new FakeGatewayServer((headers, body) -> "{}")) {
            EuclidEventStream stream = newClient(server, null, null);

            boolean matched = stream.awaitEvent("eqs.message.sent", Map.of(), 300);

            assertFalse(matched);
            stream.close();
        }
    }

    @Test
    void unmatchedEventIsNotDeliveredByTheSubscriptionAwareServer() throws Exception {
        try (FakeGatewayServer server = new FakeGatewayServer((headers, body) -> "{}")) {
            Thread pusher = new Thread(() -> {
                try {
                    server.awaitSubscription("eqs.message.sent", 5);
                    // Neither matches the subscribed filter (queueErn=queue-1); the fake server's
                    // own subscription-aware sendEventFrame must not deliver either one.
                    server.sendEventFrame("eqs.message.sent", Map.of("queueErn", "other-queue"));
                    server.sendEventFrame("eqs.message.deleted", Map.of("queueErn", "queue-1"));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            pusher.start();

            EuclidEventStream stream = newClient(server, null, null);
            boolean matched = stream.awaitEvent("eqs.message.sent", Map.of("queueErn", "queue-1"), 300);
            pusher.join();

            assertFalse(matched);
            stream.close();
        }
    }

    @Test
    void clientFiltersDefensivelyEvenIfAMismatchedFrameArrives() throws Exception {
        // sendRawFrame() bypasses the fake server's own subscription matching entirely, so this
        // proves EuclidEventStream itself re-checks the filter rather than trusting the server.
        try (FakeGatewayServer server = new FakeGatewayServer((headers, body) -> "{}")) {
            Thread pusher = new Thread(() -> {
                try {
                    server.awaitSubscription("eqs.message.sent", 5);
                    server.sendRawFrame("{\"type\":\"event\",\"id\":\"1\",\"topic\":\"eqs.message.sent\","
                            + "\"accountId\":\"863459426936\",\"region\":\"eu-central-1\","
                            + "\"body\":{\"queueErn\":\"other-queue\"}}");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            pusher.start();

            EuclidEventStream stream = newClient(server, null, null);
            boolean matched = stream.awaitEvent("eqs.message.sent", Map.of("queueErn", "queue-1"), 300);
            pusher.join();

            assertFalse(matched);
            stream.close();
        }
    }

    @Test
    void unsubscribeStopsFurtherDeliveryOfPreviouslyMatchingEvents() throws Exception {
        try (FakeGatewayServer server = new FakeGatewayServer((headers, body) -> "{}")) {
            EuclidEventStream stream = newClient(server, null, null);
            Map<String, String> filter = Map.of("queueErn", "queue-1");

            stream.subscribe("eqs.message.sent", filter);
            server.awaitSubscription("eqs.message.sent", 5);
            stream.unsubscribe("eqs.message.sent", filter);

            server.sendEventFrame("eqs.message.sent", Map.of("queueErn", "queue-1"));
            boolean matched = stream.awaitEvent("eqs.message.sent", filter, 300);

            // unsubscribe() itself re-subscribes nothing; awaitEvent() above subscribes again
            // (a fresh subscription), so the event sent *before* that re-subscribe must have
            // been missed - proving the unsubscribe in between actually took effect server-side.
            assertFalse(matched);
            stream.close();
        }
    }

    @Test
    void connectSignsHandshakeWithSigV4WhenAccessKeyConfigured() throws Exception {
        String accessKeyId = "AKIDEXAMPLE";
        String secretAccessKey = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";

        try (FakeGatewayServer server = new FakeGatewayServer((headers, body) -> "{}")) {
            AtomicReference<Map<String, String>> handshakeHeaders = new AtomicReference<>();
            Thread pusher = new Thread(() -> {
                try {
                    handshakeHeaders.set(server.awaitHandshake(5));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            pusher.start();

            EuclidEventStream stream = newClient(server, accessKeyId, secretAccessKey);
            stream.awaitEvent("eqs.message.sent", Map.of(), 300);
            pusher.join();

            Map<String, String> headers = handshakeHeaders.get();
            String authorization = headers.get("authorization");
            assertTrue(authorization.startsWith("AWS4-HMAC-SHA256 "));

            SignableRequest req = new SignableRequest("GET", "/");
            headers.forEach(req::header);

            Optional<SigV4.VerifyResult> result = SigV4.verify(req,
                    id -> id.equals(accessKeyId) ? Optional.of(secretAccessKey) : Optional.empty());
            assertTrue(result.isPresent(), "server-side verification of the client's own handshake signature must succeed");
            assertEquals(accessKeyId, result.get().accessKeyId());
            stream.close();
        }
    }

    @Test
    void awaitEventThrowsIOExceptionWhenHandshakeIsRejected() throws Exception {
        // A server that never upgrades - rejects every request with a plain 404, unlike
        // FakeGatewayServer which always completes the websocket handshake it's asked for.
        try (java.net.ServerSocket serverSocket = new java.net.ServerSocket(0)) {
            Thread rejecter = new Thread(() -> {
                try (java.net.Socket socket = serverSocket.accept()) {
                    socket.getOutputStream().write(
                            "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                                    .getBytes(java.nio.charset.StandardCharsets.US_ASCII));
                    socket.getOutputStream().flush();
                } catch (Exception ignored) {
                    // connection torn down once the client gives up
                }
            });
            rejecter.start();

            EuclidEventStream stream = new EuclidEventStream("http://localhost:" + serverSocket.getLocalPort(),
                    "test-token", "eu-central-1", "863459426936", "alice", null, null, null, "eqs");

            assertThrows(java.io.IOException.class, () -> stream.awaitEvent("eqs.message.sent", Map.of(), 1000));
            rejecter.join();
        }
    }

    /**
     * The defect this guards against: delivery is per connection on the gateway, so a stream that
     * reconnects without saying again what it is subscribed to ends up connected, healthy-looking
     * and permanently silent - events pile up in the store and nothing is logged.
     */
    @Test
    void replaysItsSubscriptionsOnTheConnectionThatReplacesADroppedOne() throws Exception {
        try (FakeGatewayServer server = new FakeGatewayServer((headers, body) -> "{}")) {
            EuclidEventStream stream = newClient(server, null, null);
            CountDownLatch delivered = new CountDownLatch(1);
            stream.addListener(new EventStreamListener() {
                @Override
                public void onEvent(String topic, JsonNode body) {
                    delivered.countDown();
                }
            });

            stream.subscribe("esm.object.created", Map.of("bucketName", "invoices"));
            server.awaitSubscriptionCount("esm.object.created", 1, 5);

            // What an idle timeout or a gateway restart does. The fake server forgets the
            // subscription with the connection, exactly as GatewayWsRegistry does.
            server.dropConnections();

            // A second connection carrying the same subscription is the whole assertion: without
            // the replay the client reconnects (or does not) and is attached to nothing.
            server.awaitSubscriptionCount("esm.object.created", 1, 15);
            server.sendEventFrame("esm.object.created", Map.of("bucketName", "invoices"));

            assertTrue(delivered.await(10, TimeUnit.SECONDS),
                    "no event delivered after the connection was replaced");
            stream.close();
        }
    }

    /**
     * A durable listener is told to go and look, because nothing could be pushed to it while it
     * was disconnected - the events it missed are in the store, not on the wire.
     */
    @Test
    void tellsListenersItReconnected() throws Exception {
        try (FakeGatewayServer server = new FakeGatewayServer((headers, body) -> "{}")) {
            EuclidEventStream stream = newClient(server, null, null);
            CountDownLatch reconnected = new CountDownLatch(1);
            stream.addListener(new EventStreamListener() {
                @Override
                public void onReconnected() {
                    reconnected.countDown();
                }
            });

            stream.subscribe("esm.object.created", Map.of());
            server.awaitSubscriptionCount("esm.object.created", 1, 5);

            server.dropConnections();

            assertTrue(reconnected.await(15, TimeUnit.SECONDS), "listeners were not told about the reconnect");
            stream.close();
        }
    }

    /**
     * Closing is closing: a stream that was shut down deliberately must not keep dialling the
     * gateway in the background.
     */
    @Test
    void doesNotReconnectAfterClose() throws Exception {
        try (FakeGatewayServer server = new FakeGatewayServer((headers, body) -> "{}")) {
            EuclidEventStream stream = newClient(server, null, null);
            stream.subscribe("esm.object.created", Map.of());
            server.awaitSubscriptionCount("esm.object.created", 1, 5);

            stream.close();
            server.dropConnections();
            Thread.sleep(2500);

            assertThrows(IOException.class, () -> stream.subscribe("esm.object.updated", Map.of()),
                    "a closed stream should refuse to reconnect");
        }
    }

    private static EuclidEventStream newClient(FakeGatewayServer server, String accessKeyId, String secretAccessKey) {
        return new EuclidEventStream("http://localhost:" + server.port(), "test-token", "eu-central-1",
                "863459426936", "alice", accessKeyId, secretAccessKey, null, "eqs");
    }
}
