package de.jensvogt.euclid.module.eap;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.jensvogt.euclid.auth.SigV4;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.dto.eap.CreateApplicationRequest;
import de.jensvogt.euclid.dto.eap.RedeployApplicationRequest;
import de.jensvogt.euclid.dto.eap.UpdateApplicationRequest;
import de.jensvogt.euclid.dto.eap.model.Application;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms EuclidEap authenticates the way it claims to (SigV4-signed when an access key is
 * configured, bearer token otherwise, mirroring euclid-cli's HttpClient.cpp), routes every
 * application action to the right request with a correctly-shaped body, parses the corresponding
 * response, and surfaces non-2xx responses as {@link EuclidServiceException}.
 */
class EuclidEapTest {

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
    void createApplicationSignsWithSigV4WhenAccessKeyConfigured() throws Exception {
        String accessKeyId = "AKIDEXAMPLE";
        String secretAccessKey = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";

        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, applicationJson());
        });

        EuclidEap eap = new EuclidEap(baseUrl(), "unused-token", "eu-central-1", "863459426936", "alice",
                accessKeyId, secretAccessKey, null, null);
        eap.createApplication(minimalRequest());

        SignableRequest req = received.get();
        assertTrue(req.header("authorization").startsWith("AWS4-HMAC-SHA256 "));

        Optional<SigV4.VerifyResult> result = SigV4.verify(req,
                id -> id.equals(accessKeyId) ? Optional.of(secretAccessKey) : Optional.empty());
        assertTrue(result.isPresent(), "server-side verification of the client's own signature must succeed");
        assertEquals(accessKeyId, result.get().accessKeyId());
    }

    @Test
    void createApplicationUsesBearerTokenWhenNoAccessKeyConfigured() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, applicationJson());
        });

        newClient().createApplication(minimalRequest());

        assertEquals("Bearer test-token", received.get().header("authorization"));
    }

    @Test
    void createApplicationSendsTheDefinitionAndParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, applicationJson());
        });

        Application created = newClient().createApplication(CreateApplicationRequest.builder()
                .applicationId("billing").runtime("JAVA").bucket("artifacts").artifact("billing-1.0.jar")
                .command("java").arguments(List.of("-jar", "billing-1.0.jar"))
                .environment(Map.of("LOG_LEVEL", "info"))
                .buckets(List.of("invoices")).queues(List.of("billing-events"))
                .minInstances(2L).maxInstances(5L).readyTimeoutMs(60000L).build());

        assertEquals("create-application", received.get().header("x-euclid-action"));
        assertEquals("eap", received.get().header("x-euclid-target"));
        assertBodyContains(received.get().body(), "\"applicationId\":\"billing\"", "\"runtime\":\"JAVA\"",
                "\"bucket\":\"artifacts\"", "\"artifact\":\"billing-1.0.jar\"", "\"command\":\"java\"",
                "\"arguments\":[\"-jar\",\"billing-1.0.jar\"]", "\"buckets\":[\"invoices\"]",
                "\"queues\":[\"billing-events\"]", "\"minInstances\":2", "\"maxInstances\":5",
                "\"readyTimeoutMs\":60000");

        // The names a caller deploys with come back resolved to ERNs.
        assertEquals("billing", created.applicationId());
        assertEquals("ern:esm:eu-central-1:863459426936:bucket/artifacts", created.bucketErn());
        assertEquals("billing-1.0.0.jar", created.artifactKey());
        // Which build is deployed, and which bytes it is - what a redeploy has to differ from.
        assertEquals("1.0.0", created.version());
        assertEquals("0dc7cdef5e707bae7f7b6bbb5be4c32a", created.md5Sum());
        assertEquals(List.of("-jar", "billing-1.0.jar"), created.arguments());
        assertEquals("info", created.environment().get("LOG_LEVEL"));
        assertEquals(List.of("ern:esm:eu-central-1:863459426936:bucket/invoices"), created.resources());
        // A new application is deployed stopped; start-application is what asks for it to run.
        assertEquals("STOPPED", created.desiredState());
        assertEquals(0, created.instances());
    }

    // The server applies its own defaults for the instance counts and the ready timeout when the
    // fields are absent, so an unset optional has to be left out rather than sent as null.
    @Test
    void createApplicationOmitsUnsetOptionalFields() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, applicationJson());
        });

        newClient().createApplication(minimalRequest());

        String body = received.get().body();
        assertFalse(body.contains("minInstances"), "an unset minInstances should not be sent, was " + body);
        assertFalse(body.contains("readyTimeoutMs"), "an unset readyTimeoutMs should not be sent, was " + body);
        assertFalse(body.contains("environment"), "an unset environment should not be sent, was " + body);
        // Left unset, EAP mints a technical principal rather than the application borrowing a user.
        assertFalse(body.contains("\"user\""), "an unset user should not be sent, was " + body);
    }

    // update-application only touches the fields it receives, so anything the caller did not set
    // has to stay out of the body - sending "arguments":null would be read as an empty list and
    // would silently clear the application's argument list.
    @Test
    void updateApplicationSendsOnlyTheFieldsThatWereSet() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, applicationJson());
        });

        newClient().updateApplication(UpdateApplicationRequest.builder()
                .applicationId("billing").maxInstances(8L).build());

        String body = received.get().body();
        assertBodyContains(body, "\"applicationId\":\"billing\"", "\"maxInstances\":8");
        assertFalse(body.contains("arguments"), "an untouched arguments must not be sent, was " + body);
        assertFalse(body.contains("environment"), "an untouched environment must not be sent, was " + body);
        assertFalse(body.contains("runtime"), "an untouched runtime must not be sent, was " + body);
    }

    @Test
    void updateApplicationReplacesTheResourceGrantsWhenTheyAreSet() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, applicationJson());
        });

        newClient().updateApplication(UpdateApplicationRequest.builder()
                .applicationId("billing").buckets(List.of("archive")).build());

        assertBodyContains(received.get().body(), "\"buckets\":[\"archive\"]");
    }

    @Test
    void listApplicationsUsesAnEmptyPrefixAndParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"applications\":[" + applicationJson() + "]}");
        });

        List<Application> applications = newClient().listApplications();

        assertEquals("list-applications", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"prefix\":\"\"");
        assertEquals(1, applications.size());
        assertEquals("billing", applications.getFirst().applicationId());
    }

    @Test
    void listApplicationsWithPrefix() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"applications\":[]}");
        });

        assertTrue(newClient().listApplications("bill").isEmpty());
        assertBodyContains(received.get().body(), "\"prefix\":\"bill\"");
    }

    @Test
    void getApplicationSendsTheApplicationId() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, applicationJson());
        });

        Application found = newClient().getApplication("billing");

        assertEquals("get-application", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"applicationId\":\"billing\"");
        assertEquals("ern:eap:eu-central-1:863459426936:application/billing", found.ern());
        assertEquals("JAVA", found.runtime());
    }

    @Test
    void deleteApplicationSendsTheApplicationId() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"applicationId\":\"billing\",\"deleted\":true}");
        });

        newClient().deleteApplication("billing");

        assertEquals("delete-application", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"applicationId\":\"billing\"");
    }

    // start and stop only record intent - the reconciler is what acts on it, so the observed state
    // and instance count can still lag the desired state in the response.
    @Test
    void startAndStopApplicationRecordDesiredStateOnly() throws Exception {
        Map<String, String> bodyByAction = new ConcurrentHashMap<>();
        server = startServer(exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            bodyByAction.put(action, new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendResponse(exchange, 200,
                    applicationJson("start-application".equals(action) ? "RUNNING" : "STOPPED", "STOPPED", 0));
        });

        EuclidEap eap = newClient();
        Application started = eap.startApplication("billing");
        Application stopped = eap.stopApplication("billing");

        assertBodyContains(bodyByAction.get("start-application"), "\"applicationId\":\"billing\"");
        assertBodyContains(bodyByAction.get("stop-application"), "\"applicationId\":\"billing\"");
        assertEquals("RUNNING", started.desiredState());
        assertEquals("STOPPED", started.state(), "the reconciler has not caught up yet");
        assertEquals(0, started.instances());
        assertEquals("STOPPED", stopped.desiredState());
    }

    @Test
    void namespaceIsSentWhenTheSessionIsScoped() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"applications\":[]}");
        });

        new EuclidEap(baseUrl(), "test-token", "eu-central-1", "863459426936", "alice", null, null, null, "prod")
                .listApplications();

        assertEquals("prod", received.get().header("x-euclid-namespace"));
    }

    // Every EAP action decides which code euclid runs, so all of them are administrator-only.
    @Test
    void forbiddenResponseThrowsEuclidServiceException() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 403, "{\"error\":\"Administrator privileges required\"}");
        });

        EuclidEap eap = newClient();
        EuclidServiceException exception = assertThrows(EuclidServiceException.class, eap::listApplications);

        assertEquals("eap", exception.service());
        assertEquals("list-applications", exception.action());
        assertEquals(403, exception.statusCode());
        assertTrue(exception.responseBody().contains("Administrator privileges required"));
    }

    // Names are resolved at deployment rather than at start-up, so a missing artifact is a rejected
    // deployment instead of a pool that never comes up.
    @Test
    void missingArtifactSurfacesTheServersMessage() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 404, "{\"error\":\"Artifact not found in bucket 'artifacts': billing-1.0.jar\"}");
        });

        EuclidEap eap = newClient();
        EuclidServiceException exception =
                assertThrows(EuclidServiceException.class, () -> eap.createApplication(minimalRequest()));

        assertEquals(404, exception.statusCode());
        assertTrue(exception.responseBody().contains("Artifact not found"));
    }

    // EAP will not create an application whose version it cannot establish, so a caller deploying
    // an artifact whose name carries none has to be able to say what it is.
    @Test
    void createApplicationSendsTheVersionWhenGiven() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, applicationJson());
        });

        newClient().createApplication(CreateApplicationRequest.builder()
                .applicationId("billing").runtime("JAVA").bucket("artifacts").artifact("billing.jar")
                .version("1.0.0").build());

        assertBodyContains(received.get().body(), "\"version\":\"1.0.0\"");
    }

    // And left unset it must stay out of the body: EAP then reads the version off the artifact's
    // own name, which sending an empty string would prevent.
    @Test
    void createApplicationOmitsAnUnsetVersion() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, applicationJson());
        });

        newClient().createApplication(minimalRequest());

        String body = received.get().body();
        assertFalse(body.contains("version"), "an unset version should not be sent, was " + body);
    }

    @Test
    void redeployApplicationSendsTheBuildAndParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, applicationJson());
        });

        Application deployed = newClient().redeployApplication(RedeployApplicationRequest.builder()
                .applicationId("billing").artifact("billing-1.0.0.jar").version("1.0.0").build());

        assertEquals("redeploy-application", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"applicationId\":\"billing\"",
                "\"artifact\":\"billing-1.0.0.jar\"", "\"version\":\"1.0.0\"");
        assertEquals("1.0.0", deployed.version());
    }

    // The ordinary redeploy names neither: the build keeps its key, and its version is read off
    // the artifact's name. Both have to be absent rather than null for the server to do that.
    @Test
    void redeployApplicationOmitsWhatItWasNotTold() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, applicationJson());
        });

        newClient().redeployApplication(RedeployApplicationRequest.builder().applicationId("billing").build());

        String body = received.get().body();
        assertFalse(body.contains("artifact"), "an unset artifact should not be sent, was " + body);
        assertFalse(body.contains("version"), "an unset version should not be sent, was " + body);
    }

    // A redeploy that is not a new build is refused by the server, and the reason is the whole
    // value of the refusal - it has to reach the caller rather than becoming a bare 409.
    @Test
    void aRefusedRedeploySurfacesTheServersReason() throws Exception {
        server = startServer(exchange -> sendResponse(exchange, 409,
                "{\"error\":\"Refusing to redeploy 'billing': version 1.0.0 is already deployed"
                        + " - a new build needs a new version. Use 'update-application' to deploy it anyway.\"}"));

        EuclidEap eap = newClient();
        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> eap.redeployApplication(RedeployApplicationRequest.builder()
                        .applicationId("billing").version("1.0.0").build()));

        assertEquals(409, exception.statusCode());
        assertTrue(exception.responseBody().contains("already deployed"));
    }

    private static CreateApplicationRequest minimalRequest() {
        return CreateApplicationRequest.builder()
                .applicationId("billing").runtime("JAVA").bucket("artifacts").artifact("billing-1.0.jar").build();
    }

    private static String applicationJson() {
        return applicationJson("STOPPED", "STOPPED", 0);
    }

    private static String applicationJson(String desiredState, String state, int instances) {
        return "{\"applicationId\":\"billing\",\"ern\":\"ern:eap:eu-central-1:863459426936:application/billing\","
                + "\"accountId\":\"863459426936\",\"region\":\"eu-central-1\",\"runtime\":\"JAVA\","
                + "\"bucketErn\":\"ern:esm:eu-central-1:863459426936:bucket/artifacts\","
                + "\"artifactKey\":\"billing-1.0.0.jar\",\"version\":\"1.0.0\","
                + "\"md5Sum\":\"0dc7cdef5e707bae7f7b6bbb5be4c32a\",\"command\":\"java\","
                + "\"arguments\":[\"-jar\",\"billing-1.0.jar\"],\"environment\":{\"LOG_LEVEL\":\"info\"},"
                + "\"resources\":[\"ern:esm:eu-central-1:863459426936:bucket/invoices\"],"
                + "\"userId\":\"eap-billing\",\"minInstances\":2,\"maxInstances\":5,\"readyTimeoutMs\":60000,"
                + "\"desiredState\":\"" + desiredState + "\",\"state\":\"" + state + "\",\"instances\":" + instances
                + ",\"created\":\"2026-01-01\",\"modified\":\"2026-01-02\"}";
    }

    private EuclidEap newClient() {
        return new EuclidEap(baseUrl(), "test-token", "eu-central-1", "863459426936", "alice", null, null, null, null);
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
