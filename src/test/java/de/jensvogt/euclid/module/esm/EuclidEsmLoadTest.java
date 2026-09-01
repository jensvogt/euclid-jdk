package de.jensvogt.euclid.module.esm;

import de.jensvogt.euclid.Euclid;
import de.jensvogt.euclid.dto.com.Variant;
import de.jensvogt.euclid.dto.eqs.ReceiveMessagesResponse;
import de.jensvogt.euclid.dto.eqs.model.Message;
import de.jensvogt.euclid.dto.esm.CompleteUploadResponse;
import de.jensvogt.euclid.dto.esm.ListObjectsResponse;
import de.jensvogt.euclid.dto.esm.model.Bucket;
import de.jensvogt.euclid.dto.esm.model.EsmObject;
import de.jensvogt.euclid.module.eam.EuclidSession;
import de.jensvogt.euclid.module.eqs.EuclidEqs;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Stress-tests EuclidEsm the way {@code EuclidEqsLoadTest} does EuclidEqs: separate, decoupled
 * producer and consumer threads working one shared bucket - UPLOADER_COUNT threads that only
 * upload and DELETER_COUNT threads that only list/delete, both running for the whole test rather
 * than one thread doing both in lockstep, since that's closer to how a store actually gets used.
 * <p>
 * Every object is uploaded with content of a random size, and its size and MD5 are recorded before
 * the upload starts; a deleter checks both against what {@code list-objects} reports before
 * removing the object. That makes the pass/fail condition about more than "no exception was
 * thrown": a part that got assembled in the wrong order, dropped, or duplicated under concurrent
 * multipart uploads shows up as an MD5 or size mismatch on exactly the object it happened to,
 * which is what {@link #parallelMultipartUploadsAssembleEveryPartInOrder} isolates further with a
 * handful of deliberately large, heavily-parallel uploads.
 * <p>
 * Note that ESM completes an upload in two steps: {@code complete-upload} returns as soon as every
 * part has arrived (status UPLOADED) and a background thread then assembles, hashes and sniffs the
 * file before flipping the object to COMPLETED. Anything here that reads an object's size or MD5
 * back therefore waits for COMPLETED first - an object listed before that point is a genuine
 * in-between state, not a defect.
 * <p>
 * {@link #metadataReadsStayConsistentWhileObjectsAreUploaded} covers the read path instead: bucket
 * lookups hammered from several threads while uploads are in flight, checking that a lookup always
 * answers about the bucket that was asked for (a crossed response under concurrency would show up
 * as another bucket's ERN coming back). {@link #esmAndEqsLoadRunConcurrentlyOnOneSession} runs an
 * ESM and an EQS load side by side through a single session, since the two share the session's
 * token and HTTP client and are dispatched by the same gateway.
 * <p>
 * Requires a live Euclid server; disabled by default since these tests move several GB of object
 * data and can take a very long time to run.
 */
@Disabled("requires a live Euclid server at https://localhost:5566 and moves several GB of object data - run manually")
class EuclidEsmLoadTest {

    private static final String SERVER_URL = "https://localhost:5566";
    private static final String CA_CERT_PATH = "/home/vogje01/work/euclid/dist/linux/etc/euclid_cert.crt";

    private static final int UPLOADER_COUNT = 4;
    private static final int DELETER_COUNT = 4;
    private static final int OBJECTS_PER_UPLOADER = 250;
    private static final int MAX_OBJECT_SIZE_BYTES = 1024 * 1024;
    private static final int UPLOAD_PART_SIZE = 256 * 1024;
    private static final int UPLOAD_CONCURRENCY = 4;
    private static final int LIST_PAGE_SIZE = 100;

    private static final int LARGE_FILE_COUNT = 4;
    private static final long LARGE_FILE_SIZE_BYTES = 64L * 1024 * 1024;
    private static final int LARGE_FILE_PART_SIZE = 5 * 1024 * 1024;
    private static final int LARGE_FILE_CONCURRENCY = 8;

    private static final int METADATA_READER_COUNT = 6;
    private static final int METADATA_UPLOADER_COUNT = 2;
    private static final int METADATA_OBJECTS_PER_UPLOADER = 100;
    private static final int METADATA_MAX_OBJECT_SIZE_BYTES = 256 * 1024;

    private static final int MIXED_UPLOADER_COUNT = 2;
    private static final int MIXED_DELETER_COUNT = 2;
    private static final int MIXED_OBJECTS_PER_UPLOADER = 100;
    private static final int MIXED_MAX_OBJECT_SIZE_BYTES = 512 * 1024;
    private static final int MIXED_EQS_WRITER_COUNT = 2;
    private static final int MIXED_EQS_READER_COUNT = 2;
    private static final int MIXED_EQS_MESSAGES_PER_WRITER = 10_000;

    private static final long POLL_INTERVAL_MILLIS = 200;
    private static final long STALL_TIMEOUT_MILLIS = 120_000;
    private static final long COMPLETION_TIMEOUT_MILLIS = 300_000;
    private static final long SHUTDOWN_GRACE_SECONDS = 60;

    @Test
    void concurrentUploadersAndDeletersOnSharedBucket(@TempDir Path tempDir) throws Exception {
        EuclidEsm esm = login().esm();

        String bucketErn = esm.createBucket("test-bucket-load").ern();
        Map<String, UploadedObject> expected = new ConcurrentHashMap<>();
        Set<String> claimedKeys = ConcurrentHashMap.newKeySet();
        Set<String> deletedKeys = ConcurrentHashMap.newKeySet();
        AtomicBoolean uploadersDone = new AtomicBoolean(false);

        ExecutorService executor = Executors.newFixedThreadPool(UPLOADER_COUNT + DELETER_COUNT);
        try {
            List<Future<?>> deleterFutures = new ArrayList<>();
            for (int d = 0; d < DELETER_COUNT; d++) {
                deleterFutures.add(executor.submit((Callable<Void>) () -> {
                    runDeleter(esm, bucketErn, uploadersDone, expected, claimedKeys, deletedKeys);
                    return null;
                }));
            }

            List<Future<?>> uploaderFutures = new ArrayList<>();
            for (int u = 0; u < UPLOADER_COUNT; u++) {
                int uploaderId = u;
                uploaderFutures.add(executor.submit((Callable<Void>) () -> {
                    runUploader(esm, bucketErn, tempDir, uploaderId, OBJECTS_PER_UPLOADER, MAX_OBJECT_SIZE_BYTES,
                            expected);
                    return null;
                }));
            }

            awaitAll(uploaderFutures);
            uploadersDone.set(true);
            awaitAll(deleterFutures);

            assertEquals(UPLOADER_COUNT * OBJECTS_PER_UPLOADER, deletedKeys.size(),
                    "every uploaded object should have been listed and deleted exactly once");
            assertEquals(0, esm.listObjects(bucketErn, "", 1, 0, "name").total(),
                    "bucket should be empty once every deleter stops");
            // The bucket's size/objects counters are maintained by a read-modify-write on every
            // upload and delete, so a lost update under concurrency leaves them drifting away from
            // the objects that actually exist - which an empty bucket makes easy to spot.
            assertEquals(0, esm.getBucketSize(bucketErn).size(),
                    "bucket size counter should be back to zero once every object is deleted");
        } finally {
            // Deleters loop until the uploaders are done and the bucket is empty, neither of which
            // ever happens if the test failed part-way through - flagging them off here is what
            // keeps a failure a failure instead of a hang.
            uploadersDone.set(true);
            shutdown(executor);
            esm.purgeBucket(bucketErn);
            esm.deleteBucket(bucketErn);
        }
    }

    @Test
    void parallelMultipartUploadsAssembleEveryPartInOrder(@TempDir Path tempDir) throws Exception {
        EuclidEsm esm = login().esm();

        String bucketErn = esm.createBucket("test-bucket-load-multipart").ern();
        Map<String, UploadedObject> expected = new ConcurrentHashMap<>();

        ExecutorService executor = Executors.newFixedThreadPool(LARGE_FILE_COUNT);
        try {
            List<Future<?>> uploads = new ArrayList<>();
            for (int f = 0; f < LARGE_FILE_COUNT; f++) {
                String key = String.format("large-%02d.bin", f);
                Path file = tempDir.resolve(key);
                expected.put(key, writeRandomFile(file, LARGE_FILE_SIZE_BYTES));
                uploads.add(executor.submit((Callable<Void>) () -> {
                    CompleteUploadResponse response =
                            esm.uploadFile(bucketErn, key, file, LARGE_FILE_PART_SIZE, LARGE_FILE_CONCURRENCY);
                    assertEquals(LARGE_FILE_SIZE_BYTES, response.size(),
                            "complete-upload reported a different size than was uploaded for " + key);
                    return null;
                }));
            }
            awaitAll(uploads);

            long totalSize = 0;
            for (Map.Entry<String, UploadedObject> entry : expected.entrySet()) {
                EsmObject object = awaitCompleted(esm, bucketErn, entry.getKey());
                assertEquals(entry.getValue().size(), object.size(),
                        "object " + entry.getKey() + " assembled to a different size than was uploaded");
                assertEquals(entry.getValue().md5(), object.md5Sum(),
                        "object " + entry.getKey() + " assembled to different bytes than were uploaded - parts "
                                + "uploaded concurrently must still be reassembled in part-number order");
                totalSize += object.size();
            }
            assertEquals(totalSize, esm.getBucketSize(bucketErn).size(),
                    "bucket size counter should add up to the objects it holds");
        } finally {
            shutdown(executor);
            esm.purgeBucket(bucketErn);
            esm.deleteBucket(bucketErn);
        }
    }

    @Test
    void metadataReadsStayConsistentWhileObjectsAreUploaded(@TempDir Path tempDir) throws Exception {
        EuclidEsm esm = login().esm();

        String bucketName = "test-bucket-load-metadata";
        String bucketErn = esm.createBucket(bucketName).ern();
        Map<String, UploadedObject> expected = new ConcurrentHashMap<>();
        AtomicBoolean uploadersDone = new AtomicBoolean(false);
        AtomicLong readCount = new AtomicLong();

        ExecutorService executor = Executors.newFixedThreadPool(METADATA_UPLOADER_COUNT + METADATA_READER_COUNT);
        try {
            List<Future<?>> readerFutures = new ArrayList<>();
            for (int r = 0; r < METADATA_READER_COUNT; r++) {
                readerFutures.add(executor.submit((Callable<Void>) () -> {
                    runMetadataReader(esm, bucketName, bucketErn, uploadersDone, readCount);
                    return null;
                }));
            }

            List<Future<?>> uploaderFutures = new ArrayList<>();
            for (int u = 0; u < METADATA_UPLOADER_COUNT; u++) {
                int uploaderId = u;
                uploaderFutures.add(executor.submit((Callable<Void>) () -> {
                    runUploader(esm, bucketErn, tempDir, uploaderId, METADATA_OBJECTS_PER_UPLOADER,
                            METADATA_MAX_OBJECT_SIZE_BYTES, expected);
                    return null;
                }));
            }

            awaitAll(uploaderFutures);
            uploadersDone.set(true);
            awaitAll(readerFutures);

            assertTrue(readCount.get() > 0, "metadata readers should have completed at least one read");
            assertEquals(METADATA_UPLOADER_COUNT * METADATA_OBJECTS_PER_UPLOADER,
                    esm.listObjects(bucketErn, "", 1, 0, "name").total(),
                    "every uploaded object should be listed once the uploaders are done");
        } finally {
            uploadersDone.set(true);
            shutdown(executor);
            esm.purgeBucket(bucketErn);
            esm.deleteBucket(bucketErn);
        }
    }

    @Test
    void esmAndEqsLoadRunConcurrentlyOnOneSession(@TempDir Path tempDir) throws Exception {
        EuclidSession session = login();
        EuclidEsm esm = session.esm();
        EuclidEqs eqs = session.eqs();

        String bucketErn = esm.createBucket("test-bucket-load-mixed").ern();
        String queueErn = eqs.createQueue("test-queue-load-mixed").ern();

        Map<String, UploadedObject> expected = new ConcurrentHashMap<>();
        Set<String> claimedKeys = ConcurrentHashMap.newKeySet();
        Set<String> deletedKeys = ConcurrentHashMap.newKeySet();
        Set<Long> receivedIds = ConcurrentHashMap.newKeySet();
        AtomicLong nextMessageId = new AtomicLong();
        AtomicBoolean uploadersDone = new AtomicBoolean(false);
        AtomicBoolean writersDone = new AtomicBoolean(false);

        ExecutorService executor = Executors.newFixedThreadPool(MIXED_UPLOADER_COUNT + MIXED_DELETER_COUNT
                + MIXED_EQS_WRITER_COUNT + MIXED_EQS_READER_COUNT);
        try {
            List<Future<?>> consumerFutures = new ArrayList<>();
            for (int d = 0; d < MIXED_DELETER_COUNT; d++) {
                consumerFutures.add(executor.submit((Callable<Void>) () -> {
                    runDeleter(esm, bucketErn, uploadersDone, expected, claimedKeys, deletedKeys);
                    return null;
                }));
            }
            for (int r = 0; r < MIXED_EQS_READER_COUNT; r++) {
                consumerFutures.add(executor.submit((Callable<Void>) () -> {
                    runMessageReader(eqs, queueErn, writersDone, receivedIds);
                    return null;
                }));
            }

            List<Future<?>> uploaderFutures = new ArrayList<>();
            for (int u = 0; u < MIXED_UPLOADER_COUNT; u++) {
                int uploaderId = u;
                uploaderFutures.add(executor.submit((Callable<Void>) () -> {
                    runUploader(esm, bucketErn, tempDir, uploaderId, MIXED_OBJECTS_PER_UPLOADER,
                            MIXED_MAX_OBJECT_SIZE_BYTES, expected);
                    return null;
                }));
            }
            List<Future<?>> writerFutures = new ArrayList<>();
            for (int w = 0; w < MIXED_EQS_WRITER_COUNT; w++) {
                writerFutures.add(executor.submit((Callable<Void>) () -> {
                    runMessageWriter(eqs, queueErn, nextMessageId);
                    return null;
                }));
            }

            awaitAll(uploaderFutures);
            uploadersDone.set(true);
            awaitAll(writerFutures);
            writersDone.set(true);
            awaitAll(consumerFutures);

            assertEquals(MIXED_UPLOADER_COUNT * MIXED_OBJECTS_PER_UPLOADER, deletedKeys.size(),
                    "every uploaded object should have been listed and deleted exactly once");
            assertEquals(0, esm.listObjects(bucketErn, "", 1, 0, "name").total(),
                    "bucket should be empty once every deleter stops");
            assertEquals(MIXED_EQS_WRITER_COUNT * (long) MIXED_EQS_MESSAGES_PER_WRITER, receivedIds.size(),
                    "every sent message should have been received exactly once");
            assertEquals(0, eqs.getMessageCount(queueErn).total(), "queue should be empty once every reader stops");
        } finally {
            uploadersDone.set(true);
            writersDone.set(true);
            shutdown(executor);
            esm.purgeBucket(bucketErn);
            esm.deleteBucket(bucketErn);
            eqs.deleteQueue(queueErn);
        }
    }

    // Uploads objectCount objects of random content and size, recording each one's size and MD5
    // before the upload starts - a deleter can pick the object up the moment it turns COMPLETED,
    // which can be well before uploadFile() has even returned here. The local file is dropped
    // straight after the upload so the test's own disk use stays bounded no matter how many
    // objects it moves.
    private static void runUploader(EuclidEsm esm, String bucketErn, Path tempDir, int uploaderId, int objectCount,
                                    int maxObjectSize, Map<String, UploadedObject> expected) throws Exception {
        Random random = new SecureRandom();
        for (int i = 0; i < objectCount; i++) {
            String key = String.format("load-%d-%06d.bin", uploaderId, i);
            Path file = tempDir.resolve(key);
            expected.put(key, writeRandomFile(file, 1 + random.nextInt(maxObjectSize)));
            try {
                esm.uploadFile(bucketErn, key, file, UPLOAD_PART_SIZE, UPLOAD_CONCURRENCY);
            } finally {
                Files.deleteIfExists(file);
            }
        }
    }

    // Runs until told the uploaders are finished AND the bucket has genuinely nothing left. Unlike
    // a queue receive, list-objects hands the same object to every deleter that lists at the same
    // moment, so the key is claimed in a shared set first and only the thread that wins the claim
    // deletes it - without that, deleters would race to delete each other's objects and the count
    // of what was actually removed would mean nothing.
    private static void runDeleter(EuclidEsm esm, String bucketErn, AtomicBoolean uploadersDone,
                                   Map<String, UploadedObject> expected, Set<String> claimedKeys,
                                   Set<String> deletedKeys) throws Exception {
        long stallDeadline = System.currentTimeMillis() + STALL_TIMEOUT_MILLIS;
        while (true) {
            ListObjectsResponse response = esm.listObjects(bucketErn, "", LIST_PAGE_SIZE, 0, "name");
            boolean progress = false;
            for (EsmObject object : response.objects()) {
                // An object is listed from the moment its upload is created, long before its parts
                // are assembled and hashed by ESM's background pass - only a COMPLETED one can be
                // checked against what was uploaded, the rest come back on a later listing.
                if (!"COMPLETED".equals(object.status()) || !claimedKeys.add(object.key())) {
                    continue;
                }
                UploadedObject uploaded = expected.get(object.key());
                assertNotNull(uploaded, "listing returned key " + object.key() + " that no uploader ever sent");
                assertEquals(uploaded.size(), object.size(),
                        "object " + object.key() + " came back with a different size than was uploaded");
                assertEquals(uploaded.md5(), object.md5Sum(),
                        "object " + object.key() + " assembled to different bytes than were uploaded");
                esm.deleteObject(object.ern());
                deletedKeys.add(object.key());
                progress = true;
            }

            if (progress) {
                stallDeadline = System.currentTimeMillis() + STALL_TIMEOUT_MILLIS;
            }
            if (uploadersDone.get() && response.total() == 0) {
                return;
            }
            if (System.currentTimeMillis() > stallDeadline) {
                fail("no object turned COMPLETED for " + STALL_TIMEOUT_MILLIS + "ms while " + response.total()
                        + " objects are still listed - background assembly appears stuck");
            }
            if (!progress) {
                Thread.sleep(POLL_INTERVAL_MILLIS);
            }
        }
    }

    // Hammers the read path for as long as objects are being uploaded. Every read names the bucket
    // it expects back, so a response that belongs to another request - the failure mode a
    // concurrency bug in request dispatch produces - fails here rather than being silently used.
    private static void runMetadataReader(EuclidEsm esm, String bucketName, String bucketErn,
                                          AtomicBoolean uploadersDone, AtomicLong readCount) throws Exception {
        while (!uploadersDone.get()) {
            assertEquals(bucketErn, esm.getBucketErn(bucketName).ern(),
                    "get-bucket-ern answered about a different bucket than was asked for");
            assertEquals(bucketErn, esm.getBucketSize(bucketErn).ern(),
                    "get-bucket-size answered about a different bucket than was asked for");
            assertTrue(esm.getBucketSize(bucketErn).size() >= 0, "bucket size should never go negative");

            List<Bucket> buckets = esm.listBuckets(bucketName, 10, 0, "name").buckets();
            assertTrue(buckets.stream().anyMatch(bucket -> bucketErn.equals(bucket.ern())),
                    "list-buckets should keep listing the bucket that is being written to");

            ListObjectsResponse objects = esm.listObjects(bucketErn, "", LIST_PAGE_SIZE, 0, "name");
            assertTrue(objects.objects().size() <= LIST_PAGE_SIZE, "list-objects returned more than one page");
            for (EsmObject object : objects.objects()) {
                assertEquals(bucketErn, object.bucketErn(), "list-objects returned an object of another bucket");
            }
            readCount.incrementAndGet();
        }
    }

    private static void runMessageWriter(EuclidEqs eqs, String queueErn, AtomicLong nextMessageId) throws Exception {
        for (int i = 0; i < MIXED_EQS_MESSAGES_PER_WRITER; i++) {
            long messageId = nextMessageId.getAndIncrement();
            eqs.sendMessage(queueErn, "message-" + messageId, Map.of("testId", new Variant("long", messageId)));
        }
    }

    private static void runMessageReader(EuclidEqs eqs, String queueErn, AtomicBoolean writersDone,
                                         Set<Long> receivedIds) throws Exception {
        while (true) {
            ReceiveMessagesResponse response = eqs.receiveMessages(queueErn, 50, 2);
            for (Message message : response.messages()) {
                eqs.deleteMessage(message.receiptHandle());
                long id = Long.parseLong(String.valueOf(message.attributes().get("testId").value()));
                assertTrue(receivedIds.add(id), "message " + id + " was delivered more than once");
            }
            if (response.messages().isEmpty() && writersDone.get() && eqs.getMessageCount(queueErn).total() == 0) {
                return;
            }
        }
    }

    // complete-upload returns as soon as every part has arrived; size and MD5 are only final once
    // the background assembly pass has flipped the object to COMPLETED.
    private static EsmObject awaitCompleted(EuclidEsm esm, String bucketErn, String key) throws Exception {
        long deadline = System.currentTimeMillis() + COMPLETION_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            for (EsmObject object : esm.listObjects(bucketErn, key, 10, 0, "name").objects()) {
                if (key.equals(object.key()) && "COMPLETED".equals(object.status())) {
                    return object;
                }
            }
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }
        return fail("object " + key + " did not reach COMPLETED within " + COMPLETION_TIMEOUT_MILLIS + "ms");
    }

    // Writes random content in chunks rather than one big array, so a multi-GB test run doesn't
    // need the whole file in memory, and MD5s it on the way out - only the digest is kept, which
    // is all a deleter needs to check the bytes came back unchanged.
    private static UploadedObject writeRandomFile(Path file, long size) throws Exception {
        Random random = new SecureRandom();
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] chunk = new byte[(int) Math.min(size, 1024 * 1024)];
        long remaining = size;
        try (OutputStream out = Files.newOutputStream(file)) {
            while (remaining > 0) {
                int length = (int) Math.min(remaining, chunk.length);
                random.nextBytes(chunk);
                out.write(chunk, 0, length);
                md5.update(chunk, 0, length);
                remaining -= length;
            }
        }
        return new UploadedObject(size, HexFormat.of().formatHex(md5.digest()));
    }

    private static EuclidSession login() throws Exception {
        return Euclid.forServer(SERVER_URL)
                .access()
                .credentials("admin", "admin")
                .namespace("development")
                .caCertPath(CA_CERT_PATH)
                .login();
    }

    // Gives the workers a bounded window to notice they are done before interrupting whatever is
    // left - a consumer blocked in a long receive or an upload retry would otherwise hold the test
    // up for as long as the wait lasts, and one still looping after a failed assertion elsewhere
    // would hold it up indefinitely.
    private static void shutdown(ExecutorService executor) throws InterruptedException {
        executor.shutdown();
        if (!executor.awaitTermination(SHUTDOWN_GRACE_SECONDS, TimeUnit.SECONDS)) {
            executor.shutdownNow();
            executor.awaitTermination(SHUTDOWN_GRACE_SECONDS, TimeUnit.SECONDS);
        }
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

    /** What an uploader recorded about an object before sending it, for a deleter to check it against. */
    private record UploadedObject(long size, String md5) {
    }
}
