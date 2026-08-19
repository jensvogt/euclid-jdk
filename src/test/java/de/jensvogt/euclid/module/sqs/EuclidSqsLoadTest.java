package de.jensvogt.euclid.module.sqs;

import de.jensvogt.euclid.Euclid;
import de.jensvogt.euclid.dto.sqs.ReceiveMessagesResponse;
import de.jensvogt.euclid.dto.sqs.model.Message;
import de.jensvogt.euclid.dto.sqs.model.Variant;
import de.jensvogt.euclid.module.access.EuclidSession;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Stress-tests EuclidSqs under concurrent load: THREAD_COUNT threads share a single queue,
 * each sending and draining MESSAGES_PER_THREAD messages, checking that concurrent
 * producers/consumers on the same queue neither lose nor double-deliver a message.
 * <p>
 * Requires a live Euclid server; disabled by default since it moves THREAD_COUNT *
 * MESSAGES_PER_THREAD (1,000,000) messages and can take a very long time to run.
 */
@Disabled("requires a live Euclid server at https://localhost:5566 and moves 1,000,000 messages - run manually")
class EuclidSqsLoadTest {

    private static final int THREAD_COUNT = 10;
    private static final int MESSAGES_PER_THREAD = 10_000;

    @Test
    void concurrentSendReceiveDeleteOnSharedQueue() throws Exception {
        EuclidSession session = Euclid.forServer("https://localhost:5566")
                .access().credentials("jvo", "Tea4TheTillerman").login();
        EuclidSqs sqs = session.sqs();

        String queueErn = sqs.createQueue("test-queue-load").ern();
        AtomicLong nextMessageId = new AtomicLong();
        Set<Long> receivedIds = ConcurrentHashMap.newKeySet();

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < THREAD_COUNT; t++) {
                futures.add(executor.submit((Callable<Void>) () -> {
                    runWorker(sqs, queueErn, nextMessageId, receivedIds);
                    return null;
                }));
            }
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    fail(e.getCause());
                }
            }

            drainRemaining(sqs, queueErn, receivedIds);

            assertEquals(THREAD_COUNT * (long) MESSAGES_PER_THREAD, receivedIds.size(),
                    "every sent message should have been received exactly once");
            assertEquals(0, sqs.getMessageCount(queueErn).total(), "queue should be empty once draining finishes");
        } finally {
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.HOURS);
            sqs.deleteQueue(queueErn);
        }
    }

    private static void runWorker(EuclidSqs sqs, String queueErn, AtomicLong nextMessageId, Set<Long> receivedIds)
            throws Exception {
        for (int i = 0; i < MESSAGES_PER_THREAD; i++) {
            long messageId = nextMessageId.getAndIncrement();
            Map<String, Variant> attributes = Map.of(
                    "testattr1", new Variant("string", "testvalue"),
                    "testattr2", new Variant("long", messageId),
                    "testattr3", new Variant("bool", true));
            sqs.sendMessage(queueErn, "{\"testattr\":\"testvalue\"}", attributes);

            ReceiveMessagesResponse response = sqs.receiveAllMessages(queueErn);
            for (Message message : response.messages()) {
                sqs.deleteMessage(message.receiptHandle());
                recordReceived(message, receivedIds);
            }
        }
    }

    // Messages sent late by one thread can still be sitting in the queue after every worker
    // finishes sending/receiving its own share, since a receive only drains what's visible at
    // that instant - so sweep the queue once more to account for the rest.
    private static void drainRemaining(EuclidSqs sqs, String queueErn, Set<Long> receivedIds) throws Exception {
        ReceiveMessagesResponse response = sqs.receiveAllMessages(queueErn);
        for (Message message : response.messages()) {
            sqs.deleteMessage(message.receiptHandle());
            recordReceived(message, receivedIds);
        }
    }

    private static void recordReceived(Message message, Set<Long> receivedIds) {
        long id = Long.parseLong(String.valueOf(message.attributes().get("testattr2").value()));
        assertTrue(receivedIds.add(id), "message " + id + " was delivered more than once");
    }
}
