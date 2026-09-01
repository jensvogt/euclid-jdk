package de.jensvogt.euclid.ws;

import de.jensvogt.euclid.dto.ees.model.DeliveryMode;
import de.jensvogt.euclid.dto.ees.model.Event;
import de.jensvogt.euclid.module.ees.EuclidEes;
import de.jensvogt.euclid.testutil.FakeGatewayServer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The durable loop, end to end against a fake gateway: the connection is told that something is
 * waiting, the listener claims it, hands it to the handler, and acknowledges only what the handler
 * got through.
 * <p>
 * The point of each test is a rule the application depends on - that it is told once and claims
 * everything, that a handler which throws leaves its event to come back, and that being told it
 * fell behind is an instruction to claim rather than a loss.
 */
class EuclidEventListenerTest {

    @Test
    void aNotifyFrameMakesTheListenerClaimHandleAndAcknowledge() throws Exception {
        List<String> actions = new CopyOnWriteArrayList<>();
        List<String> acknowledged = new CopyOnWriteArrayList<>();
        AtomicInteger claims = new AtomicInteger();

        try (FakeGatewayServer server = new FakeGatewayServer((headers, body) -> {
            String action = headers.get("x-euclid-action");
            actions.add(action);
            return switch (action) {
                case "subscribe-events" -> "{\"subscriptions\":[]}";
                // One event on the first claim, nothing after: a drain keeps claiming until the
                // store says there is no more.
                case "receive-events" -> claims.getAndIncrement() == 0
                        ? "{\"total\":1,\"events\":[{\"eventId\":\"e-1\",\"eventType\":\"esm.object.created\","
                          + "\"sourceModule\":\"esm\",\"attempts\":1,\"created\":\"2026-09-01T10:00:00Z\","
                          + "\"payload\":{\"key\":\"reports/q3.csv\",\"bucketName\":\"inbox\"}}]}"
                        : "{\"total\":0,\"events\":[]}";
                case "ack-events" -> {
                    acknowledged.add(body);
                    yield "{\"subscriber\":\"importer\",\"acknowledged\":1,\"waiting\":0}";
                }
                default -> "{}";
            };
        })) {
            CountDownLatch handled = new CountDownLatch(1);
            List<Event> received = new CopyOnWriteArrayList<>();

            EuclidEventStream stream = newStream(server);
            try (EuclidEventListener listener = EuclidEventListener.builder()
                    .ees(newEes(server)).stream(stream).name("importer")
                    .eventTypes(List.of("esm.object.created"))
                    .filter(Map.of("bucketName", "inbox"))
                    .handler(event -> {
                        received.add(event);
                        handled.countDown();
                    })
                    .build()) {

                listener.start();
                server.awaitHandshake(5);
                server.sendRawFrame("{\"type\":\"event\",\"id\":\"1\",\"topic\":\"esm.object.created\","
                                    + "\"accountId\":\"863459426936\",\"region\":\"eu-central-1\","
                                    + "\"body\":{\"delivery\":\"notify\",\"eventType\":\"esm.object.created\"}}");

                assertTrue(handled.await(5, TimeUnit.SECONDS), "the handler was never called");
            }
            stream.close();

            assertEquals(1, received.size());
            assertEquals("e-1", received.getFirst().eventId());
            assertEquals("reports/q3.csv", received.getFirst().payload().get("key"));
            assertTrue(actions.contains("subscribe-events"), "the subscription was never registered");
            assertEquals(1, acknowledged.size());
            assertTrue(acknowledged.getFirst().contains("e-1"), "the handled event was not acknowledged: " + acknowledged);
        }
    }

    @Test
    void anEventTheHandlerThrowsOnIsNotAcknowledged() throws Exception {
        List<String> acknowledged = new CopyOnWriteArrayList<>();
        AtomicInteger claims = new AtomicInteger();

        try (FakeGatewayServer server = new FakeGatewayServer((headers, body) -> switch (headers.get("x-euclid-action")) {
            case "subscribe-events" -> "{\"subscriptions\":[]}";
            case "receive-events" -> claims.getAndIncrement() == 0
                    ? "{\"total\":1,\"events\":[{\"eventId\":\"e-2\",\"eventType\":\"esm.object.created\","
                      + "\"sourceModule\":\"esm\",\"attempts\":1,\"created\":\"\",\"payload\":{}}]}"
                    : "{\"total\":0,\"events\":[]}";
            case "ack-events" -> {
                acknowledged.add(body);
                yield "{\"subscriber\":\"importer\",\"acknowledged\":0,\"waiting\":1}";
            }
            default -> "{}";
        })) {
            CountDownLatch attempted = new CountDownLatch(1);

            EuclidEventStream stream = newStream(server);
            try (EuclidEventListener listener = EuclidEventListener.builder()
                    .ees(newEes(server)).stream(stream).name("importer")
                    .eventTypes(List.of("esm.object.created"))
                    .handler(event -> {
                        attempted.countDown();
                        throw new IllegalStateException("import failed");
                    })
                    .build()) {

                listener.start();
                server.awaitHandshake(5);
                server.sendRawFrame("{\"type\":\"event\",\"id\":\"1\",\"topic\":\"esm.object.created\","
                                    + "\"accountId\":\"863459426936\",\"region\":\"eu-central-1\","
                                    + "\"body\":{\"delivery\":\"notify\"}}");

                assertTrue(attempted.await(5, TimeUnit.SECONDS), "the handler was never called");
                // Nothing to wait for on the other side, so give the drain a moment to finish and
                // prove it did not acknowledge anything rather than that it had not got there yet.
                Thread.sleep(500);
            }
            stream.close();

            // Unacknowledged means redelivered once the claim's visibility runs out, which is what
            // makes a handler that fails safe to write.
            assertTrue(acknowledged.isEmpty(), "a failed event was acknowledged anyway: " + acknowledged);
        }
    }

    @Test
    void aLagFrameMakesADurableListenerClaimWhatItMissed() throws Exception {
        AtomicInteger claims = new AtomicInteger();

        try (FakeGatewayServer server = new FakeGatewayServer((headers, body) -> switch (headers.get("x-euclid-action")) {
            case "subscribe-events" -> "{\"subscriptions\":[]}";
            case "receive-events" -> {
                claims.incrementAndGet();
                yield "{\"total\":0,\"events\":[]}";
            }
            default -> "{}";
        })) {
            EuclidEventStream stream = newStream(server);
            try (EuclidEventListener listener = EuclidEventListener.builder()
                    .ees(newEes(server)).stream(stream).name("importer")
                    .eventTypes(List.of("esm.object.created"))
                    .handler(event -> {
                    })
                    .build()) {

                listener.start();
                server.awaitHandshake(5);
                int afterStart = claims.get();

                // The gateway dropped pushed events for this connection. They are still in the
                // store, so the right response is to claim, not to log and forget.
                server.sendRawFrame("{\"type\":\"lag\",\"dropped\":12}");

                long deadline = System.currentTimeMillis() + 5000;
                while (claims.get() <= afterStart && System.currentTimeMillis() < deadline) {
                    Thread.sleep(50);
                }
                assertTrue(claims.get() > afterStart, "a lag frame did not trigger a claim");
            }
            stream.close();
        }
    }

    @Test
    void aLiveListenerHandlesTheEventItselfAndClaimsNothing() throws Exception {
        AtomicInteger claims = new AtomicInteger();

        try (FakeGatewayServer server = new FakeGatewayServer((headers, body) -> switch (headers.get("x-euclid-action")) {
            case "subscribe-events" -> "{\"subscriptions\":[]}";
            case "receive-events" -> {
                claims.incrementAndGet();
                yield "{\"total\":0,\"events\":[]}";
            }
            default -> "{}";
        })) {
            CountDownLatch handled = new CountDownLatch(1);
            List<Event> received = new CopyOnWriteArrayList<>();

            EuclidEventStream stream = newStream(server);
            try (EuclidEventListener listener = EuclidEventListener.builder()
                    .ees(newEes(server)).stream(stream).name("ui-feed")
                    .eventTypes(List.of("esm.object.created"))
                    .mode(DeliveryMode.LIVE)
                    .handler(event -> {
                        received.add(event);
                        handled.countDown();
                    })
                    .build()) {

                listener.start();
                server.awaitHandshake(5);
                server.sendRawFrame("{\"type\":\"event\",\"id\":\"1\",\"topic\":\"esm.object.created\","
                                    + "\"accountId\":\"863459426936\",\"region\":\"eu-central-1\","
                                    + "\"body\":{\"delivery\":\"live\",\"key\":\"a.csv\",\"size\":12}}");

                assertTrue(handled.await(5, TimeUnit.SECONDS), "the handler was never called");
            }
            stream.close();

            assertEquals("a.csv", received.getFirst().payload().get("key"));
            // A live subscription stores nothing, so there is nothing to claim and nothing to
            // acknowledge - the frame was the event.
            assertEquals(0, claims.get(), "a live listener claimed events it should not have");
        }
    }

    private static EuclidEventStream newStream(FakeGatewayServer server) {
        return new EuclidEventStream("http://localhost:" + server.port(), "test-token", "eu-central-1",
                "863459426936", "alice", null, null, null, "ees");
    }

    private static EuclidEes newEes(FakeGatewayServer server) {
        return new EuclidEes("http://localhost:" + server.port(), "test-token", "eu-central-1",
                "863459426936", "alice", null, null, null, null);
    }
}
