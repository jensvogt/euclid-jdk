package de.jensvogt.euclid.module.ets;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.jensvogt.euclid.auth.Rfc9421;
import de.jensvogt.euclid.auth.SigV4;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.auth.SigningScheme;
import de.jensvogt.euclid.dto.ets.CreateServerRequest;
import de.jensvogt.euclid.dto.ets.UpdateServerRequest;
import de.jensvogt.euclid.dto.ets.model.TransferServer;
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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms EuclidEts authenticates the way it claims to (signed when an access key is configured -
 * with SigV4 as euclid-cli's HttpClient.cpp does, or with RFC 9421 when that scheme is selected -
 * and with a bearer token otherwise), routes every transfer server action to the right request with
 * a correctly-shaped body, parses the corresponding response, and surfaces non-2xx responses as
 * {@link EuclidServiceException}.
 */
class EuclidEtsTest {

    private static final List<String> SIGNED_HEADERS = List.of("host", "x-amz-content-sha256", "x-amz-date",
            "x-euclid-account-id", "x-euclid-action", "x-euclid-region", "x-euclid-target", "x-euclid-user-id",
            "x-euclid-namespace");

    /**
     * The headers RFC 9421 signs into, none of which SigV4 uses - captured alongside
     * {@link #SIGNED_HEADERS} so one helper can hand either scheme's verifier what it needs.
     */
    private static final List<String> RFC9421_HEADERS = List.of("content-digest", "signature", "signature-input");

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void createServerSignsWithSigV4WhenAccessKeyConfigured() throws Exception {
        String accessKeyId = "AKIDEXAMPLE";
        String secretAccessKey = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";

        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, serverJson());
        });

        EuclidEts ets = new EuclidEts(baseUrl(), "unused-token", "eu-central-1", "863459426936", "alice",
                accessKeyId, secretAccessKey, null, null);
        ets.createServer(minimalRequest());

        SignableRequest req = received.get();
        assertTrue(req.header("authorization").startsWith("AWS4-HMAC-SHA256 "));

        Optional<SigV4.VerifyResult> result = SigV4.verify(req,
                id -> id.equals(accessKeyId) ? Optional.of(secretAccessKey) : Optional.empty());
        assertTrue(result.isPresent(), "server-side verification of the client's own signature must succeed");
        assertEquals(accessKeyId, result.get().accessKeyId());
    }

    @Test
    void createServerSignsWithRfc9421WhenThatSchemeIsSelected() throws Exception {
        String accessKeyId = "AKIDEXAMPLE";
        String secretAccessKey = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";

        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, serverJson());
        });

        EuclidEts ets = new EuclidEts(baseUrl(), "unused-token", "eu-central-1", "863459426936", "alice",
                accessKeyId, secretAccessKey, null, null);
        ets.signingScheme(SigningScheme.RFC9421);
        ets.createServer(minimalRequest());

        SignableRequest req = received.get();
        assertEquals("", req.header("authorization"), "RFC 9421 signs into its own headers, leaving Authorization free");
        assertEquals("", req.header("x-amz-date"), "no SigV4 leftovers once the scheme is switched");
        assertEquals(Optional.of(SigningScheme.RFC9421), SigningScheme.of(req));

        Optional<Rfc9421.VerifyResult> result = Rfc9421.verify(req,
                id -> id.equals(accessKeyId) ? Optional.of(secretAccessKey) : Optional.empty());
        assertTrue(result.isPresent(), "server-side verification of the client's own signature must succeed");
        assertEquals(accessKeyId, result.get().keyId());
    }

    @Test
    void createServerUsesBearerTokenWhenNoAccessKeyConfigured() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, serverJson());
        });

        newClient().createServer(minimalRequest());

        assertEquals("Bearer test-token", received.get().header("authorization"));
    }

    @Test
    void createServerSendsDefaultsAndParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, serverJson());
        });

        TransferServer created = newClient().createServer(CreateServerRequest.builder()
                .serverId("sftp-1").port(2222L).bucket("transfer")
                .userIds(List.of("alice", "bob")).userGroups(List.of("admins")).build());

        assertEquals("create-server", received.get().header("x-euclid-action"));
        assertEquals("ets", received.get().header("x-euclid-target"));
        assertBodyContains(received.get().body(), "\"serverId\":\"sftp-1\"", "\"protocol\":\"SFTP\"",
                "\"port\":2222", "\"bucket\":\"transfer\"", "\"address\":\"0.0.0.0\"",
                "\"userIds\":[\"alice\",\"bob\"]", "\"userGroups\":[\"admins\"]");

        assertEquals("sftp-1", created.serverId());
        assertEquals("transfer", created.bucketName());
        assertEquals(2222, created.port());
        assertEquals(List.of("alice", "bob"), created.userIds());
        // A new server is defined stopped; start-server is what asks for it to run.
        assertEquals("STOPPED", created.desiredState());
        assertEquals("STOPPED", created.state());
    }

    // The server applies its own defaults for hostKey/pasvMin/pasvMax when the fields are absent,
    // so an unset optional has to be left out of the body rather than sent as null.
    @Test
    void createServerOmitsUnsetOptionalFields() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, serverJson());
        });

        newClient().createServer(minimalRequest());

        String body = received.get().body();
        assertFalse(body.contains("hostKey"), "an unset hostKey should not be sent at all, was " + body);
        assertFalse(body.contains("pasvMin"), "an unset pasvMin should not be sent at all, was " + body);
        assertFalse(body.contains("userGroups"), "an unset userGroups should not be sent at all, was " + body);
    }

    @Test
    void createServerWithExplicitProtocolAndFtpPortRange() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, serverJson());
        });

        newClient().createServer(CreateServerRequest.builder()
                .serverId("ftp-1").protocol("FTP").port(2121L).bucket("transfer").address("127.0.0.1")
                .userGroups(List.of("admins")).pasvMin(7000L).pasvMax(7100L).build());

        assertBodyContains(received.get().body(), "\"protocol\":\"FTP\"", "\"address\":\"127.0.0.1\"",
                "\"pasvMin\":7000", "\"pasvMax\":7100");
    }

    // update-server only touches the fields it receives, so anything the caller did not set has to
    // stay out of the body - sending "userIds":null would be read as an empty list and would
    // silently clear the server's access list.
    @Test
    void updateServerSendsOnlyTheFieldsThatWereSet() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, serverJson());
        });

        newClient().updateServer(UpdateServerRequest.builder().serverId("sftp-1").port(2223L).build());

        String body = received.get().body();
        assertBodyContains(body, "\"serverId\":\"sftp-1\"", "\"port\":2223");
        assertFalse(body.contains("userIds"), "an untouched userIds must not be sent, was " + body);
        assertFalse(body.contains("bucket"), "an untouched bucket must not be sent, was " + body);
        assertFalse(body.contains("address"), "an untouched address must not be sent, was " + body);
    }

    @Test
    void updateServerReplacesTheAccessListWhenItIsSet() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, serverJson());
        });

        newClient().updateServer(UpdateServerRequest.builder().serverId("sftp-1")
                .userIds(List.of("carol")).build());

        assertBodyContains(received.get().body(), "\"userIds\":[\"carol\"]");
    }

    @Test
    void listServersUsesAnEmptyPrefixAndParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"servers\":[" + serverJson() + "]}");
        });

        List<TransferServer> servers = newClient().listServers();

        assertEquals("list-servers", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"prefix\":\"\"");
        assertEquals(1, servers.size());
        assertEquals("sftp-1", servers.getFirst().serverId());
    }

    @Test
    void listServersWithPrefix() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"servers\":[]}");
        });

        assertTrue(newClient().listServers("sftp").isEmpty());
        assertBodyContains(received.get().body(), "\"prefix\":\"sftp\"");
    }

    @Test
    void getServerSendsTheServerId() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, serverJson());
        });

        TransferServer found = newClient().getServer("sftp-1");

        assertEquals("get-server", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"serverId\":\"sftp-1\"");
        assertEquals("ern:ets:eu-central-1:863459426936:server/sftp-1", found.ern());
        assertEquals("SFTP", found.protocol());
    }

    @Test
    void deleteServerSendsTheServerId() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"serverId\":\"sftp-1\",\"deleted\":true}");
        });

        newClient().deleteServer("sftp-1");

        assertEquals("delete-server", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"serverId\":\"sftp-1\"");
    }

    // start-server and stop-server only record intent - the reconciler is what acts on it, so the
    // observed state can still lag the desired one in the response.
    @Test
    void startAndStopServerRecordDesiredStateOnly() throws Exception {
        Map<String, String> bodyByAction = new ConcurrentHashMap<>();
        server = startServer(exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            bodyByAction.put(action, new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendResponse(exchange, 200, serverJson("start-server".equals(action) ? "RUNNING" : "STOPPED", "STOPPED"));
        });

        EuclidEts ets = newClient();
        TransferServer started = ets.startServer("sftp-1");
        TransferServer stopped = ets.stopServer("sftp-1");

        assertBodyContains(bodyByAction.get("start-server"), "\"serverId\":\"sftp-1\"");
        assertBodyContains(bodyByAction.get("stop-server"), "\"serverId\":\"sftp-1\"");
        assertEquals("RUNNING", started.desiredState());
        assertEquals("STOPPED", started.state(), "the reconciler has not caught up yet");
        assertEquals("STOPPED", stopped.desiredState());
    }

    @Test
    void namespaceIsSentWhenTheSessionIsScoped() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"servers\":[]}");
        });

        new EuclidEts(baseUrl(), "test-token", "eu-central-1", "863459426936", "alice", null, null, null, "prod")
                .listServers();

        assertEquals("prod", received.get().header("x-euclid-namespace"));
    }

    // Every ETS action is administrator-only, so a non-admin session is turned away by the server.
    @Test
    void forbiddenResponseThrowsEuclidServiceException() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 403, "{\"error\":\"Administrator privileges required\"}");
        });

        EuclidEts ets = newClient();
        EuclidServiceException exception = assertThrows(EuclidServiceException.class, ets::listServers);

        assertEquals("ets", exception.service());
        assertEquals("list-servers", exception.action());
        assertEquals(403, exception.statusCode());
        assertTrue(exception.responseBody().contains("Administrator privileges required"));
    }

    @Test
    void portConflictSurfacesTheServersMessage() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 409, "{\"error\":\"port 2222 is already used by transfer server 'sftp-1'\"}");
        });

        EuclidEts ets = newClient();
        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> ets.createServer(CreateServerRequest.builder().serverId("sftp-2").port(2222L)
                        .bucket("transfer").userIds(List.of("alice")).build()));

        assertEquals(409, exception.statusCode());
        assertTrue(exception.responseBody().contains("already used by transfer server"));
    }

    // The server would accept a definition naming neither users nor groups, but nobody could log
    // in to the resulting listener, so it is refused before it is sent - as euclid-cli does.
    @Test
    void createServerRefusesADefinitionNobodyCouldLogInTo() throws Exception {
        AtomicReference<String> receivedAction = new AtomicReference<>();
        server = startServer(exchange -> {
            receivedAction.set(exchange.getRequestHeaders().getFirst("x-euclid-action"));
            sendResponse(exchange, 200, serverJson());
        });

        EuclidEts ets = newClient();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ets.createServer(CreateServerRequest.builder().serverId("sftp-1").port(2222L)
                        .bucket("transfer").build()));

        assertTrue(exception.getMessage().contains("nobody can log in"));
        assertNull(receivedAction.get(), "the request must not reach the server at all");
    }

    @Test
    void createServerAcceptsGroupsAloneAsAnAccessList() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, serverJson());
        });

        newClient().createServer(CreateServerRequest.builder().serverId("sftp-1").port(2222L)
                .bucket("transfer").userGroups(List.of("admins")).build());

        assertBodyContains(received.get().body(), "\"userGroups\":[\"admins\"]");
    }

    private static CreateServerRequest minimalRequest() {
        return CreateServerRequest.builder().serverId("sftp-1").port(2222L).bucket("transfer")
                .userIds(List.of("alice")).build();
    }

    private static String serverJson() {
        return serverJson("STOPPED", "STOPPED");
    }

    private static String serverJson(String desiredState, String state) {
        return "{\"serverId\":\"sftp-1\",\"ern\":\"ern:ets:eu-central-1:863459426936:server/sftp-1\","
                + "\"accountId\":\"863459426936\",\"region\":\"eu-central-1\",\"protocol\":\"SFTP\","
                + "\"address\":\"0.0.0.0\",\"port\":2222,\"bucketName\":\"transfer\","
                + "\"bucketErn\":\"ern:esm:eu-central-1:863459426936:development:bucket:transfer\","
                + "\"userIds\":[\"alice\",\"bob\"],\"userGroups\":[\"admins\"],"
                + "\"desiredState\":\"" + desiredState + "\",\"state\":\"" + state + "\",\"hostKey\":\"\","
                + "\"pasvMin\":6000,\"pasvMax\":6100,\"created\":\"2026-01-01\",\"modified\":\"2026-01-02\"}";
    }

    private EuclidEts newClient() {
        return new EuclidEts(baseUrl(), "test-token", "eu-central-1", "863459426936", "alice", null, null, null, null);
    }

    private static void assertBodyContains(String body, String... fragments) {
        for (String fragment : fragments) {
            assertTrue(body.contains(fragment), "expected body to contain " + fragment + " but was " + body);
        }
    }

    private static SignableRequest captureRequest(HttpExchange exchange) throws IOException {
        SignableRequest req = new SignableRequest(exchange.getRequestMethod(), exchange.getRequestURI().toString());
        // The test server is plain HTTP, and RFC 9421's @authority depends on knowing that.
        req.scheme("http");
        Headers requestHeaders = exchange.getRequestHeaders();
        for (String name : Stream.concat(SIGNED_HEADERS.stream(), RFC9421_HEADERS.stream()).toList()) {
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
