package de.jensvogt.euclid.module.esm;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.jensvogt.euclid.auth.SigV4;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.dto.com.Variant;
import de.jensvogt.euclid.dto.esm.CompleteUploadResponse;
import de.jensvogt.euclid.dto.esm.CreateBucketResponse;
import de.jensvogt.euclid.dto.esm.GetBucketErnResponse;
import de.jensvogt.euclid.dto.esm.GetBucketSizeResponse;
import de.jensvogt.euclid.dto.esm.GetObjectCountResponse;
import de.jensvogt.euclid.dto.esm.ListBucketsResponse;
import de.jensvogt.euclid.dto.esm.ListObjectAttributesResponse;
import de.jensvogt.euclid.dto.esm.ListObjectsResponse;
import de.jensvogt.euclid.dto.esm.ListSubscriptionsResponse;
import de.jensvogt.euclid.dto.esm.ObjectAttributeResponse;
import de.jensvogt.euclid.dto.esm.PurgeBucketResponse;
import de.jensvogt.euclid.dto.esm.SubscribeResponse;
import de.jensvogt.euclid.dto.esm.model.Bucket;
import de.jensvogt.euclid.dto.esm.model.BucketEvent;
import de.jensvogt.euclid.dto.esm.model.EsmObject;
import de.jensvogt.euclid.exception.EuclidServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms EuclidEsm authenticates the way it claims to (SigV4-signed when an access key is
 * configured, bearer token otherwise, mirroring euclid-cli's HttpClient.cpp), routes every
 * bucket/object action to the right request with a correctly-shaped body, parses the
 * corresponding response, surfaces non-2xx responses as {@link EuclidServiceException},
 * and - for {@code uploadFile} - correctly orchestrates create-upload/upload-part/complete-upload
 * including splitting, retrying, and always using the bearer token for the binary part uploads.
 */
class EuclidEsmTest {

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
    void createBucketSignsWithSigV4WhenAccessKeyConfigured() throws Exception {
        String accessKeyId = "AKIDEXAMPLE";
        String secretAccessKey = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";

        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"name\":\"photos\",\"ern\":\"bucket-ern\"}");
        });

        EuclidEsm esm = new EuclidEsm(baseUrl(), "unused-token", "eu-central-1", "863459426936", "alice",
                accessKeyId, secretAccessKey, null, null);
        esm.createBucket("photos");

        SignableRequest req = received.get();
        assertTrue(req.header("authorization").startsWith("AWS4-HMAC-SHA256 "));

        Optional<SigV4.VerifyResult> result = SigV4.verify(req,
                id -> id.equals(accessKeyId) ? Optional.of(secretAccessKey) : Optional.empty());
        assertTrue(result.isPresent(), "server-side verification of the client's own signature must succeed");
        assertEquals(accessKeyId, result.get().accessKeyId());
    }

    @Test
    void createBucketUsesBearerTokenWhenNoAccessKeyConfigured() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"name\":\"photos\",\"ern\":\"bucket-ern\"}");
        });

        EuclidEsm esm = new EuclidEsm(baseUrl(), "my-jwt-token", "eu-central-1", "863459426936", "alice",
                null, null, null, null);
        esm.createBucket("photos");

        assertEquals("Bearer my-jwt-token", received.get().header("authorization"));
    }

    @Test
    void createBucketParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"name\":\"photos\",\"ern\":\"ern:esm:eu-central-1:863459426936:bucket/photos\"}");
        });

        CreateBucketResponse response = newClient().createBucket("photos");

        assertEquals("create-bucket", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"name\":\"photos\"");
        assertEquals("photos", response.name());
        assertEquals("ern:esm:eu-central-1:863459426936:bucket/photos", response.ern());
    }

    @Test
    void deleteBucketSendsErn() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newClient().deleteBucket("bucket-ern");

        assertEquals("delete-bucket", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"ern\":\"bucket-ern\"");
    }

    @Test
    void listBucketsUsesDefaultsAndParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"buckets\":[{\"region\":\"eu-central-1\",\"owner\":\"alice\","
                    + "\"name\":\"photos\",\"ern\":\"bucket-ern\",\"size\":1024,\"objects\":3,"
                    + "\"created\":\"2026-01-01\",\"modified\":\"2026-01-02\"}],\"total\":1}");
        });

        ListBucketsResponse response = newClient().listBuckets();
        List<Bucket> buckets = response.buckets();
        assertEquals(1, response.total(), "the server's total must survive rather than be dropped");

        assertEquals("list-buckets", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"prefix\":\"\"", "\"pageSize\":10", "\"pageIndex\":0",
                "\"sortColumn\":\"name\"");
        assertEquals(1, buckets.size());
        assertEquals("photos", buckets.getFirst().name());
        assertEquals(3, buckets.getFirst().objects());
    }

    @Test
    void listBucketsWithExplicitParameters() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"buckets\":[]}");
        });

        List<Bucket> buckets = newClient().listBuckets("pho", 25, 2, "created").buckets();

        assertBodyContains(received.get().body(), "\"prefix\":\"pho\"", "\"pageSize\":25", "\"pageIndex\":2",
                "\"sortColumn\":\"created\"");
        assertTrue(buckets.isEmpty());
    }

    @Test
    void getBucketErnParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"ern\":\"bucket-ern\"}");
        });

        GetBucketErnResponse response = newClient().getBucketErn("photos");

        assertEquals("get-bucket-ern", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"name\":\"photos\"");
        assertEquals("bucket-ern", response.ern());
    }

    @Test
    void getBucketSizeParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"ern\":\"bucket-ern\",\"size\":2048}");
        });

        GetBucketSizeResponse response = newClient().getBucketSize("bucket-ern");

        assertEquals("get-bucket-size", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"ern\":\"bucket-ern\"");
        assertEquals(2048, response.size());
    }

    @Test
    void listObjectsUsesDefaultsAndParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"objects\":[{\"ern\":\"obj-ern\",\"bucketErn\":\"bucket-ern\","
                    + "\"key\":\"a.txt\",\"size\":11,\"status\":\"AVAILABLE\",\"contentType\":\"text/plain\","
                    + "\"md5Sum\":\"abc\",\"created\":\"2026-01-01\",\"modified\":\"2026-01-02\"}],\"total\":1}");
        });

        ListObjectsResponse response = newClient().listObjects("bucket-ern");

        assertEquals("list-objects", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"bucketErn\":\"bucket-ern\"", "\"prefix\":\"\"",
                "\"pageSize\":10", "\"pageIndex\":0", "\"sortColumn\":\"name\"");
        assertEquals(1, response.total());
        assertEquals("a.txt", response.objects().getFirst().key());
    }

    @Test
    void listObjectsWithExplicitParameters() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"objects\":[],\"total\":0}");
        });

        newClient().listObjects("bucket-ern", "a", 25, 2, "created");

        assertBodyContains(received.get().body(), "\"prefix\":\"a\"", "\"pageSize\":25", "\"pageIndex\":2",
                "\"sortColumn\":\"created\"");
    }

    @Test
    void deleteObjectSendsErn() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newClient().deleteObject("obj-ern");

        assertEquals("delete-object", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"ern\":\"obj-ern\"");
    }

    @Test
    void purgeBucketParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"ern\":\"bucket-ern\",\"count\":5}");
        });

        PurgeBucketResponse response = newClient().purgeBucket("bucket-ern");

        assertEquals("purge-bucket", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"ern\":\"bucket-ern\"");
        assertEquals(5, response.count());
    }

    @Test
    void nonSuccessResponseThrowsEuclidServiceException() throws Exception {
        server = startServer(exchange -> sendResponse(exchange, 500, "{\"error\":\"boom\"}"));

        EuclidEsm esm = newClient();
        EuclidServiceException exception =
                assertThrows(EuclidServiceException.class, () -> esm.createBucket("photos"));

        assertEquals("esm", exception.service());
        assertEquals("create-bucket", exception.action());
        assertEquals(500, exception.statusCode());
        assertTrue(exception.responseBody().contains("boom"));
    }

    /**
     * Attributes belong on the upload, not on a call after it. EsmServer::handleCompleteUpload is
     * what reads the x-euclid-attributes header, and the object row its background pass writes at
     * the end is built from it - so an attribute added once the upload returns is written to a row
     * that pass then replaces. Asserted on complete-upload rather than create-upload because that
     * is the request the server actually looks at; sending it on the other one is silently
     * ignored, which looks exactly like the attributes never being set.
     */
    @Test
    void uploadFileCarriesItsAttributesOnCompleteUpload(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("data.bin");
        Files.writeString(file, "ABCDEFGHIJ", StandardCharsets.US_ASCII);

        AtomicReference<String> attributesHeader = new AtomicReference<>();

        server = startServer(exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            switch (action) {
                case "create-upload" -> {
                    exchange.getRequestBody().readAllBytes();
                    sendResponse(exchange, 200, "{\"uploadId\":\"upload-1\",\"bucketErn\":\"bucket-ern\",\"key\":\"data.bin\"}");
                }
                case "upload-part" -> {
                    exchange.getRequestBody().readAllBytes();
                    sendResponse(exchange, 200, "{}");
                }
                case "complete-upload" -> {
                    attributesHeader.set(exchange.getRequestHeaders().getFirst("x-euclid-attributes"));
                    sendResponse(exchange, 200,
                        "{\"ern\":\"obj-ern\",\"bucketErn\":\"bucket-ern\",\"key\":\"data.bin\","
                                + "\"size\":10,\"status\":\"AVAILABLE\",\"contentType\":\"application/octet-stream\",\"md5Sum\":\"abc\"}");
                }
                default -> sendResponse(exchange, 500, "{\"error\":\"unexpected action " + action + "\"}");
            }
        });

        newClient().uploadFile("bucket-ern", "data.bin", file,
                Map.of("file_origin", new Variant("string", "FTP_UPLOAD")));

        assertEquals("{\"file_origin\":{\"type\":\"string\",\"value\":\"FTP_UPLOAD\"}}", attributesHeader.get());
    }

    /** An upload with nothing to say sends no header at all, rather than an empty object. */
    @Test
    void uploadFileWithoutAttributesSendsNoAttributesHeader(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("data.bin");
        Files.writeString(file, "ABCDEFGHIJ", StandardCharsets.US_ASCII);

        AtomicReference<String> attributesHeader = new AtomicReference<>();

        server = startServer(exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            switch (action) {
                case "create-upload" -> {
                    exchange.getRequestBody().readAllBytes();
                    sendResponse(exchange, 200, "{\"uploadId\":\"upload-1\",\"bucketErn\":\"bucket-ern\",\"key\":\"data.bin\"}");
                }
                case "upload-part" -> {
                    exchange.getRequestBody().readAllBytes();
                    sendResponse(exchange, 200, "{}");
                }
                case "complete-upload" -> {
                    attributesHeader.set(exchange.getRequestHeaders().getFirst("x-euclid-attributes"));
                    sendResponse(exchange, 200,
                        "{\"ern\":\"obj-ern\",\"bucketErn\":\"bucket-ern\",\"key\":\"data.bin\","
                                + "\"size\":10,\"status\":\"AVAILABLE\",\"contentType\":\"application/octet-stream\",\"md5Sum\":\"abc\"}");
                }
                default -> sendResponse(exchange, 500, "{\"error\":\"unexpected action " + action + "\"}");
            }
        });

        newClient().uploadFile("bucket-ern", "data.bin", file);

        assertNull(attributesHeader.get());
    }

    @Test
    void uploadFileSplitsIntoPartsUploadsThemAndCompletes(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("data.bin");
        Files.writeString(file, "ABCDEFGHIJ", StandardCharsets.US_ASCII);

        ConcurrentHashMap<Long, byte[]> partsByNumber = new ConcurrentHashMap<>();
        AtomicReference<String> createUploadConcurrencyHeader = new AtomicReference<>();
        AtomicReference<SignableRequest> completeUploadRequest = new AtomicReference<>();

        server = startServer(exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            switch (action) {
                case "create-upload" -> {
                    createUploadConcurrencyHeader.set(exchange.getRequestHeaders().getFirst("x-euclid-expected-concurrency"));
                    exchange.getRequestBody().readAllBytes();
                    sendResponse(exchange, 200, "{\"uploadId\":\"upload-1\",\"bucketErn\":\"bucket-ern\",\"key\":\"data.bin\"}");
                }
                case "upload-part" -> {
                    long partNumber = Long.parseLong(exchange.getRequestHeaders().getFirst("x-euclid-part-number"));
                    partsByNumber.put(partNumber, exchange.getRequestBody().readAllBytes());
                    sendResponse(exchange, 200, "{}");
                }
                case "complete-upload" -> {
                    completeUploadRequest.set(captureRequest(exchange));
                    sendResponse(exchange, 200, "{\"ern\":\"obj-ern\",\"bucketErn\":\"bucket-ern\",\"key\":\"data.bin\","
                            + "\"size\":10,\"status\":\"AVAILABLE\",\"contentType\":\"application/octet-stream\",\"md5Sum\":\"abc\"}");
                }
                default -> sendResponse(exchange, 500, "{\"error\":\"unexpected action " + action + "\"}");
            }
        });

        CompleteUploadResponse response = newClient().uploadFile("bucket-ern", "data.bin", file, 4, 2);

        assertEquals("2", createUploadConcurrencyHeader.get());
        assertEquals(3, partsByNumber.size(), "10 bytes split into 4-byte parts should yield 3 parts");

        SortedMap<Long, byte[]> ordered = new TreeMap<>(partsByNumber);
        ByteArrayOutputStream reassembled = new ByteArrayOutputStream();
        for (byte[] part : ordered.values()) {
            reassembled.writeBytes(part);
        }
        assertArrayEquals("ABCDEFGHIJ".getBytes(StandardCharsets.US_ASCII), reassembled.toByteArray());
        assertArrayEquals("ABCD".getBytes(StandardCharsets.US_ASCII), ordered.get(1L));
        assertArrayEquals("IJ".getBytes(StandardCharsets.US_ASCII), ordered.get(3L));

        assertBodyContains(completeUploadRequest.get().body(), "\"uploadId\":\"upload-1\"");
        assertEquals("obj-ern", response.ern());
        assertEquals(10, response.size());
    }

    // create-upload and complete-upload bracket every part of an upload, so a transient 5xx on
    // either one discards the whole file - they retry on the same terms the parts between them do.
    @Test
    void uploadFileRetriesTransientServerErrorsOnCreateAndCompleteUpload(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("data.bin");
        Files.writeString(file, "ABCD", StandardCharsets.US_ASCII);

        AtomicInteger createUploadAttempts = new AtomicInteger();
        AtomicInteger completeUploadAttempts = new AtomicInteger();

        server = startServer(exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            exchange.getRequestBody().readAllBytes();
            switch (action) {
                case "create-upload" -> {
                    if (createUploadAttempts.incrementAndGet() == 1) {
                        sendResponse(exchange, 500, "{\"error\":\"internal server error\"}");
                    } else {
                        sendResponse(exchange, 200, "{\"uploadId\":\"upload-1\",\"bucketErn\":\"bucket-ern\",\"key\":\"data.bin\"}");
                    }
                }
                case "upload-part" -> sendResponse(exchange, 200, "{}");
                case "complete-upload" -> {
                    if (completeUploadAttempts.incrementAndGet() == 1) {
                        sendResponse(exchange, 500, "{\"error\":\"internal server error\"}");
                    } else {
                        sendResponse(exchange, 200, "{\"ern\":\"obj-ern\",\"bucketErn\":\"bucket-ern\",\"key\":\"data.bin\","
                                + "\"size\":4,\"status\":\"AVAILABLE\",\"contentType\":\"application/octet-stream\",\"md5Sum\":\"abc\"}");
                    }
                }
                default -> sendResponse(exchange, 500, "{\"error\":\"unexpected action " + action + "\"}");
            }
        });

        CompleteUploadResponse response = newClient().uploadFile("bucket-ern", "data.bin", file, 4, 1);

        assertEquals(2, createUploadAttempts.get(), "create-upload should have been retried once after the 500");
        assertEquals(2, completeUploadAttempts.get(), "complete-upload should have been retried once after the 500");
        assertEquals("obj-ern", response.ern());
    }

    // A 4xx says the request itself is wrong, so repeating it can only waste the caller's time.
    @Test
    void uploadFileDoesNotRetryClientErrorOnCreateUpload(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("data.bin");
        Files.writeString(file, "ABCD", StandardCharsets.US_ASCII);

        AtomicInteger createUploadAttempts = new AtomicInteger();
        server = startServer(exchange -> {
            createUploadAttempts.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 404, "{\"error\":\"Bucket not found, ern: bucket-ern\"}");
        });

        EuclidEsm esm = newClient();
        EuclidServiceException exception =
                assertThrows(EuclidServiceException.class, () -> esm.uploadFile("bucket-ern", "data.bin", file, 4, 1));

        assertEquals(1, createUploadAttempts.get(), "a 4xx should fail on the first attempt");
        assertEquals("esm", exception.service());
        assertEquals("create-upload", exception.action());
        assertEquals(404, exception.statusCode());
        assertTrue(exception.responseBody().contains("Bucket not found"));
    }

    @Test
    void uploadFileWithEmptyFileUploadsSingleEmptyPart(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("empty.bin");
        Files.createFile(file);

        ConcurrentHashMap<Long, byte[]> partsByNumber = new ConcurrentHashMap<>();
        server = startServer(exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            switch (action) {
                case "create-upload" -> {
                    exchange.getRequestBody().readAllBytes();
                    sendResponse(exchange, 200, "{\"uploadId\":\"upload-1\",\"bucketErn\":\"bucket-ern\",\"key\":\"empty.bin\"}");
                }
                case "upload-part" -> {
                    long partNumber = Long.parseLong(exchange.getRequestHeaders().getFirst("x-euclid-part-number"));
                    partsByNumber.put(partNumber, exchange.getRequestBody().readAllBytes());
                    sendResponse(exchange, 200, "{}");
                }
                case "complete-upload" -> {
                    exchange.getRequestBody().readAllBytes();
                    sendResponse(exchange, 200, "{\"ern\":\"obj-ern\",\"bucketErn\":\"bucket-ern\",\"key\":\"empty.bin\","
                            + "\"size\":0,\"status\":\"AVAILABLE\",\"contentType\":\"application/octet-stream\",\"md5Sum\":\"abc\"}");
                }
                default -> sendResponse(exchange, 500, "{\"error\":\"unexpected action " + action + "\"}");
            }
        });

        CompleteUploadResponse response = newClient().uploadFile("bucket-ern", "empty.bin", file);

        assertEquals(1, partsByNumber.size());
        assertArrayEquals(new byte[0], partsByNumber.get(1L));
        assertEquals(0, response.size());
    }

    @Test
    void uploadFileRetriesTransientPartFailureAndSucceeds(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("data.bin");
        Files.writeString(file, "hello world", StandardCharsets.US_ASCII);

        AtomicInteger uploadPartAttempts = new AtomicInteger();
        server = startServer(exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            switch (action) {
                case "create-upload" -> {
                    exchange.getRequestBody().readAllBytes();
                    sendResponse(exchange, 200, "{\"uploadId\":\"upload-1\",\"bucketErn\":\"bucket-ern\",\"key\":\"data.bin\"}");
                }
                case "upload-part" -> {
                    exchange.getRequestBody().readAllBytes();
                    if (uploadPartAttempts.getAndIncrement() == 0) {
                        sendResponse(exchange, 503, "{\"error\":\"transient\"}");
                    } else {
                        sendResponse(exchange, 200, "{}");
                    }
                }
                case "complete-upload" -> {
                    exchange.getRequestBody().readAllBytes();
                    sendResponse(exchange, 200, "{\"ern\":\"obj-ern\",\"bucketErn\":\"bucket-ern\",\"key\":\"data.bin\","
                            + "\"size\":11,\"status\":\"AVAILABLE\",\"contentType\":\"application/octet-stream\",\"md5Sum\":\"abc\"}");
                }
                default -> sendResponse(exchange, 500, "{\"error\":\"unexpected action " + action + "\"}");
            }
        });

        CompleteUploadResponse response = newClient().uploadFile("bucket-ern", "data.bin", file, 1024, 1);

        assertEquals(2, uploadPartAttempts.get(), "should retry once after the transient 503 and then succeed");
        assertEquals("obj-ern", response.ern());
    }

    @Test
    void uploadFileThrowsWhenPartUploadPermanentlyFails(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("data.bin");
        Files.writeString(file, "hello world", StandardCharsets.US_ASCII);

        server = startServer(exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            if ("create-upload".equals(action)) {
                exchange.getRequestBody().readAllBytes();
                sendResponse(exchange, 200, "{\"uploadId\":\"upload-1\",\"bucketErn\":\"bucket-ern\",\"key\":\"data.bin\"}");
            } else if ("upload-part".equals(action)) {
                exchange.getRequestBody().readAllBytes();
                sendResponse(exchange, 400, "{\"error\":\"bad request\"}");
            } else {
                sendResponse(exchange, 500, "{\"error\":\"unexpected action " + action + "\"}");
            }
        });

        EuclidEsm esm = newClient();
        IOException exception = assertThrows(IOException.class,
                () -> esm.uploadFile("bucket-ern", "data.bin", file, 1024, 1));
        assertTrue(exception.getMessage().contains("upload-file failed"));
    }

    @Test
    void uploadPartAlwaysUsesBearerTokenEvenWithAccessKeyConfigured(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("data.bin");
        Files.writeString(file, "hello world", StandardCharsets.US_ASCII);

        AtomicReference<String> createUploadAuth = new AtomicReference<>();
        AtomicReference<String> uploadPartAuth = new AtomicReference<>();
        server = startServer(exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            switch (action) {
                case "create-upload" -> {
                    createUploadAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
                    exchange.getRequestBody().readAllBytes();
                    sendResponse(exchange, 200, "{\"uploadId\":\"upload-1\",\"bucketErn\":\"bucket-ern\",\"key\":\"data.bin\"}");
                }
                case "upload-part" -> {
                    uploadPartAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
                    exchange.getRequestBody().readAllBytes();
                    sendResponse(exchange, 200, "{}");
                }
                case "complete-upload" -> {
                    exchange.getRequestBody().readAllBytes();
                    sendResponse(exchange, 200, "{\"ern\":\"obj-ern\",\"bucketErn\":\"bucket-ern\",\"key\":\"data.bin\","
                            + "\"size\":11,\"status\":\"AVAILABLE\",\"contentType\":\"application/octet-stream\",\"md5Sum\":\"abc\"}");
                }
                default -> sendResponse(exchange, 500, "{\"error\":\"unexpected action " + action + "\"}");
            }
        });

        EuclidEsm esm = new EuclidEsm(baseUrl(), "my-jwt-token", "eu-central-1", "863459426936", "alice",
                "AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY", null, null);
        esm.uploadFile("bucket-ern", "data.bin", file, 1024, 1);

        assertTrue(createUploadAuth.get().startsWith("AWS4-HMAC-SHA256 "), "create-upload should be SigV4-signed");
        assertEquals("Bearer my-jwt-token", uploadPartAuth.get());
    }

    @Test
    void createBucketSendsNamespaceHeaderWhenConfigured() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"name\":\"photos\",\"ern\":\"bucket-ern\"}");
        });

        new EuclidEsm(baseUrl(), "test-token", "eu-central-1", "863459426936", "alice", null, null, null, "prod")
                .createBucket("photos");

        assertEquals("prod", received.get().header("x-euclid-namespace"));
    }

    @Test
    void createBucketOmitsNamespaceHeaderWhenUnset() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"name\":\"photos\",\"ern\":\"bucket-ern\"}");
        });

        newClient().createBucket("photos");

        assertEquals("", received.get().header("x-euclid-namespace"));
    }

    @Test
    void getObjectCountSendsBucketErnAndPrefix() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"ern\":\"bucket-ern\",\"count\":42}");
        });

        GetObjectCountResponse response = newClient().getObjectCount("bucket-ern", "photos/");

        assertBodyContains(received.get().body(), "\"ern\":\"bucket-ern\"", "\"prefix\":\"photos/\"");
        assertEquals("bucket-ern", response.ern());
        assertEquals(42, response.count());
    }

    @Test
    void bucketTagActionsSendTheKeyAndValue() throws Exception {
        Map<String, String> bodyByAction = new ConcurrentHashMap<>();
        server = startServer(exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            bodyByAction.put(action, new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendResponse(exchange, 200, "{}");
        });

        EuclidEsm esm = newClient();
        esm.addBucketTag("bucket-ern", "owner", "alice");
        esm.setBucketTag("bucket-ern", "owner", "bob");
        esm.deleteBucketTag("bucket-ern", "owner");

        assertBodyContains(bodyByAction.get("add-bucket-tag"), "\"ern\":\"bucket-ern\"", "\"key\":\"owner\"", "\"value\":\"alice\"");
        assertBodyContains(bodyByAction.get("set-bucket-tag"), "\"ern\":\"bucket-ern\"", "\"key\":\"owner\"", "\"value\":\"bob\"");
        assertBodyContains(bodyByAction.get("delete-bucket-tag"), "\"ern\":\"bucket-ern\"", "\"key\":\"owner\"");
    }

    @Test
    void objectAttributeActionsRoundTripTypedValues() throws Exception {
        Map<String, String> bodyByAction = new ConcurrentHashMap<>();
        server = startServer(exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            bodyByAction.put(action, new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            switch (action) {
                case "add-object-attribute", "set-object-attribute" -> sendResponse(exchange, 200,
                        "{\"ern\":\"obj-ern\",\"name\":\"revision\",\"value\":{\"type\":\"long\",\"value\":\"7\"}}");
                case "list-object-attributes" -> sendResponse(exchange, 200,
                        "{\"ern\":\"obj-ern\",\"total\":2,\"attributes\":{\"revision\":{\"type\":\"long\",\"value\":\"7\"},"
                                + "\"source\":{\"type\":\"string\",\"value\":\"pim\"}}}");
                default -> sendResponse(exchange, 200, "{}");
            }
        });

        EuclidEsm esm = newClient();
        ObjectAttributeResponse added = esm.addObjectAttribute("obj-ern", "revision", new Variant("long", 7L));
        esm.setObjectAttribute("obj-ern", "revision", new Variant("long", 8L));
        ListObjectAttributesResponse listed = esm.listObjectAttributes("obj-ern");
        esm.deleteObjectAttribute("obj-ern", "revision");

        assertBodyContains(bodyByAction.get("add-object-attribute"), "\"ern\":\"obj-ern\"", "\"name\":\"revision\"",
                "\"value\":{\"type\":\"long\",\"value\":7}");
        assertBodyContains(bodyByAction.get("set-object-attribute"), "\"value\":{\"type\":\"long\",\"value\":8}");
        assertBodyContains(bodyByAction.get("delete-object-attribute"), "\"ern\":\"obj-ern\"", "\"name\":\"revision\"");

        assertEquals("revision", added.name());
        assertEquals("long", added.value().type());
        assertEquals(2, listed.total());
        assertEquals("pim", listed.attributes().get("source").value());
        assertEquals("long", listed.attributes().get("revision").type());
    }

    @Test
    void subscriptionActionsRoundTrip() throws Exception {
        Map<String, String> bodyByAction = new ConcurrentHashMap<>();
        server = startServer(exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            bodyByAction.put(action, new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            switch (action) {
                case "subscribe" -> sendResponse(exchange, 200, "{\"ern\":\"sub-ern\",\"sourceErn\":\"bucket-ern\","
                        + "\"type\":\"queue\",\"targetErn\":\"queue-ern\"}");
                case "list-subscriptions" -> sendResponse(exchange, 200, "{\"total\":1,\"subscriptions\":[{"
                        + "\"ern\":\"sub-ern\",\"sourceErn\":\"bucket-ern\",\"type\":\"queue\",\"targetErn\":\"queue-ern\","
                        + "\"created\":\"2026-09-01T00:00:00Z\",\"modified\":\"2026-09-01T00:00:00Z\"}]}");
                default -> sendResponse(exchange, 200, "{}");
            }
        });

        EuclidEsm esm = newClient();
        SubscribeResponse subscribed = esm.subscribe("bucket-ern", "queue", "queue-ern");
        ListSubscriptionsResponse listed = esm.listSubscriptions("bucket-ern");
        esm.unsubscribe(subscribed.ern());

        assertBodyContains(bodyByAction.get("subscribe"), "\"sourceErn\":\"bucket-ern\"", "\"type\":\"queue\"",
                "\"targetErn\":\"queue-ern\"");
        assertBodyContains(bodyByAction.get("list-subscriptions"), "\"bucketErn\":\"bucket-ern\"");
        // unsubscribe takes the subscription's own ERN, not the bucket's or the target's.
        assertBodyContains(bodyByAction.get("unsubscribe"), "\"ern\":\"sub-ern\"");

        assertEquals("sub-ern", subscribed.ern());
        assertEquals(1, listed.total());
        assertEquals("queue-ern", listed.subscriptions().getFirst().targetErn());
    }

    @Test
    void putObjectSendsRawBytesWithBucketAndKeyHeaders() throws Exception {
        AtomicReference<byte[]> body = new AtomicReference<>();
        AtomicReference<Headers> headers = new AtomicReference<>();
        server = startServer(exchange -> {
            headers.set(exchange.getRequestHeaders());
            body.set(exchange.getRequestBody().readAllBytes());
            sendResponse(exchange, 200, "{}");
        });

        newClient().putObject("bucket-ern", "notes.txt", "hello".getBytes(StandardCharsets.US_ASCII));

        assertArrayEquals("hello".getBytes(StandardCharsets.US_ASCII), body.get());
        assertEquals("bucket-ern", headers.get().getFirst("x-euclid-bucket-ern"));
        assertEquals("notes.txt", headers.get().getFirst("x-euclid-key"));
        assertEquals("put-object", headers.get().getFirst("x-euclid-action"));
    }

    @Test
    void getObjectReturnsRawBytes() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendBinaryResponse(exchange, 200, "hello".getBytes(StandardCharsets.US_ASCII));
        });

        assertArrayEquals("hello".getBytes(StandardCharsets.US_ASCII),
                newClient().getObject("bucket-ern", "notes.txt", 1024));
    }

    @Test
    void downloadFileFetchesSmallObjectInOneRequest(@TempDir Path tempDir) throws Exception {
        AtomicInteger requests = new AtomicInteger();
        server = startServer(exchange -> {
            requests.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            sendBinaryResponse(exchange, 200, "ABCDEFGHIJ".getBytes(StandardCharsets.US_ASCII));
        });

        Path file = tempDir.resolve("nested/data.bin");
        long written = newClient().downloadFile("bucket-ern", "data.bin", file, 1024, 2);

        assertEquals(10, written);
        assertEquals(1, requests.get(), "an object that fits in one part should skip the multipart flow");
        assertArrayEquals("ABCDEFGHIJ".getBytes(StandardCharsets.US_ASCII), Files.readAllBytes(file));
    }

    // A download's size isn't known until asked, so the single-request path is always tried first
    // and HTTP 413 is what tells the client the object needs create-download/download-part instead.
    @Test
    void downloadFileFallsBackToMultipartWhenObjectIsTooLarge(@TempDir Path tempDir) throws Exception {
        byte[] content = "ABCDEFGHIJ".getBytes(StandardCharsets.US_ASCII);
        AtomicInteger completeDownloads = new AtomicInteger();
        AtomicReference<String> createDownloadConcurrencyHeader = new AtomicReference<>();

        server = startServer(exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            exchange.getRequestBody().readAllBytes();
            switch (action) {
                case "get-object" -> sendResponse(exchange, 413, "{\"error\":\"object too large\"}");
                case "create-download" -> {
                    createDownloadConcurrencyHeader.set(exchange.getRequestHeaders().getFirst("x-euclid-expected-concurrency"));
                    sendResponse(exchange, 200, "{\"downloadId\":\"download-1\",\"bucketErn\":\"bucket-ern\","
                            + "\"key\":\"data.bin\",\"ern\":\"obj-ern\",\"size\":10,\"contentType\":\"application/octet-stream\"}");
                }
                case "download-part" -> {
                    int partNumber = Integer.parseInt(exchange.getRequestHeaders().getFirst("x-euclid-part-number"));
                    int partSize = Integer.parseInt(exchange.getRequestHeaders().getFirst("x-euclid-part-size"));
                    int from = (partNumber - 1) * partSize;
                    int to = Math.min(from + partSize, content.length);
                    sendBinaryResponse(exchange, 200, Arrays.copyOfRange(content, from, to));
                }
                case "complete-download" -> {
                    completeDownloads.incrementAndGet();
                    sendResponse(exchange, 200, "{}");
                }
                default -> sendResponse(exchange, 500, "{\"error\":\"unexpected action " + action + "\"}");
            }
        });

        Path file = tempDir.resolve("data.bin");
        long written = newClient().downloadFile("bucket-ern", "data.bin", file, 4, 2);

        assertEquals(10, written);
        assertEquals("2", createDownloadConcurrencyHeader.get());
        assertEquals(1, completeDownloads.get());
        assertArrayEquals(content, Files.readAllBytes(file), "parts should be reassembled in order");
    }

    @Test
    void listRequestsCarrySortDirectionAndIncludeDirectories() throws Exception {
        Map<String, String> bodyByAction = new ConcurrentHashMap<>();
        server = startServer(exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            bodyByAction.put(action, new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendResponse(exchange, 200, "{\"total\":0,\"objects\":[],\"buckets\":[]}");
        });

        EuclidEsm esm = newClient();
        esm.listObjects("bucket-ern", "", 10, 0, "name", "desc", true);
        esm.listBuckets("", 10, 0, "name", "desc");

        assertBodyContains(bodyByAction.get("list-objects"), "\"sortDirection\":\"desc\"", "\"includeDirectories\":true");
        assertBodyContains(bodyByAction.get("list-buckets"), "\"sortDirection\":\"desc\"");
    }

    // Both defaults matter: the no-direction overloads have to keep sending something the server
    // accepts rather than dropping the field, and directories stay out of a listing unless asked for.
    @Test
    void listRequestsDefaultToAscendingWithoutDirectories() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"total\":0,\"objects\":[]}");
        });

        newClient().listObjects("bucket-ern");

        assertBodyContains(received.get().body(), "\"sortDirection\":\"asc\"", "\"includeDirectories\":false");
    }

    @Test
    void listResponsesCarryBucketTagsAndObjectAttributes() throws Exception {
        server = startServer(exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            exchange.getRequestBody().readAllBytes();
            if ("list-buckets".equals(action)) {
                sendResponse(exchange, 200, "{\"total\":1,\"buckets\":[{\"owner\":\"alice\",\"name\":\"photos\","
                        + "\"ern\":\"bucket-ern\",\"size\":10,\"objects\":1,\"tags\":{\"team\":\"platform\"},"
                        + "\"created\":\"2026-09-01T00:00:00Z\",\"modified\":\"2026-09-01T00:00:00Z\"}]}");
            } else {
                sendResponse(exchange, 200, "{\"total\":1,\"objects\":[{\"ern\":\"obj-ern\",\"bucketErn\":\"bucket-ern\","
                        + "\"key\":\"a.bin\",\"size\":10,\"status\":\"COMPLETED\",\"contentType\":\"application/octet-stream\","
                        + "\"md5Sum\":\"abc\",\"attributes\":{\"source\":{\"type\":\"string\",\"value\":\"pim\"}},"
                        + "\"created\":\"2026-09-01T00:00:00Z\",\"modified\":\"2026-09-01T00:00:00Z\"}]}");
            }
        });

        EuclidEsm esm = newClient();
        List<Bucket> buckets = esm.listBuckets().buckets();
        ListObjectsResponse objects = esm.listObjects("bucket-ern");

        assertEquals("platform", buckets.getFirst().tags().get("team"));
        assertEquals("pim", objects.objects().getFirst().attributes().get("source").value());
    }

    @Test
    void copyAndMoveObjectSendBothEndsAndParseTheStoredObject() throws Exception {
        Map<String, String> bodyByAction = new ConcurrentHashMap<>();
        server = startServer(exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            bodyByAction.put(action, new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendResponse(exchange, 200, "{\"ern\":\"obj-ern\",\"bucketErn\":\"target-ern\",\"key\":\"b.bin\","
                    + "\"size\":10,\"status\":\"COMPLETED\",\"contentType\":\"application/octet-stream\","
                    + "\"md5Sum\":\"abc\",\"attributes\":{},\"created\":\"2026-01-01\",\"modified\":\"2026-01-02\"}");
        });

        EuclidEsm esm = newClient();
        EsmObject copied = esm.copyObject("source-ern", "a.bin", "target-ern", "b.bin");
        esm.moveObject("source-ern", "a.bin", "target-ern", "b.bin");

        for (String action : List.of("copy-object", "move-object")) {
            assertBodyContains(bodyByAction.get(action), "\"sourceBucketErn\":\"source-ern\"",
                    "\"sourceKey\":\"a.bin\"", "\"targetBucketErn\":\"target-ern\"", "\"targetKey\":\"b.bin\"");
        }

        assertEquals("obj-ern", copied.ern());
        assertEquals("target-ern", copied.bucketErn());
        assertEquals("b.bin", copied.key());
        assertEquals("abc", copied.md5Sum());
        assertEquals(10, copied.size());
    }

    // rename-object is a move that cannot leave the bucket, so it names one bucket and two keys
    // rather than two of each.
    @Test
    void renameObjectSendsOneBucketAndTwoKeys() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"ern\":\"obj-ern\",\"bucketErn\":\"bucket-ern\",\"key\":\"new.bin\","
                    + "\"size\":10,\"status\":\"COMPLETED\",\"attributes\":{}}");
        });

        EsmObject renamed = newClient().renameObject("bucket-ern", "old.bin", "new.bin");

        assertEquals("rename-object", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"bucketErn\":\"bucket-ern\"", "\"key\":\"old.bin\"",
                "\"newKey\":\"new.bin\"");
        assertEquals("new.bin", renamed.key());
    }

    // The server refuses to silently replace an object at the target rather than overwriting it.
    @Test
    void copyObjectSurfacesAConflictAtTheTarget() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 409, "{\"error\":\"Object already exists, bucket: target-ern, key: b.bin\"}");
        });

        EuclidEsm esm = newClient();
        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> esm.copyObject("source-ern", "a.bin", "target-ern", "b.bin"));

        assertEquals("esm", exception.service());
        assertEquals("copy-object", exception.action());
        assertEquals(409, exception.statusCode());
        assertTrue(exception.responseBody().contains("Object already exists"));
    }

    // A bucket subscription delivers its notification as the body of an ordinary queue or topic
    // message, so the consumer's job is to turn that body into something typed.
    @Test
    void parseBucketEventReadsANotificationDeliveredToAQueue() throws Exception {
        String messageBody = "{\"eventType\":\"esm:ObjectCreated:Put\","
                + "\"bucketErn\":\"ern:esm:eu-central-1:863459426936:bucket/invoices\","
                + "\"key\":\"invoices/2026/invoice-1.pdf\","
                + "\"ern\":\"ern:esm:eu-central-1:863459426936:object/invoice-1.pdf\","
                + "\"size\":1024,\"contentType\":\"application/pdf\",\"md5Sum\":\"abc\"}";

        BucketEvent event = EuclidEsm.parseBucketEvent(messageBody);

        assertEquals("esm:ObjectCreated:Put", event.eventType());
        assertEquals("ern:esm:eu-central-1:863459426936:bucket/invoices", event.bucketErn());
        assertEquals("invoices/2026/invoice-1.pdf", event.key());
        assertEquals("ern:esm:eu-central-1:863459426936:object/invoice-1.pdf", event.ern());
        assertEquals(1024, event.size());
        assertEquals("application/pdf", event.contentType());
        assertEquals("abc", event.md5Sum());
    }

    // A notification missing an optional field is still readable rather than throwing - a consumer
    // draining a queue should not stop on one sparse message.
    @Test
    void parseBucketEventToleratesMissingFields() throws Exception {
        BucketEvent event = EuclidEsm.parseBucketEvent(
                "{\"eventType\":\"esm:ObjectCreated:Put\",\"key\":\"a.bin\"}");

        assertEquals("esm:ObjectCreated:Put", event.eventType());
        assertEquals("a.bin", event.key());
        assertEquals(0, event.size());
        assertNull(event.contentType());
    }

    @Test
    void parseBucketEventRejectsABodyThatIsNotJson() {
        assertThrows(IOException.class, () -> EuclidEsm.parseBucketEvent("not json at all"));
    }

    private EuclidEsm newClient() {
        return new EuclidEsm(baseUrl(), "test-token", "eu-central-1", "863459426936", "alice", null, null, null, null);
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

    private static void sendBinaryResponse(HttpExchange exchange, int status, byte[] bytes) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
