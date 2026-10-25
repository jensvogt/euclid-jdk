package de.jensvogt.euclid.module.eap;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.jensvogt.euclid.auth.SigV4;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.dto.eap.ApplyInfrastructureResponse;
import de.jensvogt.euclid.dto.eap.CopyApplicationRequest;
import de.jensvogt.euclid.dto.eap.CreateApplicationRequest;
import de.jensvogt.euclid.dto.eap.EgressLimitResponse;
import de.jensvogt.euclid.dto.eap.ListEgressSitesResponse;
import de.jensvogt.euclid.dto.eap.RebalanceApplicationResponse;
import de.jensvogt.euclid.dto.eap.ScaleApplicationRequest;
import de.jensvogt.euclid.dto.eap.RedeployApplicationRequest;
import de.jensvogt.euclid.dto.eap.RestartApplicationResponse;
import de.jensvogt.euclid.dto.eap.SetLogLevelResponse;
import de.jensvogt.euclid.dto.eap.UpdateApplicationRequest;
import de.jensvogt.euclid.dto.eap.ListNodesResponse;
import de.jensvogt.euclid.dto.eap.model.ApplicationType;
import de.jensvogt.euclid.dto.eap.model.EgressSite;
import de.jensvogt.euclid.dto.eap.model.Node;
import de.jensvogt.euclid.dto.eap.model.Platform;
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
import static org.junit.jupiter.api.Assertions.assertNull;
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

    // A copy names where it is going. targetApplicationId stays out of the body unless it is being
    // changed: the server reads an absent one as "the original's name", so sending null would have
    // to be read as a request for an application with no name.
    @Test
    void copyApplicationNamesTheTargetNamespace() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, applicationJson());
        });

        EuclidEap client = newClient();
        client.copyApplication(CopyApplicationRequest.builder()
                .applicationId("billing").targetNamespace("production").build());

        String body = received.get().body();
        assertBodyContains(body, "\"applicationId\":\"billing\"", "\"targetNamespace\":\"production\"");
        assertFalse(body.contains("targetApplicationId"), "an unset targetApplicationId must not be sent, was " + body);

        client.copyApplication(CopyApplicationRequest.builder()
                .applicationId("billing").targetNamespace("development")
                .targetApplicationId("billing-next").build());

        assertBodyContains(received.get().body(), "\"targetApplicationId\":\"billing-next\"");
    }

    // A bound the caller did not set must not be sent: the server reads an absent one as "leave it
    // as it stands", which is what lets a ceiling be raised without disturbing the floor under it.
    @Test
    void scaleApplicationSendsOnlyTheBoundItWasGiven() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, applicationJson());
        });

        EuclidEap client = newClient();
        client.scaleApplication(ScaleApplicationRequest.builder()
                .applicationId("billing").maxInstances(16L).build());

        String body = received.get().body();
        assertBodyContains(body, "\"applicationId\":\"billing\"", "\"maxInstances\":16");
        assertFalse(body.contains("minInstances"), "an unset minInstances must not be sent, was " + body);

        // The builder's instances() shorthand sets both, which pins the pool at that size.
        client.scaleApplication(ScaleApplicationRequest.builder()
                .applicationId("billing").instances(3).build());

        assertBodyContains(received.get().body(), "\"minInstances\":3", "\"maxInstances\":3");
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

    // An applicationId is only unique within an account and a namespace, so the definition carries
    // the namespace it belongs to - and the runtime name, which is what euclid actually calls the
    // process pool, the data directory, the socket and the log channel.
    @Test
    void anApplicationCarriesItsNamespaceAndRuntimeName() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, applicationJson());
        });

        Application found = newClient().getApplication("billing");

        assertEquals("development", found.namespace());
        assertEquals("billing-7f3a91", found.runtimeName());
    }

    // Where the pool actually is. Ports are handed out at spawn time and change as the pool scales,
    // so this is the only place an API gateway in front of the application can discover them.
    @Test
    void anApplicationCarriesItsRunningEndpointsAndLogLevel() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, applicationJson());
        });

        Application found = newClient().getApplication("billing");

        assertEquals("info", found.logLevel());
        assertEquals(2, found.endpoints().size());
        assertEquals("i-1", found.endpoints().getFirst().instanceId());
        assertEquals(4711, found.endpoints().getFirst().pid());
        assertEquals(34001, found.endpoints().getFirst().httpPort());
        assertEquals(34002, found.endpoints().get(1).httpPort());
    }

    // A stopped application reports none, which has to read back as an empty list rather than null.
    @Test
    void anApplicationWithNoRunningInstancesHasNoEndpoints() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, "{\"applicationId\":\"billing\",\"instances\":0}");
        });

        assertTrue(newClient().getApplication("billing").endpoints().isEmpty());
    }

    // A definition written before either field existed answers without them, and has to stay
    // readable rather than failing to parse.
    @Test
    void anApplicationWithoutNamespaceOrRuntimeNameIsStillReadable() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, "{\"applicationId\":\"billing\",\"runtime\":\"JAVA\"}");
        });

        Application found = newClient().getApplication("billing");

        assertEquals("billing", found.applicationId());
        assertNull(found.namespace());
        assertNull(found.runtimeName());
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
    void restartApplicationAsksForTheInstancesWithoutChangingTheDesiredState() throws Exception {
        Map<String, String> bodyByAction = new ConcurrentHashMap<>();
        server = startServer(exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            bodyByAction.put(action, new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendResponse(exchange, 200, "{\"applicationId\":\"billing\",\"restarting\":true,\"instances\":3}");
        });

        RestartApplicationResponse restarted = newClient().restartApplication("billing");

        assertBodyContains(bodyByAction.get("restart-application"), "\"applicationId\":\"billing\"");
        assertTrue(restarted.restarting());
        assertEquals("billing", restarted.applicationId());

        // What is running when the request is answered, not what came back from the restart: the
        // manager has not stopped anything yet at this point.
        assertEquals(3, restarted.instances());
    }

    @Test
    void restartingAStoppedApplicationIsRefusedRatherThanStartingIt() throws Exception {
        server = startServer(exchange -> sendResponse(exchange, 400,
                "{\"message\":\"Refusing to restart 'billing': billing is stopped - use \\\"eap start-application\\\" to bring it back\"}"));

        EuclidEap eap = newClient();
        EuclidServiceException thrown =
                assertThrows(EuclidServiceException.class, () -> eap.restartApplication("billing"));

        assertEquals(400, thrown.statusCode());
    }

    @Test
    void applyInfrastructureNamesWhatItCreatedAndWhatItRemoved() throws Exception {
        Map<String, String> bodyByAction = new ConcurrentHashMap<>();
        server = startServer(exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            bodyByAction.put(action, new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendResponse(exchange, 200, "{\"applicationId\":\"billing\",\"declared\":true,"
                    + "\"created\":[\"ern:eqs:eu-central-1:000000000000:development:queue:orders\"],"
                    + "\"deleted\":[\"ern:eqs:eu-central-1:000000000000:development:queue:retired\"],"
                    + "\"granted\":[\"access-queue-consume\"],\"revoked\":[\"access-queue-produce\"]}");
        });

        ApplyInfrastructureResponse applied = newClient().applyInfrastructure("billing");

        assertBodyContains(bodyByAction.get("apply-infrastructure"), "\"applicationId\":\"billing\"");
        assertTrue(applied.declared());
        assertEquals(List.of("ern:eqs:eu-central-1:000000000000:development:queue:orders"), applied.created());

        // The half worth reading before trusting a declaration: a reconcile is full, so a resource
        // this application created and the file no longer names is gone, and took its messages with
        // it. Named rather than counted, so a removal nobody intended is visible in the answer.
        assertEquals(List.of("ern:eqs:eu-central-1:000000000000:development:queue:retired"), applied.deleted());
        assertEquals(List.of("access-queue-consume"), applied.granted());
        assertEquals(List.of("access-queue-produce"), applied.revoked());
    }

    @Test
    void anApplicationWithNoDeclarationIsAnsweredRatherThanRefused() throws Exception {
        server = startServer(exchange -> sendResponse(exchange, 200,
                "{\"applicationId\":\"billing\",\"declared\":false}"));

        ApplyInfrastructureResponse applied = newClient().applyInfrastructure("billing");

        // Not an error: an application that provisions its resources by hand reads this way every
        // time, and the four lists come back empty rather than absent.
        assertFalse(applied.declared());
        assertTrue(applied.created().isEmpty());
        assertTrue(applied.deleted().isEmpty());
        assertTrue(applied.granted().isEmpty());
        assertTrue(applied.revoked().isEmpty());
    }

    @Test
    void aDeclarationClaimingAnotherApplicationsResourceIsRefused() throws Exception {
        server = startServer(exchange -> sendResponse(exchange, 400,
                "{\"message\":\"declaration belongs to \\\"protocolizing\\\", not to \\\"billing\\\"\"}"));

        EuclidEap eap = newClient();
        EuclidServiceException thrown =
                assertThrows(EuclidServiceException.class, () -> eap.applyInfrastructure("billing"));

        assertEquals(400, thrown.statusCode());
    }

    @Test
    void applyInfrastructureCanKeepWhatIsNoLongerDeclared() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"applicationId\":\"billing\",\"declared\":true,\"dryRun\":false,"
                    + "\"kept\":[{\"ern\":\"queue-ern\",\"reason\":\"not pruned\"}]}");
        });

        ApplyInfrastructureResponse applied = newClient().applyInfrastructure("billing", false);

        assertEquals("apply-infrastructure", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"applicationId\":\"billing\"", "\"prune\":false");
        assertEquals("not pruned", applied.kept().getFirst().reason());
    }

    /**
     * A plan is its own action, never a flag on apply-infrastructure: an EAP that predates plans
     * would ignore the flag and apply for real, removals included.
     */
    @Test
    void planInfrastructureHandsOverTheDeclarationAsAnObjectAndParsesThePlan() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"applicationId\":\"billing\",\"declared\":true,\"dryRun\":true,"
                    + "\"created\":[\"new-ern\"],\"adopted\":[\"old-ern\"],"
                    + "\"updated\":[{\"ern\":\"queue-ern\",\"changes\":[\"visibility 30 -> 60\"]}],"
                    + "\"deleted\":[],\"kept\":[{\"ern\":\"busy-ern\",\"reason\":\"queue has 3 messages\"}],"
                    + "\"granted\":[\"access-queue-consume on q\"],\"revoked\":[]}");
        });

        ApplyInfrastructureResponse plan = newClient().planInfrastructure("billing",
                "{\"applicationId\":\"billing\",\"owns\":{\"queues\":[{\"name\":\"orders\"}]}}", true);

        assertEquals("plan-infrastructure", received.get().header("x-euclid-action"));
        // Embedded, not a string holding JSON: EAP reads the field as the declaration itself.
        assertBodyContains(received.get().body(), "\"declaration\":{\"applicationId\":\"billing\"", "\"prune\":true");
        assertTrue(plan.dryRun());
        assertEquals(List.of("new-ern"), plan.created());
        assertEquals(List.of("old-ern"), plan.adopted());
        assertEquals("queue-ern", plan.updated().getFirst().ern());
        assertEquals(List.of("visibility 30 -> 60"), plan.updated().getFirst().changes());
        assertEquals("queue has 3 messages", plan.kept().getFirst().reason());
        assertEquals(List.of("access-queue-consume on q"), plan.granted());
    }

    @Test
    void planInfrastructureWithoutADeclarationPlansTheStoredOne() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"applicationId\":\"billing\",\"declared\":true,\"dryRun\":true}");
        });

        newClient().planInfrastructure("billing");

        assertEquals("plan-infrastructure", received.get().header("x-euclid-action"));
        assertFalse(received.get().body().contains("declaration"));
    }

    @Test
    void applyPlatformSendsTheNameAndDeclarationAndParsesThePlatform() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"platform\":\"ftp\",\"declared\":true,\"dryRun\":false,"
                    + "\"created\":[\"bucket-ern\"]}");
        });

        ApplyInfrastructureResponse applied = newClient().applyPlatform("ftp", "{\"owns\":{\"buckets\":[{\"name\":\"inbox\"}]}}", true);

        assertEquals("apply-platform", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"name\":\"ftp\"", "\"declaration\":{\"owns\"");
        assertEquals("ftp", applied.platform());
        assertNull(applied.applicationId());
        assertEquals(List.of("bucket-ern"), applied.created());
    }

    @Test
    void aPlatformWithoutANameIsCalledPlatform() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"platform\":\"platform\",\"declared\":true,\"dryRun\":true}");
        });

        newClient().planPlatform(null, "{}", true);

        assertEquals("plan-platform", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"name\":\"platform\"");
    }

    @Test
    void getPlatformParsesWhatItOwnsAndItsDeclaration() throws Exception {
        server = startServer(exchange -> sendResponse(exchange, 200, "{\"name\":\"ftp\",\"namespace\":\"development\","
                + "\"owns\":[\"bucket-ern\"],\"declaration\":{\"owns\":{}},\"modified\":\"2026-10-25T10:00:00Z\"}"));

        Platform platform = newClient().getPlatform("ftp");

        assertEquals("ftp", platform.name());
        assertEquals("development", platform.namespace());
        assertEquals(List.of("bucket-ern"), platform.owns());
        assertEquals("{\"owns\":{}}", platform.declaration());
    }

    @Test
    void listPlatformsParsesEveryPlatform() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"platforms\":[{\"name\":\"ftp\",\"owns\":[],\"declaration\":null},"
                    + "{\"name\":\"platform\",\"owns\":[\"topic-ern\"]}]}");
        });

        List<Platform> platforms = newClient().listPlatforms();

        assertEquals("list-platforms", received.get().header("x-euclid-action"));
        assertEquals(2, platforms.size());
        assertNull(platforms.getFirst().declaration());
        assertEquals(List.of("topic-ern"), platforms.get(1).owns());
    }

    @Test
    void rebalanceApplicationParsesTheAnswer() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"applicationId\":\"billing\",\"rebalancing\":true,\"instances\":3}");
        });

        RebalanceApplicationResponse response = newClient().rebalanceApplication("billing");

        assertEquals("rebalance-application", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"applicationId\":\"billing\"");
        assertTrue(response.rebalancing());
        assertEquals(3, response.instances());
    }

    @Test
    void listEgressSitesParsesSitesAndTheDefault() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"defaultLimits\":{\"requestsPerSecond\":5.0,\"burst\":10.0,\"concurrent\":4},"
                    + "\"sites\":[{\"applicationId\":\"billing\",\"site\":\"*\","
                    + "\"limits\":{\"requestsPerSecond\":2.0,\"burst\":2.0,\"concurrent\":1},\"limitsSet\":true},"
                    + "{\"applicationId\":\"billing\",\"site\":\"api.example.com\","
                    + "\"limits\":{\"requestsPerSecond\":2.0,\"burst\":2.0,\"concurrent\":1},\"limitsSet\":false,"
                    + "\"tokens\":1.5,\"inFlight\":1,\"blockedUntil\":\"\",\"lastUsed\":\"2026-10-25T10:00:00Z\"}]}");
        });

        ListEgressSitesResponse response = newClient().listEgressSites("billing");

        assertEquals("list-egress-sites", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"applicationId\":\"billing\"");
        assertEquals(5.0, response.defaultLimits().requestsPerSecond());
        assertEquals(4, response.defaultLimits().concurrent());

        // The "*" entry is a default and nothing else: limits, and no state.
        EgressSite all = response.sites().getFirst();
        assertTrue(all.limitsSet());
        assertEquals(0, all.inFlight());
        assertNull(all.lastUsed());

        EgressSite site = response.sites().get(1);
        assertEquals("api.example.com", site.site());
        assertEquals(1.5, site.tokens());
        assertEquals(1, site.inFlight());
        assertNull(site.blockedUntil(), "an empty blockedUntil means not blocked");
        assertEquals("2026-10-25T10:00:00Z", site.lastUsed());
    }

    @Test
    void listEgressSitesWithoutAnApplicationAsksForAll() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"sites\":[]}");
        });

        newClient().listEgressSites();

        assertEquals("{}", received.get().body());
    }

    /** A limit left null keeps what the site is held to now, so it must not be sent at all. */
    @Test
    void setEgressLimitSendsOnlyTheLimitsGiven() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"applicationId\":\"billing\",\"site\":\"api.example.com\","
                    + "\"limits\":{\"requestsPerSecond\":2.5,\"burst\":10.0,\"concurrent\":4}}");
        });

        EgressLimitResponse response = newClient().setEgressLimit("billing", "api.example.com", 2.5, null, null);

        assertEquals("set-egress-limit", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"site\":\"api.example.com\"", "\"requestsPerSecond\":2.5");
        assertFalse(received.get().body().contains("burst"));
        assertFalse(received.get().body().contains("concurrent"));
        assertEquals(2.5, response.limits().requestsPerSecond());
        assertEquals(10.0, response.limits().burst());
    }

    @Test
    void deleteEgressLimitParsesWhatTheSiteIsHeldToNow() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"applicationId\":\"billing\",\"site\":\"*\","
                    + "\"limits\":{\"requestsPerSecond\":5.0,\"burst\":10.0,\"concurrent\":0}}");
        });

        EgressLimitResponse response = newClient().deleteEgressLimit("billing", "*");

        assertEquals("delete-egress-limit", received.get().header("x-euclid-action"));
        assertEquals("*", response.site());
        assertEquals(5.0, response.limits().requestsPerSecond());
    }

    @Test
    void deleteEgressSiteForAnUnknownSiteIsNotFound() throws Exception {
        server = startServer(exchange -> sendResponse(exchange, 404,
                "{\"message\":\"application billing has no state for site gone.example.com\"}"));

        EuclidEap eap = newClient();
        EuclidServiceException thrown =
                assertThrows(EuclidServiceException.class, () -> eap.deleteEgressSite("billing", "gone.example.com"));

        assertEquals(404, thrown.statusCode());
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

    @Test
    void setLogLevelSendsTheLevelAndParsesTheAppliedChannel() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"applicationId\":\"billing\",\"logLevel\":\"debug\","
                    + "\"channel\":\"application.billing\"}");
        });

        SetLogLevelResponse response = newClient().setLogLevel("billing", "debug");

        assertEquals("set-log-level", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"applicationId\":\"billing\"", "\"level\":\"debug\"");
        assertEquals("billing", response.applicationId());
        assertEquals("debug", response.logLevel());
        assertEquals("application.billing", response.channel());
    }

    // An empty level is how the override is withdrawn rather than merely changed, so it has to
    // reach the server as an empty string and not be dropped from the body.
    @Test
    void resetLogLevelSendsAnEmptyLevel() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"applicationId\":\"billing\",\"logLevel\":\"\","
                    + "\"channel\":\"application.billing\"}");
        });

        SetLogLevelResponse response = newClient().resetLogLevel("billing");

        assertEquals("set-log-level", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"level\":\"\"");
        assertEquals("", response.logLevel());
    }

    // The server refuses a level it does not recognise rather than defaulting it, so the client
    // must surface that instead of pretending the level took effect.
    @Test
    void setLogLevelSurfacesARejectedLevel() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 400, "{\"error\":\"level must be \\\"trace\\\", ...\"}");
        });

        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> newClient().setLogLevel("billing", "warnign"));

        assertEquals("eap", exception.service());
        assertEquals("set-log-level", exception.action());
        assertEquals(400, exception.statusCode());
    }

    @Test
    void anApplicationIsAProcessWithNoScheduleByDefault() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, applicationJson());
        });

        Application found = newClient().getApplication("billing");

        assertEquals(ApplicationType.PROCESS, found.type());
        assertFalse(found.type().isJob());
        // Empty rather than the epoch: a job with no schedule is not one overdue since 1970.
        assertNull(found.nextRunAt());
        assertEquals(Map.of("os", "linux"), found.nodeLabels());
        assertTrue(found.userExists());
    }

    @Test
    void aScheduledJobReportsItsTypeScheduleAndNextRun() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, applicationJson()
                    .replace("\"type\":\"PROCESS\"", "\"type\":\"JOB\"")
                    .replace("\"schedule\":\"\"", "\"schedule\":\"0 2 * * *\"")
                    .replace("\"nextRunAt\":\"\"", "\"nextRunAt\":\"2026-10-14T02:00:00Z\""));
        });

        Application found = newClient().getApplication("billing");

        assertEquals(ApplicationType.JOB, found.type());
        assertTrue(found.type().isJob());
        assertEquals("0 2 * * *", found.schedule());
        assertEquals("2026-10-14T02:00:00Z", found.nextRunAt());
    }

    // A definition written before types existed answers without one, and has to keep the
    // long-running behaviour rather than being read as a job.
    @Test
    void anUnrecognisedTypeReadsAsUnknownAndNotAsAJob() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, applicationJson().replace("\"type\":\"PROCESS\"", "\"type\":\"CRONJOB\""));
        });

        Application found = newClient().getApplication("billing");

        assertEquals(ApplicationType.UNKNOWN, found.type());
        assertFalse(found.type().isJob(), "UNKNOWN must keep the long-running behaviour");
    }

    // userExists false is the interesting answer: it turns "euclid refused my secret" into "this
    // application's user does not exist".
    @Test
    void anApplicationWhoseUserWasDeletedSaysSo() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, applicationJson().replace("\"userExists\":true", "\"userExists\":false"));
        });

        assertFalse(newClient().getApplication("billing").userExists());
    }

    @Test
    void deployCanDefineAScheduledJobWithNodeLabels() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, applicationJson());
        });

        newClient().createApplication(CreateApplicationRequest.builder()
                .applicationId("nightly-import").runtime("JAVA").bucket("artifacts")
                .artifact("import-1.0.jar").type(ApplicationType.JOB).schedule("0 2 * * *")
                .nodeLabels(Map.of("os", "windows")).build());

        assertBodyContains(received.get().body(), "\"type\":\"JOB\"", "\"schedule\":\"0 2 * * *\"",
                "\"nodeLabels\":{\"os\":\"windows\"}");
    }

    // A schedule only means something on a JOB, and the server says so rather than storing one
    // that never fires.
    @Test
    void deploySurfacesAScheduleOnANonJob() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 400, "{\"error\":\"schedule is only valid on a JOB - pass '--type JOB' as well\"}");
        });

        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> newClient().createApplication(CreateApplicationRequest.builder()
                        .applicationId("billing").schedule("0 2 * * *").build()));

        assertEquals("create-application", exception.action());
        assertEquals(400, exception.statusCode());
    }

    // Case-sensitive on purpose: "job" must not quietly turn a service into a one-shot task.
    @Test
    void deploySurfacesAMisspelledType() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 400, "{\"error\":\"type must be PROCESS or JOB, not 'job'\"}");
        });

        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> newClient().createApplication(CreateApplicationRequest.builder()
                        .applicationId("billing").build()));

        assertEquals(400, exception.statusCode());
    }

    // An empty schedule is how a job goes back to on-demand only, so it has to reach the server
    // rather than being dropped as unset.
    @Test
    void updateCanUnscheduleAJob() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, applicationJson());
        });

        newClient().updateApplication(UpdateApplicationRequest.builder()
                .applicationId("nightly-import").schedule("").build());

        String body = received.get().body();
        assertBodyContains(body, "\"schedule\":\"\"");
        assertFalse(body.contains("\"type\""), "an unset type must not reach the server: " + body);
    }

    @Test
    void listNodesParsesTheRegistrations() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"nodes\":[" + nodeJson(true, false) + "],\"total\":1}");
        });

        ListNodesResponse response = newClient().listNodes();

        assertEquals("list-nodes", received.get().header("x-euclid-action"));
        assertEquals(1, response.total());

        Node node = response.nodes().getFirst();
        assertEquals("worker-1", node.name());
        assertEquals(Map.of("os", "linux", "gpu", "true"), node.labels());
        assertEquals(16, node.cpuCount());
        assertTrue(node.live());
        assertFalse(node.drained());
        // A listing says nothing about what a node runs; an empty list would read as "holding
        // nothing", which is a different claim.
        assertNull(node.applications(), "list-nodes must not imply a node is holding nothing");
    }

    @Test
    void aNodeThatStoppedRenewingIsNotLive() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, "{\"nodes\":[" + nodeJson(false, true) + "],\"total\":1}");
        });

        Node node = newClient().listNodes().nodes().getFirst();

        assertFalse(node.live());
        assertTrue(node.drained(), "draining refuses new work while keeping what it has");
    }

    @Test
    void getNodeAlsoReportsWhatItIsRunning() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, nodeJson(true, false).replaceFirst("\\}$",
                    ",\"applications\":[{\"applicationId\":\"billing\",\"runtimeName\":\"billing-7f3a91\","
                            + "\"namespace\":\"development\",\"runtime\":\"JAVA\",\"instances\":4,"
                            + "\"running\":1}]}"));
        });

        Node node = newClient().getNode("worker-1");

        assertEquals("get-node", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"node\":\"worker-1\"");
        assertEquals(1, node.applications().size());
        // Four slots held, one actually serving - a single count could not say this.
        assertEquals(4, node.applications().getFirst().instances());
        assertEquals(1, node.applications().getFirst().running());
    }

    @Test
    void getNodeSurfacesANodeThatIsNotRegistered() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 404, "{\"error\":\"Node is not registered, node: nope\"}");
        });

        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> newClient().getNode("nope"));

        assertEquals("eap", exception.service());
        assertEquals("get-node", exception.action());
        assertEquals(404, exception.statusCode());
    }

    @Test
    void deleteNodeSendsTheName() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"node\":\"worker-1\",\"deleted\":true}");
        });

        newClient().deleteNode("worker-1");

        assertEquals("delete-node", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"node\":\"worker-1\"");
    }

    // Administrators only, unlike the other node actions: a worker able to delete another's
    // registration could re-register under its name and inherit its credentials.
    @Test
    void deleteNodeSurfacesANonAdministrator() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 403, "{\"error\":\"Administrator privileges required\"}");
        });

        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> newClient().deleteNode("worker-1"));

        assertEquals(403, exception.statusCode());
    }

    private static String nodeJson(boolean live, boolean drained) {
        return "{\"name\":\"worker-1\",\"address\":\"10.0.0.7:9100\",\"principal\":\"node-worker-1\","
                + "\"labels\":{\"os\":\"linux\",\"gpu\":\"true\"},\"cpuCount\":16,"
                + "\"version\":\"1.2.20\",\"os\":\"linux\",\"arch\":\"x86_64\","
                + "\"drained\":" + drained + ",\"live\":" + live + ",\"lastSeen\":\"2026-10-13T08:00:00Z\"}";
    }

    private static String applicationJson() {
        return applicationJson("STOPPED", "STOPPED", 0);
    }

    private static String applicationJson(String desiredState, String state, int instances) {
        return "{\"applicationId\":\"billing\",\"runtimeName\":\"billing-7f3a91\","
                + "\"ern\":\"ern:eap:eu-central-1:863459426936:application/billing\","
                + "\"accountId\":\"863459426936\",\"namespace\":\"development\","
                + "\"region\":\"eu-central-1\",\"runtime\":\"JAVA\","
                + "\"bucketErn\":\"ern:esm:eu-central-1:863459426936:bucket/artifacts\","
                + "\"artifactKey\":\"billing-1.0.0.jar\",\"version\":\"1.0.0\","
                + "\"md5Sum\":\"0dc7cdef5e707bae7f7b6bbb5be4c32a\",\"command\":\"java\","
                + "\"arguments\":[\"-jar\",\"billing-1.0.jar\"],\"environment\":{\"LOG_LEVEL\":\"info\"},"
                + "\"resources\":[\"ern:esm:eu-central-1:863459426936:bucket/invoices\"],"
                + "\"userId\":\"eap-billing\",\"userExists\":true,\"logLevel\":\"info\","
                + "\"minInstances\":2,\"maxInstances\":5,\"readyTimeoutMs\":60000,"
                + "\"nodeLabels\":{\"os\":\"linux\"},\"type\":\"PROCESS\","
                + "\"schedule\":\"\",\"nextRunAt\":\"\","
                + "\"desiredState\":\"" + desiredState + "\",\"state\":\"" + state + "\",\"instances\":" + instances
                + ",\"endpoints\":[{\"instanceId\":\"i-1\",\"pid\":4711,\"httpPort\":34001},"
                + "{\"instanceId\":\"i-2\",\"pid\":4712,\"httpPort\":34002}]"
                + ",\"created\":\"2026-01-01\",\"modified\":\"2026-01-02\"}";
    }

    @Test
    void theEgressCallsSendTheSiteAndParseTheAnswer() throws Exception {
        List<SignableRequest> received = new java.util.concurrent.CopyOnWriteArrayList<>();
        server = startServer(exchange -> {
            SignableRequest request = captureRequest(exchange);
            received.add(request);
            String action = request.header("x-euclid-action");
            String body = switch (action) {
                case "acquire-egress-permit" -> "{\"site\":\"downloads.example.com\",\"granted\":false,"
                        + "\"reason\":\"rate\",\"retryAfterMs\":250,\"limits\":{}}";
                case "release-egress-permit" -> "{\"site\":\"downloads.example.com\",\"released\":true}";
                default -> "{\"site\":\"downloads.example.com\",\"blockedUntil\":\"2026-10-16T12:00:00Z\"}";
            };
            sendResponse(exchange, 200, body);
        });

        EuclidEap eap = newClient();
        var answer = eap.acquireEgressPermit("downloader", "downloads.example.com", 600);
        assertTrue(eap.releaseEgressPermit("downloader", "downloads.example.com", "p-1"));
        eap.reportEgressThrottled("downloader", "downloads.example.com", 90);

        assertFalse(answer.granted());
        assertEquals("rate", answer.reason());
        assertEquals(250, answer.retryAfterMs());

        assertEquals("acquire-egress-permit", received.get(0).header("x-euclid-action"));
        assertBodyContains(received.get(0).body(), "\"applicationId\":\"downloader\"",
                "\"site\":\"downloads.example.com\"", "\"leaseSeconds\":600");
        assertBodyContains(received.get(1).body(), "\"applicationId\":\"downloader\"", "\"permitId\":\"p-1\"");
        assertBodyContains(received.get(2).body(), "\"applicationId\":\"downloader\"");
        assertEquals("report-egress-throttled", received.get(2).header("x-euclid-action"));
        assertBodyContains(received.get(2).body(), "\"retryAfterSeconds\":90");
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
