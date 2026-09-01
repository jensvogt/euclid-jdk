package de.jensvogt.euclid.module.eqs;

import de.jensvogt.euclid.Euclid;
import de.jensvogt.euclid.dto.com.Variant;
import de.jensvogt.euclid.dto.eqs.ReceiveMessagesResponse;
import de.jensvogt.euclid.dto.eqs.model.Message;
import de.jensvogt.euclid.module.eam.EuclidSession;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Stress-tests EuclidEqs under concurrent load with separate, decoupled producer and consumer
 * threads sharing one queue - WRITER_COUNT threads that only send, and READER_COUNT threads that
 * only receive/delete, both running for the whole test rather than one thread doing both in
 * lockstep, since that's closer to how a queue actually gets used in production (producers and
 * consumers are independent processes with no coordination between them). Checks that no message
 * is lost or delivered more than once despite the concurrency.
 * <p>
 * {@link #concurrentWritersAndReadersOnSharedQueue()} floods the queue, so it never actually
 * proves the websocket wake-up in {@link EuclidEqs#receiveMessages} fired instead of falling back
 * to polling - a receiver's very first check almost always finds a backlog already waiting either
 * way. {@link #websocketWakesReadersPromptlyDuringLowRateProduction()} specifically isolates that
 * mechanism: a single writer paced well below EuclidEqs's poll interval, so a receiver is
 * genuinely idle/waiting between messages, plus a receive wait time far longer than that poll
 * interval - only a working websocket "eqs.message.sent" event, not the polling fallback, could
 * keep measured latency low under those conditions.
 * <p>
 * Requires a live Euclid server; disabled by default since {@link #concurrentWritersAndReadersOnSharedQueue()}
 * moves WRITER_COUNT * MESSAGES_PER_WRITER (1,000,000) messages and can take a very long time to run.
 */
@Disabled("requires a live Euclid server at https://localhost:5566 and moves 1,000,000 messages - run manually")
class EuclidEqsLoadTest {

    private static final int WRITER_COUNT = 4;
    private static final int READER_COUNT = 6;
    private static final int MESSAGES_PER_WRITER = 10_000;

    private static final int WS_READER_COUNT = 3;
    private static final int WS_MESSAGE_COUNT = 10_000;
    private static final long WS_SEND_INTERVAL_MILLIS = 150;
    private static final long WS_RECEIVE_WAIT_SECONDS = 30;
    private static final double WS_MAX_MEAN_LATENCY_MILLIS = 300;

    @Test
    void concurrentWritersAndReadersOnSharedQueue() throws Exception {
        EuclidSession session = Euclid.forServer("https://localhost:5566")
                .access()
                .credentials("admin", "admin")
                .namespace("development")
                .caCertPath("/home/vogje01/work/euclid/dist/linux/etc/euclid_cert.crt")
                .login();
        EuclidEqs sqs = session.eqs();

        String queueErn = sqs.createQueue("test-queue-load").ern();
        AtomicLong nextMessageId = new AtomicLong();
        AtomicBoolean writersDone = new AtomicBoolean(false);
        Set<Long> receivedIds = ConcurrentHashMap.newKeySet();

        ExecutorService executor = Executors.newFixedThreadPool(WRITER_COUNT + READER_COUNT);
        try {
            List<Future<?>> readerFutures = new ArrayList<>();
            for (int r = 0; r < READER_COUNT; r++) {
                readerFutures.add(executor.submit((Callable<Void>) () -> {
                    runReader(sqs, queueErn, writersDone, receivedIds);
                    return null;
                }));
            }

            List<Future<?>> writerFutures = new ArrayList<>();
            for (int w = 0; w < WRITER_COUNT; w++) {
                writerFutures.add(executor.submit((Callable<Void>) () -> {
                    runWriter(sqs, queueErn, nextMessageId);
                    return null;
                }));
            }

            awaitAll(writerFutures);
            writersDone.set(true);
            awaitAll(readerFutures);

            assertEquals(WRITER_COUNT * (long) MESSAGES_PER_WRITER, receivedIds.size(),"every sent message should have been received exactly once");
            assertEquals(0, sqs.getMessageCount(queueErn).total(), "queue should be empty once every reader stops");
        } finally {
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.HOURS);
            sqs.deleteQueue(queueErn);
        }
    }

    @Test
    void websocketWakesReadersPromptlyDuringLowRateProduction() throws Exception {
        EuclidSession session = Euclid.forServer("https://localhost:5566")
                .access()
                .credentials("admin", "admin")
                .namespace("development")
                .caCertPath("/home/vogje01/work/euclid/dist/linux/etc/euclid_cert.crt")
                .login();
        EuclidEqs sqs = session.eqs();

        String queueErn = sqs.createQueue("test-queue-ws-latency").ern();
        AtomicBoolean writerDone = new AtomicBoolean(false);
        Set<Long> receivedIds = ConcurrentHashMap.newKeySet();
        List<Long> latenciesMillis = Collections.synchronizedList(new ArrayList<>());

        ExecutorService executor = Executors.newFixedThreadPool(WS_READER_COUNT + 1);
        try {
            List<Future<?>> readerFutures = new ArrayList<>();
            for (int r = 0; r < WS_READER_COUNT; r++) {
                readerFutures.add(executor.submit((Callable<Void>) () -> {
                    runLatencyReader(sqs, queueErn, writerDone, receivedIds, latenciesMillis);
                    return null;
                }));
            }

            Future<?> writerFuture = executor.submit((Callable<Void>) () -> {
                runPacedWriter(sqs, queueErn);
                return null;
            });

            awaitAll(List.of(writerFuture));
            writerDone.set(true);
            awaitAll(readerFutures);

            assertEquals(WS_MESSAGE_COUNT, receivedIds.size(), "every sent message should have been received exactly once");
            assertEquals(0, sqs.getMessageCount(queueErn).total(), "queue should be empty once every reader stops");

            double meanLatencyMillis = latenciesMillis.stream().mapToLong(Long::longValue).average().orElseThrow();
            assertTrue(meanLatencyMillis < WS_MAX_MEAN_LATENCY_MILLIS,
                    "mean receive latency was " + meanLatencyMillis + "ms, expected well under the 500ms poll "
                            + "interval EuclidEqs falls back to if the websocket connection never came up - this "
                            + "assertion only passes if messages are actually waking receiveMessages() via the "
                            + "eqs.message.sent websocket event, not by polling");
        } finally {
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.HOURS);
            sqs.deleteQueue(queueErn);
        }
    }

    // One message every WS_SEND_INTERVAL_MILLIS, well below the 500ms poll interval EuclidEqs
    // falls back to - slow enough that readers are genuinely blocked in receiveMessages() waiting
    // for the next message rather than always finding one already sitting in the queue.
    private static void runPacedWriter(EuclidEqs sqs, String queueErn) throws Exception {
        for (int i = 0; i < WS_MESSAGE_COUNT; i++) {
            Map<String, Variant> attributes = Map.of(
                    "testId", new Variant("long", (long) i),
                    "sentAt", new Variant("long", System.currentTimeMillis()));
            Random random = new SecureRandom();
            random.setSeed(System.currentTimeMillis() + i * WS_SEND_INTERVAL_MILLIS);
            int length = random.nextInt(1024*1024);
            sqs.sendMessage(queueErn, generateRandomString(length), attributes);
            Thread.sleep(WS_SEND_INTERVAL_MILLIS);
        }
    }

    // Waits up to WS_RECEIVE_WAIT_SECONDS per call - far longer than the 500ms poll interval, so
    // a receive that comes back quickly can only mean the websocket wake-up fired.
    private static void runLatencyReader(EuclidEqs sqs, String queueErn, AtomicBoolean writerDone,
                                          Set<Long> receivedIds, List<Long> latenciesMillis) throws Exception {
        while (true) {
            ReceiveMessagesResponse response = sqs.receiveMessages(queueErn, 10, WS_RECEIVE_WAIT_SECONDS);
            long now = System.currentTimeMillis();
            for (Message message : response.messages()) {
                sqs.deleteMessage(message.receiptHandle());
                long sentAt = Long.parseLong(String.valueOf(message.attributes().get("sentAt").value()));
                latenciesMillis.add(now - sentAt);
                recordReceivedById(message, receivedIds);
            }
            if (response.messages().isEmpty() && writerDone.get() && sqs.getMessageCount(queueErn).total() == 0) {
                return;
            }
        }
    }

    private static void recordReceivedById(Message message, Set<Long> receivedIds) {
        long id = Long.parseLong(String.valueOf(message.attributes().get("testId").value()));
        assertTrue(receivedIds.add(id), "message " + id + " was delivered more than once");
    }

    private static void awaitAll(List<Future<?>> futures) throws InterruptedException {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                fail(e.getCause());
            }
        }
    }

    private static void runWriter(EuclidEqs sqs, String queueErn, AtomicLong nextMessageId) throws Exception {
        for (int i = 0; i < MESSAGES_PER_WRITER; i++) {
            long messageId = nextMessageId.getAndIncrement();
            Map<String, Variant> attributes = Map.of(
                    "testattr1", new Variant("string", "testvalue"),
                    "testattr2", new Variant("long", messageId),
                    "testattr3", new Variant("bool", true),
                    "testattr4", new Variant("double", 1.0),
                    "testattr5", new Variant("float", 1.0f));
            Random random = new SecureRandom();
            random.setSeed(messageId);
            int length = random.nextInt(1024*1024);
            sqs.sendMessage(queueErn, generateRandomString(length), attributes);
        }
    }

    // Random alphanumeric string of exact length N
    public static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // Runs until told the writers are finished AND the queue has genuinely nothing left -
    // checked via getMessageCount() rather than just "my last receive was empty", since another
    // reader could be mid-processing a batch it already claimed (invisible, not yet deleted) when
    // this reader's own receive comes up empty.
    private static void runReader(EuclidEqs sqs, String queueErn, AtomicBoolean writersDone, Set<Long> receivedIds)
            throws Exception {
        while (true) {
            ReceiveMessagesResponse response = sqs.receiveMessages(queueErn, 50, 2);
            for (Message message : response.messages()) {
                sqs.deleteMessage(message.receiptHandle());
                recordReceived(message, receivedIds);
            }
            if (response.messages().isEmpty() && writersDone.get() && sqs.getMessageCount(queueErn).total() == 0) {
                return;
            }
        }
    }

    private static void recordReceived(Message message, Set<Long> receivedIds) {
        long id = Long.parseLong(String.valueOf(message.attributes().get("testattr2").value()));
        assertTrue(receivedIds.add(id), "message " + id + " was delivered more than once");
    }
}
