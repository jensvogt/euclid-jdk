package de.jensvogt.euclid.module.ekm;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.jensvogt.euclid.auth.SigV4;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.dto.ekm.CreateKeyResponse;
import de.jensvogt.euclid.dto.ekm.DeleteKeyResponse;
import de.jensvogt.euclid.dto.ekm.ListKeysResponse;
import de.jensvogt.euclid.dto.ekm.RevokeKeyResponse;
import de.jensvogt.euclid.dto.ekm.model.Key;
import de.jensvogt.euclid.exception.EuclidServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms EuclidEkm authenticates the way it claims to (SigV4-signed when an access key is
 * configured, bearer token otherwise, mirroring euclid-cli's HttpClient.cpp), routes every key
 * action to the right request with a correctly-shaped body, parses the corresponding response, and
 * surfaces non-2xx responses as {@link EuclidServiceException}.
 */
class EuclidEkmTest {

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
    void createKeySignsWithSigV4WhenAccessKeyConfigured() throws Exception {
        String accessKeyId = "AKIDEXAMPLE";
        String secretAccessKey = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";

        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"ern\":\"key-ern\",\"name\":\"key-1\",\"status\":\"AVAILABLE\"}");
        });

        EuclidEkm ekm = new EuclidEkm(baseUrl(), "unused-token", "eu-central-1", "863459426936", "alice",
                accessKeyId, secretAccessKey, null, null);
        ekm.createKey();

        SignableRequest req = received.get();
        assertTrue(req.header("authorization").startsWith("AWS4-HMAC-SHA256 "));

        Optional<SigV4.VerifyResult> result = SigV4.verify(req,
                id -> id.equals(accessKeyId) ? Optional.of(secretAccessKey) : Optional.empty());
        assertTrue(result.isPresent(), "server-side verification of the client's own signature must succeed");
        assertEquals(accessKeyId, result.get().accessKeyId());
    }

    @Test
    void createKeyUsesBearerTokenWhenNoAccessKeyConfigured() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"ern\":\"key-ern\",\"name\":\"key-1\",\"status\":\"AVAILABLE\"}");
        });

        newClient().createKey();

        assertEquals("Bearer test-token", received.get().header("authorization"));
    }

    @Test
    void createKeyUsesAes128ByDefaultAndParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"ern\":\"key-ern\",\"name\":\"key-1\",\"algorithm\":\"AES\","
                    + "\"length\":128,\"status\":\"AVAILABLE\"}");
        });

        CreateKeyResponse response = newClient().createKey();

        assertEquals("create-key", received.get().header("x-euclid-action"));
        assertEquals("ekm", received.get().header("x-euclid-target"));
        assertBodyContains(received.get().body(), "\"algorithm\":\"AES\"", "\"length\":128");
        assertEquals("key-ern", response.ern());
        // The server mints the key ID; there is no name field on the request to choose one.
        assertEquals("key-1", response.name());
        assertEquals("AVAILABLE", response.status());
    }

    @Test
    void createKeyWithExplicitAlgorithmAndLength() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"ern\":\"key-ern\",\"name\":\"key-1\",\"status\":\"AVAILABLE\"}");
        });

        newClient().createKey("AES", 256);

        assertBodyContains(received.get().body(), "\"algorithm\":\"AES\"", "\"length\":256");
    }

    @Test
    void listKeysUsesDefaultsAndParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"total\":1,\"keys\":[{\"ern\":\"key-ern\",\"name\":\"key-1\","
                    + "\"algorithm\":\"AES\",\"length\":256,\"status\":\"AVAILABLE\",\"tags\":{\"env\":\"prod\"},"
                    + "\"created\":\"2026-01-01\",\"modified\":\"2026-01-02\"}]}");
        });

        ListKeysResponse response = newClient().listKeys();

        assertEquals("list-keys", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"prefix\":\"\"", "\"pageSize\":10", "\"pageIndex\":0",
                "\"sortColumn\":\"name\"", "\"sortDirection\":\"asc\"");

        assertEquals(1, response.total());
        Key key = response.keys().getFirst();
        assertEquals("key-1", key.name());
        assertEquals("key-ern", key.ern());
        assertEquals("AES", key.algorithm());
        assertEquals(256, key.length());
        assertEquals("prod", key.tags().get("env"));
        // The server leaves deletionDate out entirely unless the key is scheduled for deletion.
        assertNull(key.deletionDate());
    }

    @Test
    void listKeysWithExplicitParameters() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"total\":0,\"keys\":[]}");
        });

        ListKeysResponse response = newClient().listKeys("pro", 25, 2, "created", "desc");

        assertBodyContains(received.get().body(), "\"prefix\":\"pro\"", "\"pageSize\":25", "\"pageIndex\":2",
                "\"sortColumn\":\"created\"", "\"sortDirection\":\"desc\"");
        assertTrue(response.keys().isEmpty());
    }

    @Test
    void listKeysCarriesDeletionDateForAKeyScheduledForDeletion() throws Exception {
        server = startServer(exchange -> {
            captureRequest(exchange);
            sendResponse(exchange, 200, "{\"total\":1,\"keys\":[{\"ern\":\"key-ern\",\"name\":\"key-1\","
                    + "\"algorithm\":\"AES\",\"length\":128,\"status\":\"PENDING_DELETION\",\"tags\":{},"
                    + "\"deletionDate\":\"2026-09-08T00:00:00Z\",\"created\":\"2026-01-01\","
                    + "\"modified\":\"2026-01-02\"}]}");
        });

        Key key = newClient().listKeys().keys().getFirst();

        assertEquals("PENDING_DELETION", key.status());
        assertEquals("2026-09-08T00:00:00Z", key.deletionDate());
    }

    @Test
    void deleteKeyDefaultsToASevenDayPendingWindow() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"ern\":\"key-ern\",\"name\":\"key-1\","
                    + "\"deletionDate\":\"2026-09-08T00:00:00Z\",\"status\":\"PENDING_DELETION\"}");
        });

        DeleteKeyResponse response = newClient().deleteKey("key-1");

        assertEquals("delete-key", received.get().header("x-euclid-action"));
        // delete-key addresses the key by ID, unlike revoke-key which takes the ERN.
        assertBodyContains(received.get().body(), "\"keyId\":\"key-1\"", "\"pendingWindowInDays\":7");
        assertEquals("2026-09-08T00:00:00Z", response.deletionDate());
        assertEquals("PENDING_DELETION", response.status());
    }

    @Test
    void deleteKeyWithExplicitPendingWindow() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"ern\":\"key-ern\",\"name\":\"key-1\",\"status\":\"PENDING_DELETION\"}");
        });

        newClient().deleteKey("key-1", 30);

        assertBodyContains(received.get().body(), "\"keyId\":\"key-1\"", "\"pendingWindowInDays\":30");
    }

    @Test
    void revokeKeyTakesTheErnAndParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"ern\":\"key-ern\",\"name\":\"key-1\",\"status\":\"REVOKED\"}");
        });

        RevokeKeyResponse response = newClient().revokeKey("key-ern");

        assertEquals("revoke-key", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"ern\":\"key-ern\"");
        assertEquals("REVOKED", response.status());
    }

    // Plaintext and ciphertext travel as the raw body with the key named in a header, rather than
    // base64 in a JSON field - so the bytes have to survive the round trip untouched.
    @Test
    void encryptAndDecryptSendRawBytesWithTheKeyIdInAHeader() throws Exception {
        byte[] plaintext = {0x00, 0x01, (byte) 0xFE, (byte) 0xFF};
        byte[] ciphertext = {0x7F, (byte) 0x80, 0x00, 0x2A};

        Map<String, byte[]> bodyByAction = new ConcurrentHashMap<>();
        Map<String, String> keyIdByAction = new ConcurrentHashMap<>();
        server = startServer(exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            keyIdByAction.put(action, exchange.getRequestHeaders().getFirst("x-euclid-key-id"));
            bodyByAction.put(action, exchange.getRequestBody().readAllBytes());
            sendBinaryResponse(exchange, 200, "encrypt".equals(action) ? ciphertext : plaintext);
        });

        EuclidEkm ekm = newClient();
        assertArrayEquals(ciphertext, ekm.encrypt("key-1", plaintext));
        assertArrayEquals(plaintext, ekm.decrypt("key-1", ciphertext));

        assertArrayEquals(plaintext, bodyByAction.get("encrypt"));
        assertArrayEquals(ciphertext, bodyByAction.get("decrypt"));
        assertEquals("key-1", keyIdByAction.get("encrypt"));
        assertEquals("key-1", keyIdByAction.get("decrypt"));
    }

    // SigV4 hashes the body as a UTF-8 String, which is lossy for arbitrary bytes, so the two
    // payload-carrying actions authenticate with the bearer token even when a key is configured.
    @Test
    void encryptUsesBearerTokenEvenWhenAccessKeyConfigured() throws Exception {
        AtomicReference<Headers> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(exchange.getRequestHeaders());
            exchange.getRequestBody().readAllBytes();
            sendBinaryResponse(exchange, 200, new byte[]{1});
        });

        EuclidEkm ekm = new EuclidEkm(baseUrl(), "test-token", "eu-central-1", "863459426936", "alice",
                "AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY", null, null);
        ekm.encrypt("key-1", new byte[]{0});

        assertEquals("Bearer test-token", received.get().getFirst("Authorization"));
    }

    @Test
    void tagActionsSendTheKeyErnAndTag() throws Exception {
        Map<String, String> bodyByAction = new ConcurrentHashMap<>();
        server = startServer(exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            bodyByAction.put(action, new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendResponse(exchange, 200, "{}");
        });

        EuclidEkm ekm = newClient();
        ekm.addKeyTag("key-ern", "env", "prod");
        ekm.deleteKeyTag("key-ern", "env");

        assertBodyContains(bodyByAction.get("add-key-tag"), "\"ern\":\"key-ern\"", "\"key\":\"env\"",
                "\"value\":\"prod\"");
        assertBodyContains(bodyByAction.get("delete-key-tag"), "\"ern\":\"key-ern\"", "\"key\":\"env\"");
    }

    @Test
    void namespaceIsSentWhenTheSessionIsScoped() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"total\":0,\"keys\":[]}");
        });

        new EuclidEkm(baseUrl(), "test-token", "eu-central-1", "863459426936", "alice", null, null, null, "prod")
                .listKeys();

        assertEquals("prod", received.get().header("x-euclid-namespace"));
    }

    @Test
    void nonSuccessResponseThrowsEuclidServiceException() throws Exception {
        server = startServer(exchange -> sendResponse(exchange, 500, "{\"error\":\"boom\"}"));

        EuclidEkm ekm = newClient();
        EuclidServiceException exception = assertThrows(EuclidServiceException.class, ekm::createKey);

        assertEquals("ekm", exception.service());
        assertEquals("create-key", exception.action());
        assertEquals(500, exception.statusCode());
        assertTrue(exception.responseBody().contains("boom"));
    }

    // An encrypt/decrypt failure comes back as a JSON error body in a response the client is
    // otherwise reading as raw bytes, so it has to be decoded before it can be reported.
    @Test
    void nonSuccessBinaryResponseCarriesTheServersErrorBody() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 403, "{\"error\":\"Key 'key-1' is REVOKED and cannot be used for encryption\"}");
        });

        EuclidEkm ekm = newClient();
        EuclidServiceException exception =
                assertThrows(EuclidServiceException.class, () -> ekm.encrypt("key-1", new byte[]{0}));

        assertEquals("ekm", exception.service());
        assertEquals("encrypt", exception.action());
        assertEquals(403, exception.statusCode());
        assertTrue(exception.responseBody().contains("REVOKED"));
    }

    private EuclidEkm newClient() {
        return new EuclidEkm(baseUrl(), "test-token", "eu-central-1", "863459426936", "alice", null, null, null, null);
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
