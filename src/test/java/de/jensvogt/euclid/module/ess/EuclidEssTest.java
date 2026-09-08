package de.jensvogt.euclid.module.ess;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.jensvogt.euclid.auth.SigV4;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.dto.ess.CreateSecretRequest;
import de.jensvogt.euclid.dto.ess.DeleteSecretResponse;
import de.jensvogt.euclid.dto.ess.GetSecretResponse;
import de.jensvogt.euclid.dto.ess.ListSecretsResponse;
import de.jensvogt.euclid.dto.ess.UpdateSecretRequest;
import de.jensvogt.euclid.dto.ess.model.Secret;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms EuclidEss authenticates the way it claims to (SigV4-signed when an access key is
 * configured, bearer token otherwise, mirroring euclid-cli's HttpClient.cpp), routes every secret
 * action to the right request with a correctly-shaped body, parses the corresponding response, and
 * surfaces non-2xx responses as {@link EuclidServiceException}.
 * <p>
 * The update-secret cases carry most of the weight here: ESS reads a field's presence rather than
 * its value, so a client that serialized its nulls would silently clear a description somebody
 * meant to leave alone.
 */
class EuclidEssTest {

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
    void createSecretSignsWithSigV4WhenAccessKeyConfigured() throws Exception {
        String accessKeyId = "AKIDEXAMPLE";
        String secretAccessKey = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";

        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"secret\":" + secretJson(1) + "}");
        });

        new EuclidEss(baseUrl(), "unused-token", "eu-central-1", "863459426936", "alice",
                accessKeyId, secretAccessKey, null, null)
                .createSecret("db-password", "hunter2");

        SignableRequest req = received.get();
        assertTrue(req.header("authorization").startsWith("AWS4-HMAC-SHA256 "));

        Optional<SigV4.VerifyResult> result = SigV4.verify(req,
                id -> id.equals(accessKeyId) ? Optional.of(secretAccessKey) : Optional.empty());
        assertTrue(result.isPresent(), "server-side verification of the client's own signature must succeed");
        assertEquals(accessKeyId, result.get().accessKeyId());
        assertEquals("ess", req.header("x-euclid-target"));
    }

    @Test
    void createSecretUsesTheBearerTokenWithoutAnAccessKey() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"secret\":" + secretJson(1) + "}");
        });

        newClient().createSecret("db-password", "hunter2");

        assertEquals("Bearer test-token", received.get().header("authorization"));
    }

    @Test
    void createSecretSendsNameAndValueAndParsesTheMetadata() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"secret\":" + secretJson(1) + "}");
        });

        Secret secret = newClient().createSecret("db-password", "hunter2");

        assertEquals("create-secret", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"name\":\"db-password\"", "\"value\":\"hunter2\"");
        assertEquals("db-password", secret.name());
        assertEquals("ern:ekm:key/ns-key", secret.encryptionKeyErn());
        assertEquals(1, secret.version());
        assertEquals(Map.of("env", "prod"), secret.tags());
    }

    @Test
    void createSecretCanNameTheKeyToEncryptUnder() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"secret\":" + secretJson(1) + "}");
        });

        newClient().createSecret(CreateSecretRequest.builder().name("db-password").value("hunter2")
                .description("the reporting replica").keyErn("ern:ekm:key/own").build());

        assertBodyContains(received.get().body(), "\"keyErn\":\"ern:ekm:key/own\"",
                "\"description\":\"the reporting replica\"");
    }

    // create-secret refuses to overwrite: that is what update-secret is for.
    @Test
    void createSecretSurfacesAConflictForANameThatExists() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 409, "{\"error\":\"Secret exists already, name: db-password\"}");
        });

        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> newClient().createSecret("db-password", "hunter2"));

        assertEquals("ess", exception.service());
        assertEquals("create-secret", exception.action());
        assertEquals(409, exception.statusCode());
    }

    @Test
    void getSecretReturnsTheMetadataAndThePlaintext() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"secret\":" + secretJson(3) + ",\"value\":\"hunter2\"}");
        });

        GetSecretResponse response = newClient().getSecret("db-password");

        assertEquals("get-secret", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"name\":\"db-password\"");
        assertEquals("hunter2", response.value());
        assertEquals(3, response.secret().version());
    }

    @Test
    void listSecretsParsesThePageAndTheTotal() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"secrets\":[" + secretJson(1) + "],\"total\":7}");
        });

        ListSecretsResponse response = newClient().listSecrets("db-", 5, 1, "name", "desc");

        assertEquals("list-secrets", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"prefix\":\"db-\"", "\"pageSize\":5", "\"pageIndex\":1",
                "\"sortDirection\":\"desc\"");
        assertEquals(1, response.secrets().size());
        assertEquals("db-password", response.secrets().getFirst().name());
    }

    // A restricted principal gets the secrets it may read, filtered rather than refused, so the
    // total can legitimately exceed what came back. The client must not try to reconcile the two.
    @Test
    void listSecretsKeepsATotalLargerThanThePage() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, "{\"secrets\":[" + secretJson(1) + "],\"total\":42}");
        });

        ListSecretsResponse response = newClient().listSecrets();

        assertEquals(1, response.secrets().size());
        assertEquals(42, response.total());
    }

    @Test
    void listSecretsToleratesAnEmptyResult() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, "{\"total\":0}");
        });

        assertTrue(newClient().listSecrets().secrets().isEmpty());
    }

    @Test
    void rotateSecretSendsOnlyTheValue() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"secret\":" + secretJson(2) + "}");
        });

        Secret secret = newClient().rotateSecret("db-password", "hunter3");

        assertEquals("update-secret", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"name\":\"db-password\"", "\"value\":\"hunter3\"");
        assertEquals(2, secret.version());
    }

    // The heart of it: ESS asks whether a field is present, not what it holds. An omitted
    // description has to stay omitted on the wire, or the server reads it as "clear this".
    @Test
    void updateSecretOmitsTheFieldsItWasNotGiven() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"secret\":" + secretJson(2) + "}");
        });

        newClient().updateSecret(UpdateSecretRequest.builder().name("db-password").value("hunter3").build());

        String body = received.get().body();
        assertBodyContains(body, "\"value\":\"hunter3\"");
        assertFalse(body.contains("description"), "an unset description must not reach the server at all: " + body);
        assertFalse(body.contains("keyErn"), "an unset keyErn must not reach the server at all: " + body);
    }

    // The other half of the same rule: an explicitly empty description is a request to clear it,
    // and has to survive serialization as an empty string rather than being dropped.
    @Test
    void updateSecretSendsAnEmptyDescriptionWhenAskedToClearIt() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"secret\":" + secretJson(1) + "}");
        });

        newClient().updateSecret(UpdateSecretRequest.builder().name("db-password").description("").build());

        assertBodyContains(received.get().body(), "\"description\":\"\"");
    }

    @Test
    void updateSecretCanMoveTheSecretToAnotherKey() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"secret\":" + secretJson(1) + "}");
        });

        newClient().updateSecret(UpdateSecretRequest.builder().name("db-password")
                .keyErn("ern:ekm:key/new").build());

        String body = received.get().body();
        assertBodyContains(body, "\"keyErn\":\"ern:ekm:key/new\"");
        assertFalse(body.contains("\"value\""), "moving a secret must not resend its value: " + body);
    }

    @Test
    void updateSecretSurfacesARequestThatChangesNothing() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 400, "{\"error\":\"Name a value, a description or a key to change\"}");
        });

        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> newClient().updateSecret(UpdateSecretRequest.builder().name("db-password").build()));

        assertEquals("update-secret", exception.action());
        assertEquals(400, exception.statusCode());
    }

    @Test
    void deleteSecretReturnsTheErnAndName() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"ern\":\"ern:ess:secret/db-password\",\"name\":\"db-password\"}");
        });

        DeleteSecretResponse response = newClient().deleteSecret("db-password");

        assertEquals("delete-secret", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"name\":\"db-password\"");
        assertEquals("ern:ess:secret/db-password", response.ern());
        assertEquals("db-password", response.name());
    }

    @Test
    void addSecretTagSendsNameKeyAndValue() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"secret\":" + secretJson(1) + "}");
        });

        Secret secret = newClient().addSecretTag("db-password", "env", "prod");

        assertEquals("add-secret-tag", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"name\":\"db-password\"", "\"key\":\"env\"",
                "\"value\":\"prod\"");
        assertEquals(Map.of("env", "prod"), secret.tags());
    }

    @Test
    void deleteSecretTagSendsNameAndKey() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"secret\":" + secretJson(1) + "}");
        });

        newClient().deleteSecretTag("db-password", "env");

        assertEquals("delete-secret-tag", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"name\":\"db-password\"", "\"key\":\"env\"");
    }

    @Test
    void namespaceIsSentWhenTheSessionIsScoped() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"total\":0}");
        });

        new EuclidEss(baseUrl(), "test-token", "eu-central-1", "863459426936", "alice", null, null, null, "prod")
                .listSecrets();

        assertEquals("prod", received.get().header("x-euclid-namespace"));
    }

    @Test
    void namespaceHeaderIsOmittedWhenUnset() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"total\":0}");
        });

        newClient().listSecrets();

        assertEquals("", received.get().header("x-euclid-namespace"));
    }

    @Test
    void aSecretWithoutOptionalFieldsIsStillReadable() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, "{\"secret\":{\"name\":\"db-password\"},\"value\":\"hunter2\"}");
        });

        GetSecretResponse response = newClient().getSecret("db-password");

        assertEquals("db-password", response.secret().name());
        assertNull(response.secret().description());
        assertEquals(0, response.secret().version());
        assertTrue(response.secret().tags().isEmpty());
    }

    private static String secretJson(int version) {
        return "{\"name\":\"db-password\",\"ern\":\"ern:ess:secret/db-password\","
                + "\"description\":\"the reporting replica\",\"encryptionKeyErn\":\"ern:ekm:key/ns-key\","
                + "\"version\":" + version + ",\"rotated\":\"2026-01-02T00:00:00Z\","
                + "\"tags\":{\"env\":\"prod\"},\"created\":\"2026-01-01\",\"modified\":\"2026-01-02\"}";
    }

    private EuclidEss newClient() {
        return new EuclidEss(baseUrl(), "test-token", "eu-central-1", "863459426936", "alice", null, null, null, null);
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
