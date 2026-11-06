package de.jensvogt.euclid.module.emm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.jensvogt.euclid.auth.SigV4;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.dto.emm.CreateBackupResponse;
import de.jensvogt.euclid.dto.emm.ExportRequest;
import de.jensvogt.euclid.dto.emm.ImportRequest;
import de.jensvogt.euclid.dto.emm.ImportResponse;
import de.jensvogt.euclid.dto.emm.ListBackupsResponse;
import de.jensvogt.euclid.dto.emm.ListModulesResponse;
import de.jensvogt.euclid.dto.emm.ModuleStoppedResponse;
import de.jensvogt.euclid.dto.emm.RestartModuleResponse;
import de.jensvogt.euclid.dto.emm.SetInstancesRequest;
import de.jensvogt.euclid.dto.emm.SetInstancesResponse;
import de.jensvogt.euclid.dto.emm.SetLogLevelResponse;
import de.jensvogt.euclid.dto.emm.SetThreadsResponse;
import de.jensvogt.euclid.dto.emm.model.BackupFile;
import de.jensvogt.euclid.dto.emm.model.ManagedModule;
import de.jensvogt.euclid.dto.emm.model.ModuleInstance;
import de.jensvogt.euclid.exception.EuclidServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms EuclidEmm authenticates the way it claims to (SigV4-signed when an access key is
 * configured, bearer token otherwise), addresses every action to the {@code emm} target with a
 * correctly-shaped body, parses the responses, and surfaces non-2xx responses as
 * {@link EuclidServiceException}.
 * <p>
 * The export and import cases carry most of the weight: a sealed export is assembled from one
 * request per module, tied together by the salt the first response carries, and must come out as
 * the same file euclid-cli writes - otherwise an archive made by one could not be restored by the
 * other.
 */
class EuclidEmmTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

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
    void listModulesSignsWithSigV4WhenAccessKeyConfigured() throws Exception {
        String accessKeyId = "AKIDEXAMPLE";
        String secretAccessKey = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";

        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"modules\":[],\"total\":0}");
        });

        new EuclidEmm(baseUrl(), "unused-token", "eu-central-1", "863459426936", "admin",
                accessKeyId, secretAccessKey, null, null).listModules();

        SignableRequest req = received.get();
        assertTrue(req.header("authorization").startsWith("AWS4-HMAC-SHA256 "));
        Optional<SigV4.VerifyResult> result = SigV4.verify(req,
                id -> id.equals(accessKeyId) ? Optional.of(secretAccessKey) : Optional.empty());
        assertTrue(result.isPresent(), "server-side verification of the client's own signature must succeed");
        assertEquals("emm", req.header("x-euclid-target"));
    }

    @Test
    void listModulesParsesModulesAndInstances() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"modules\":[" + moduleJson() + "],\"total\":1}");
        });

        ListModulesResponse response = newClient().listModules();

        assertEquals("Bearer test-token", received.get().header("authorization"));
        assertEquals("emm", received.get().header("x-euclid-target"));
        assertEquals("list-modules", received.get().header("x-euclid-action"));
        assertEquals(1, response.total());
        ManagedModule module = response.modules().get(0);
        assertEquals("esm", module.name());
        assertTrue(module.core());
        assertFalse(module.desiredStopped());
        assertEquals(1, module.minInstances());
        assertEquals(4, module.maxInstances());
        assertEquals(-1, module.desiredMinInstances());
        assertEquals(8, module.desiredThreads());
        assertEquals("warning", module.logLevel());
        assertNull(module.lastStartTime());
        ModuleInstance instance = module.instances().get(0);
        assertEquals("esm-1", instance.instanceId());
        assertEquals(4242, instance.pid());
        assertEquals("RUNNING", instance.state());
        assertEquals(0.25, instance.utilisation());
        assertEquals(-1, instance.backlog());
        assertEquals(2, instance.restartCount());
    }

    @Test
    void setInstancesLeavesOutTheLimitItWasNotGiven() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200,
                    "{\"name\":\"esm\",\"minInstances\":1,\"maxInstances\":6,\"runningInstances\":2}");
        });

        SetInstancesResponse response = newClient().setInstances(
                SetInstancesRequest.builder().name("esm").maxInstances(6).build());

        assertEquals("set-instances", received.get().header("x-euclid-action"));
        assertEquals("{\"name\":\"esm\",\"maxInstances\":6}", received.get().body());
        assertEquals(1, response.minInstances());
        assertEquals(6, response.maxInstances());
        assertEquals(2, response.runningInstances());
    }

    @Test
    void setThreadsSendsNameAndCount() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"name\":\"eqs\",\"threads\":16,\"runningInstances\":3}");
        });

        SetThreadsResponse response = newClient().setThreads("eqs", 16);

        assertEquals("set-threads", received.get().header("x-euclid-action"));
        assertEquals("{\"name\":\"eqs\",\"threads\":16}", received.get().body());
        assertEquals(16, response.threads());
        assertEquals(3, response.runningInstances());
    }

    @Test
    void setLogLevelSendsTheLevel() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"name\":\"esm\",\"logLevel\":\"error\",\"channel\":\"module.esm\"}");
        });

        SetLogLevelResponse response = newClient().setLogLevel("esm", "error");

        assertEquals("set-log-level", received.get().header("x-euclid-action"));
        assertEquals("{\"name\":\"esm\",\"level\":\"error\"}", received.get().body());
        assertEquals("error", response.logLevel());
        assertEquals("module.esm", response.channel());
    }

    // "default" is the word euclid-cli takes for going back to the configured level; the server
    // reads an empty level that way.
    @Test
    void setLogLevelDefaultSendsAnEmptyLevel() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"name\":\"esm\",\"logLevel\":\"\",\"channel\":\"module.esm\"}");
        });

        newClient().setLogLevel("esm", "default");

        assertEquals("{\"name\":\"esm\",\"level\":\"\"}", received.get().body());
    }

    @Test
    void stopAndStartModuleSendTheNameAndParseTheState() throws Exception {
        List<SignableRequest> received = new CopyOnWriteArrayList<>();
        server = startServer(exchange -> {
            SignableRequest req = captureRequest(exchange);
            received.add(req);
            boolean stopped = "stop-module".equals(req.header("x-euclid-action"));
            sendResponse(exchange, 200, "{\"name\":\"ftp\",\"stopped\":" + stopped + ",\"runningInstances\":1}");
        });

        ModuleStoppedResponse stop = newClient().stopModule("ftp");
        ModuleStoppedResponse start = newClient().startModule("ftp");

        assertEquals("stop-module", received.get(0).header("x-euclid-action"));
        assertEquals("start-module", received.get(1).header("x-euclid-action"));
        assertEquals("{\"name\":\"ftp\"}", received.get(0).body());
        assertTrue(stop.stopped());
        assertFalse(start.stopped());
        assertEquals(1, start.runningInstances());
    }

    @Test
    void stopModuleSurfacesTheRefusalToStopEmm() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 400, "{\"error\":\"emm cannot stop itself\"}");
        });

        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> newClient().stopModule("emm"));

        assertEquals("emm", exception.service());
        assertEquals("stop-module", exception.action());
        assertEquals(400, exception.statusCode());
    }

    @Test
    void restartModuleSendsTheName() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"name\":\"esm\",\"runningInstances\":3}");
        });

        RestartModuleResponse response = newClient().restartModule("esm");

        assertEquals("restart-module", received.get().header("x-euclid-action"));
        assertEquals("{\"name\":\"esm\"}", received.get().body());
        assertEquals(3, response.runningInstances());
    }

    @Test
    void listBackupsParsesTheConfigurationAndTheFiles() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"directory\":\"/backup\",\"enabled\":true,\"schedule\":\"0 1 * * *\","
                    + "\"keep\":7,\"full\":false,\"encrypted\":true,\"backups\":[{\"name\":\"euclid-2026.zip\","
                    + "\"path\":\"/backup/acc/euclid-2026.zip\",\"accountId\":\"acc\",\"namespace\":\"\","
                    + "\"startedAt\":\"2026-11-06T01:00:00Z\",\"modified\":\"2026-11-06T01:02:00Z\","
                    + "\"size\":12345,\"complete\":false}]}");
        });

        ListBackupsResponse response = newClient().listBackups();

        assertEquals("list-backups", received.get().header("x-euclid-action"));
        assertEquals("/backup", response.directory());
        assertTrue(response.enabled());
        assertEquals("0 1 * * *", response.schedule());
        assertEquals(7, response.keep());
        assertTrue(response.encrypted());
        BackupFile file = response.backups().get(0);
        assertEquals("acc", file.accountId());
        assertEquals(12345, file.size());
        assertFalse(file.complete());
    }

    @Test
    void createBackupLetsTheConfigurationDecideByDefault() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 202, "{\"name\":\"euclid-2026.zip\",\"directory\":\"/backup\","
                    + "\"startedAt\":\"2026-11-06T10:00:00Z\",\"full\":false,\"encrypted\":false}");
        });

        CreateBackupResponse response = newClient().createBackup();

        assertEquals("create-backup", received.get().header("x-euclid-action"));
        assertEquals("{}", received.get().body());
        assertEquals("euclid-2026.zip", response.name());
        assertEquals("/backup", response.directory());
        assertFalse(response.full());
    }

    @Test
    void createBackupCanAskForAFullBackup() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 202, "{\"name\":\"n\",\"directory\":\"d\",\"startedAt\":\"s\",\"full\":true,"
                    + "\"encrypted\":false}");
        });

        assertTrue(newClient().createBackup(true).full());
        assertEquals("{\"full\":true}", received.get().body());
    }

    @Test
    void createBackupSurfacesABackupAlreadyRunning() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 409, "{\"error\":\"a backup is already being written; see list-backups\"}");
        });

        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> newClient().createBackup());

        assertEquals("create-backup", exception.action());
        assertEquals(409, exception.statusCode());
    }

    @Test
    void aCallerWhoIsNotAnAdministratorGetsA403() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 403, "{\"error\":\"administrator privileges required\"}");
        });

        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> newClient().listModules());

        assertEquals("list-modules", exception.action());
        assertEquals(403, exception.statusCode());
    }

    @Test
    void anUnencryptedExportIsTheServersAnswerUntouched() throws Exception {
        String document = "{\"modules\":[\"esm\",\"eqs\"],\"full\":true,\"exportedAt\":\"2026-11-06T10:00:00Z\","
                + "\"collections\":{\"esm_bucket\":[{\"_id\":\"1\"}],\"eqs_queue\":[]}}";
        List<SignableRequest> received = new CopyOnWriteArrayList<>();
        server = startServer(exchange -> {
            received.add(captureRequest(exchange));
            sendResponse(exchange, 200, document);
        });

        String exported = newClient().exportModules(ExportRequest.builder().modules("esm", "eqs").full(true).build());

        assertEquals(1, received.size());
        assertEquals("export", received.get(0).header("x-euclid-action"));
        assertEquals("{\"full\":true,\"modules\":[\"esm\",\"eqs\"]}", received.get(0).body());
        assertEquals(document, exported);
    }

    @Test
    void anExportOfEverythingSendsAll() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"modules\":[],\"collections\":{}}");
        });

        newClient().exportModules(ExportRequest.builder().all(true).build());

        assertEquals("{\"full\":false,\"all\":true}", received.get().body());
    }

    // One request per module, every one after the first carrying back the first one's salt, and
    // the frames gathered under the first envelope - exactly what euclid-cli writes to its file.
    @Test
    void aSealedExportIsAssembledFromOneFramePerModule() throws Exception {
        List<SignableRequest> received = new CopyOnWriteArrayList<>();
        server = startServer(exchange -> {
            SignableRequest req = captureRequest(exchange);
            received.add(req);
            String module = MAPPER.readTree(req.body()).path("modules").get(0).asText();
            sendResponse(exchange, 200, "{\"encrypted\":true,\"modules\":[\"" + module + "\"],\"full\":false,"
                    + "\"exportedAt\":\"2026-11-06T10:00:00Z\",\"kdf\":\"pbkdf2-sha256\",\"iterations\":600000,"
                    + "\"salt\":\"c2FsdHNhbHRzYWx0c2FsdA==\",\"cipher\":\"aes-256-gcm\",\"data\":\"frame-" + module + "\"}");
        });

        String exported = newClient().exportModules(
                ExportRequest.builder().modules("esm", "ekm").passphrase("s3cret").build());

        assertEquals(2, received.size());
        JsonNode first = MAPPER.readTree(received.get(0).body());
        JsonNode second = MAPPER.readTree(received.get(1).body());
        assertEquals("s3cret", first.path("passphrase").asText());
        assertFalse(first.has("salt"), "the first request has no salt to hand back yet");
        assertEquals("c2FsdHNhbHRzYWx0c2FsdA==", second.path("salt").asText());
        assertEquals("ekm", second.path("modules").get(0).asText());

        JsonNode archive = MAPPER.readTree(exported);
        assertFalse(archive.has("data"), "the payload belongs in frames, not on the envelope");
        assertTrue(archive.path("encrypted").asBoolean());
        assertEquals("c2FsdHNhbHRzYWx0c2FsdA==", archive.path("salt").asText());
        assertEquals(MAPPER.readTree("[\"esm\",\"ekm\"]"), archive.path("modules"));
        assertEquals(MAPPER.readTree("[\"frame-esm\",\"frame-ekm\"]"), archive.path("frames"));
    }

    @Test
    void anExportNeedsExactlyOneOfAllAndModules() {
        EuclidEmm client = new EuclidEmm("http://localhost:1", "t", null, null, null, null, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> client.exportModules(ExportRequest.builder().build()));
        assertThrows(IllegalArgumentException.class,
                () -> client.exportModules(ExportRequest.builder().all(true).modules("esm").build()));
    }

    @Test
    void exportSurfacesEkmWithoutAPassphrase() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 400, "{\"error\":\"exporting ekm requires a \\\"passphrase\\\"\"}");
        });

        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> newClient().exportModules("ekm"));

        assertEquals("export", exception.action());
        assertEquals(400, exception.statusCode());
    }

    @Test
    void importSendsThePlainDocumentAndParsesTheOutcome() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"imported\":{\"esm_bucket\":{\"imported\":3},"
                    + "\"eqs_queue\":{\"imported\":1,\"failed\":2}},"
                    + "\"skipped\":[{\"collection\":\"other\",\"reason\":\"unknown collection\"}]}");
        });

        ImportResponse response = newClient().importModules(
                "{\"modules\":[\"esm\"],\"collections\":{\"esm_bucket\":[{\"_id\":\"1\"}]}}");

        assertEquals("import", received.get().header("x-euclid-action"));
        JsonNode body = MAPPER.readTree(received.get().body());
        assertTrue(body.has("collections"));
        assertFalse(body.has("passphrase"));
        assertEquals(3, response.imported().get("esm_bucket").imported());
        assertEquals(0, response.imported().get("esm_bucket").failed());
        assertEquals(2, response.imported().get("eqs_queue").failed());
        assertEquals("other", response.skipped().get(0).collection());
        assertEquals("unknown collection", response.skipped().get(0).reason());
    }

    @Test
    void importAddsThePassphraseAndTheModuleFilterToASealedArchive() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"imported\":{},\"skipped\":[]}");
        });

        newClient().importModules(ImportRequest.builder()
                .document("{\"encrypted\":true,\"salt\":\"abc\",\"frames\":[\"f1\",\"f2\"]}")
                .modules("esm").passphrase("s3cret").build());

        JsonNode body = MAPPER.readTree(received.get().body());
        assertEquals("s3cret", body.path("passphrase").asText());
        assertEquals("abc", body.path("salt").asText());
        assertEquals(MAPPER.readTree("[\"f1\",\"f2\"]"), body.path("frames"));
        assertEquals(MAPPER.readTree("[\"esm\"]"), body.path("modules"));
    }

    @Test
    void importChecksTheArchiveBeforeSendingIt() {
        EuclidEmm client = new EuclidEmm("http://localhost:1", "t", null, null, null, null, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> client.importModules("not json"));
        assertThrows(IllegalArgumentException.class, () -> client.importModules("{\"something\":\"else\"}"));
        // Sealed without a passphrase, and plain with one.
        assertThrows(IllegalArgumentException.class, () -> client.importModules("{\"frames\":[]}"));
        assertThrows(IllegalArgumentException.class, () -> client.importModules(ImportRequest.builder()
                .document("{\"collections\":{}}").passphrase("unneeded").build()));
    }

    @Test
    void importSurfacesAWrongPassphrase() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 400, "{\"error\":\"cannot open this export: wrong passphrase\"}");
        });

        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> newClient().importModules(ImportRequest.builder().document("{\"frames\":[\"f\"]}")
                        .passphrase("wrong").build()));

        assertEquals("import", exception.action());
        assertEquals(400, exception.statusCode());
    }

    private static String moduleJson() {
        return "{\"name\":\"esm\",\"executable\":\"/usr/local/euclid/bin/euclid-esm\",\"socketPath\":\"/tmp/esm.sock\","
                + "\"active\":true,\"core\":true,\"desiredStopped\":false,\"autoRestart\":true,\"maxRestarts\":5,"
                + "\"minInstances\":1,\"maxInstances\":4,\"desiredMinInstances\":-1,\"desiredMaxInstances\":-1,"
                + "\"desiredThreads\":8,\"logLevel\":\"warning\",\"created\":\"2026-01-01\",\"modified\":\"2026-01-02\","
                + "\"lastStartTime\":null,\"instances\":[{\"instanceId\":\"esm-1\",\"pid\":4242,\"host\":\"\","
                + "\"state\":\"RUNNING\",\"socketPath\":\"/tmp/esm-4242.sock\",\"httpPort\":0,\"restartCount\":2,"
                + "\"utilisation\":0.25,\"backlog\":-1,\"loadReportedAt\":\"\",\"backgroundTasks\":0,"
                + "\"started\":\"2026-11-06T09:00:00Z\",\"created\":\"2026-01-01\",\"modified\":\"2026-11-06\"}]}";
    }

    private EuclidEmm newClient() {
        return new EuclidEmm(baseUrl(), "test-token", "eu-central-1", "863459426936", "admin", null, null, null, null);
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
