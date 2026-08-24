package de.jensvogt.euclid.module.esm;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.jensvogt.euclid.auth.SigV4;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.dto.esm.CompleteUploadResponse;
import de.jensvogt.euclid.dto.esm.CreateBucketResponse;
import de.jensvogt.euclid.dto.esm.GetBucketErnResponse;
import de.jensvogt.euclid.dto.esm.GetBucketSizeResponse;
import de.jensvogt.euclid.dto.esm.ListObjectsResponse;
import de.jensvogt.euclid.dto.esm.PurgeBucketResponse;
import de.jensvogt.euclid.dto.esm.model.Bucket;
import de.jensvogt.euclid.exception.EuclidAuthenticationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms EuclidEsm authenticates the way it claims to (SigV4-signed when an access key is
 * configured, bearer token otherwise, mirroring euclid-cli's HttpClient.cpp), routes every
 * bucket/object action to the right request with a correctly-shaped body, parses the
 * corresponding response, surfaces non-2xx responses as {@link EuclidAuthenticationException},
 * and - for {@code uploadFile} - correctly orchestrates create-upload/upload-part/complete-upload
 * including splitting, retrying, and always using the bearer token for the binary part uploads.
 */
class EuclidEsmTest {

    private static final List<String> SIGNED_HEADERS = List.of("host", "x-amz-content-sha256", "x-amz-date",
            "x-euclid-account-id", "x-euclid-action", "x-euclid-region", "x-euclid-target", "x-euclid-user-id");

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
                accessKeyId, secretAccessKey, null);
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
                null, null, null);
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
                    + "\"created\":\"2026-01-01\",\"modified\":\"2026-01-02\"}]}");
        });

        List<Bucket> buckets = newClient().listBuckets();

        assertEquals("list-buckets", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"prefix\":\"\"", "\"pageSize\":10", "\"pageIndex\":0",
                "\"sortColumn\":\"name\"");
        assertEquals(1, buckets.size());
        assertEquals("photos", buckets.get(0).name());
        assertEquals(3, buckets.get(0).objects());
    }

    @Test
    void listBucketsWithExplicitParameters() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"buckets\":[]}");
        });

        List<Bucket> buckets = newClient().listBuckets("pho", 25, 2, "created");

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
        assertEquals("a.txt", response.objects().get(0).key());
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
    void nonSuccessResponseThrowsEuclidAuthenticationException() throws Exception {
        server = startServer(exchange -> sendResponse(exchange, 500, "{\"error\":\"boom\"}"));

        EuclidEsm esm = newClient();
        EuclidAuthenticationException exception =
                assertThrows(EuclidAuthenticationException.class, () -> esm.createBucket("photos"));

        assertEquals(500, exception.statusCode());
        assertTrue(exception.responseBody().contains("boom"));
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
                "AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY", null);
        esm.uploadFile("bucket-ern", "data.bin", file, 1024, 1);

        assertTrue(createUploadAuth.get().startsWith("AWS4-HMAC-SHA256 "), "create-upload should be SigV4-signed");
        assertEquals("Bearer my-jwt-token", uploadPartAuth.get());
    }

    private EuclidEsm newClient() {
        return new EuclidEsm(baseUrl(), "test-token", "eu-central-1", "863459426936", "alice", null, null, null);
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
