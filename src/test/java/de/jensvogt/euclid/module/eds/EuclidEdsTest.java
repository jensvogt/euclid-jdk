package de.jensvogt.euclid.module.eds;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.jensvogt.euclid.auth.SigV4;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.dto.eds.CreateZoneRequest;
import de.jensvogt.euclid.dto.eds.ListRecordsRequest;
import de.jensvogt.euclid.dto.eds.ListRecordsResponse;
import de.jensvogt.euclid.dto.eds.ListZonesRequest;
import de.jensvogt.euclid.dto.eds.ListZonesResponse;
import de.jensvogt.euclid.dto.eds.PutRecordRequest;
import de.jensvogt.euclid.dto.eds.model.Answer;
import de.jensvogt.euclid.dto.eds.model.HostedZone;
import de.jensvogt.euclid.dto.eds.model.RecordSet;
import de.jensvogt.euclid.exception.EuclidServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms EuclidEds authenticates the way it claims to (SigV4-signed when an access key is
 * configured, bearer token otherwise), addresses every action to the {@code eds} target with the
 * body the server's DTOs expect, parses the zone, record-set and answer shapes EDS describes, and
 * surfaces non-2xx responses as {@link EuclidServiceException}.
 */
class EuclidEdsTest {

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
    void listZonesSignsWithSigV4WhenAccessKeyConfigured() throws Exception {
        String accessKeyId = "AKIDEXAMPLE";
        String secretAccessKey = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";

        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"zones\":[],\"total\":0}");
        });

        new EuclidEds(baseUrl(), "unused-token", "eu-central-1", "863459426936", "alice",
                accessKeyId, secretAccessKey, null, null).listZones();

        SignableRequest req = received.get();
        assertTrue(req.header("authorization").startsWith("AWS4-HMAC-SHA256 "));
        Optional<SigV4.VerifyResult> result = SigV4.verify(req,
                id -> id.equals(accessKeyId) ? Optional.of(secretAccessKey) : Optional.empty());
        assertTrue(result.isPresent(), "server-side verification of the client's own signature must succeed");
        assertEquals("eds", req.header("x-euclid-target"));
    }

    @Test
    void createZoneSendsTheZoneAndParsesWhatWasStored() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 201, zoneJson(0));
        });

        HostedZone zone = newClient().createZone(CreateZoneRequest.builder().name("corp.example").ttl(600)
                .description("office network").build());

        SignableRequest req = received.get();
        assertEquals("Bearer test-token", req.header("authorization"));
        assertEquals("eds", req.header("x-euclid-target"));
        assertEquals("create-zone", req.header("x-euclid-action"));
        assertEquals("{\"name\":\"corp.example\",\"ttl\":600,\"description\":\"office network\"}", req.body());
        assertEquals("corp.example", zone.name());
        assertEquals("ern:eds:eu-central-1:863459426936:prod:zone:corp.example", zone.ern());
        assertEquals("prod", zone.namespace());
        assertEquals(600, zone.ttl());
        assertEquals("office network", zone.description());
        assertEquals(0, zone.recordCount());
        assertEquals(1793527200L, zone.serial());
        assertEquals("2026-11-01T10:00:00Z", zone.created());
        assertEquals("2026-11-01T10:00:00Z", zone.modified());
    }

    @Test
    void createZoneByNameLeavesTheDescriptionOut() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 201, zoneJson(0));
        });

        newClient().createZone("corp.example");

        assertEquals("{\"name\":\"corp.example\",\"ttl\":0}", received.get().body());
    }

    // A zone name is unique across the installation, so another account holding it is a conflict.
    @Test
    void createZoneSurfacesAConflict() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 409, "{\"error\":\"Zone exists already: corp.example\"}");
        });

        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> newClient().createZone("corp.example"));

        assertEquals("eds", exception.service());
        assertEquals("create-zone", exception.action());
        assertEquals(409, exception.statusCode());
    }

    @Test
    void getZoneNamesTheZoneAndReadsTheRecordCount() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, zoneJson(7));
        });

        HostedZone zone = newClient().getZone("corp.example");

        assertEquals("get-zone", received.get().header("x-euclid-action"));
        assertEquals("{\"name\":\"corp.example\"}", received.get().body());
        assertEquals(7, zone.recordCount());
    }

    @Test
    void listZonesWithoutAPrefixSendsNone() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"zones\":[" + zoneJson(3) + "],\"total\":1}");
        });

        ListZonesResponse response = newClient().listZones();

        assertEquals("list-zones", received.get().header("x-euclid-action"));
        assertFalse(received.get().body().contains("prefix"), received.get().body());
        assertEquals(1, response.total());
        assertEquals(1, response.zones().size());
        assertEquals("corp.example", response.zones().get(0).name());
        assertEquals(3, response.zones().get(0).recordCount());
    }

    @Test
    void listZonesSendsThePrefixAndPage() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"zones\":[],\"total\":0}");
        });

        newClient().listZones(ListZonesRequest.builder().prefix("corp").pageSize(20).pageIndex(1).build());

        assertBodyContains(received.get().body(), "\"prefix\":\"corp\"", "\"pageSize\":20", "\"pageIndex\":1");
    }

    @Test
    void deleteZoneReturnsHowManyRecordSetsWentWithIt() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"deletedRecords\":5}");
        });

        assertEquals(5, newClient().deleteZone("corp.example"));
        assertEquals("delete-zone", received.get().header("x-euclid-action"));
        assertEquals("{\"name\":\"corp.example\"}", received.get().body());
    }

    // Another account's zone is answered exactly as a missing one is.
    @Test
    void anotherAccountsZoneIsNotFound() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 404, "{\"error\":\"Zone does not exist: other.example\"}");
        });

        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> newClient().getZone("other.example"));

        assertEquals("get-zone", exception.action());
        assertEquals(404, exception.statusCode());
    }

    @Test
    void putRecordSendsTheWholeSetAndParsesIt() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, recordJson());
        });

        RecordSet record = newClient().putRecord(PutRecordRequest.builder().zone("corp.example").name("api")
                .type("A").ttl(60).values("10.1.2.3", "10.1.2.4").build());

        assertEquals("put-record", received.get().header("x-euclid-action"));
        assertEquals("{\"zone\":\"corp.example\",\"name\":\"api\",\"type\":\"A\",\"ttl\":60,"
                + "\"values\":[\"10.1.2.3\",\"10.1.2.4\"]}", received.get().body());
        assertEquals("corp.example", record.zone());
        assertEquals("api.corp.example", record.name());
        assertEquals("A", record.type());
        assertEquals(60, record.ttl());
        assertEquals(List.of("10.1.2.3", "10.1.2.4"), record.values());
        assertEquals("2026-11-01T10:00:00Z", record.created());
    }

    @Test
    void putRecordShorthandWritesAtTheGivenName() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, recordJson());
        });

        newClient().putRecord("corp.example", "@", "MX", "10 mail.corp.example");

        assertBodyContains(received.get().body(), "\"name\":\"@\"", "\"type\":\"MX\"", "\"ttl\":0",
                "\"values\":[\"10 mail.corp.example\"]");
    }

    @Test
    void putRecordBuilderDefaultsToTheApex() {
        assertEquals("@", PutRecordRequest.builder().zone("corp.example").type("TXT").values("v=spf1 -all")
                .build().name());
    }

    @Test
    void putRecordSurfacesACnameConflict() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 409, "{\"error\":\"www.corp.example has A records, and a CNAME cannot sit beside them\"}");
        });

        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> newClient().putRecord("corp.example", "www", "CNAME", "api.corp.example"));

        assertEquals("put-record", exception.action());
        assertEquals(409, exception.statusCode());
    }

    @Test
    void deleteRecordNamesTheSet() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"deleted\":true}");
        });

        newClient().deleteRecord("corp.example", "api", "A");

        assertEquals("delete-record", received.get().header("x-euclid-action"));
        assertEquals("{\"zone\":\"corp.example\",\"name\":\"api\",\"type\":\"A\"}", received.get().body());
    }

    @Test
    void deleteRecordWithoutANameMeansTheApex() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"deleted\":true}");
        });

        newClient().deleteRecord("corp.example", null, "TXT");

        assertBodyContains(received.get().body(), "\"name\":\"@\"");
    }

    @Test
    void listRecordsSendsOnlyTheFiltersThatAreSet() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"records\":[" + recordJson() + "],\"total\":12}");
        });

        ListRecordsResponse response = newClient().listRecords("corp.example");

        assertEquals("list-records", received.get().header("x-euclid-action"));
        String body = received.get().body();
        assertBodyContains(body, "\"zone\":\"corp.example\"");
        assertFalse(body.contains("\"name\""), body);
        assertFalse(body.contains("\"type\""), body);
        assertEquals(12, response.total());
        assertEquals(1, response.records().size());
        assertEquals("api.corp.example", response.records().get(0).name());
    }

    @Test
    void listRecordsSendsTheFiltersAndPage() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"records\":[],\"total\":0}");
        });

        newClient().listRecords(ListRecordsRequest.builder().zone("corp.example").name("api").type("AAAA")
                .pageSize(50).pageIndex(2).build());

        assertBodyContains(received.get().body(), "\"name\":\"api\"", "\"type\":\"AAAA\"",
                "\"pageSize\":50", "\"pageIndex\":2");
    }

    @Test
    void getAnswerParsesAnAuthoritativeAnswer() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"name\":\"www.corp.example\",\"type\":\"A\",\"authoritative\":true,"
                    + "\"zone\":\"corp.example\",\"rcode\":\"NOERROR\",\"answers\":["
                    + "{\"name\":\"www.corp.example\",\"type\":\"CNAME\",\"ttl\":300,\"value\":\"api.corp.example.\"},"
                    + "{\"name\":\"api.corp.example\",\"type\":\"A\",\"ttl\":60,\"value\":\"10.1.2.3\"}],"
                    + "\"authority\":[]}");
        });

        Answer answer = newClient().getAnswer("www.corp.example", "A");

        assertEquals("get-answer", received.get().header("x-euclid-action"));
        assertEquals("{\"name\":\"www.corp.example\",\"type\":\"A\"}", received.get().body());
        assertTrue(answer.authoritative());
        assertEquals("corp.example", answer.zone());
        assertEquals("NOERROR", answer.rcode());
        assertEquals(2, answer.answers().size());
        assertEquals("CNAME", answer.answers().get(0).type());
        assertEquals("10.1.2.3", answer.answers().get(1).value());
        assertEquals(60, answer.answers().get(1).ttl());
        assertTrue(answer.authority().isEmpty());
    }

    @Test
    void getAnswerForANameInNoZoneIsRefused() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, "{\"name\":\"example.org\",\"type\":\"A\",\"authoritative\":false,"
                    + "\"rcode\":\"REFUSED\",\"answers\":[]}");
        });

        Answer answer = newClient().getAnswer("example.org");

        assertFalse(answer.authoritative());
        assertNull(answer.zone());
        assertEquals("REFUSED", answer.rcode());
        assertTrue(answer.answers().isEmpty());
        assertTrue(answer.authority().isEmpty());
    }

    @Test
    void getAnswerCarriesTheSoaOnANegativeAnswer() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, "{\"name\":\"nope.corp.example\",\"type\":\"A\",\"authoritative\":true,"
                    + "\"zone\":\"corp.example\",\"rcode\":\"NXDOMAIN\",\"answers\":[],\"authority\":["
                    + "{\"name\":\"corp.example\",\"type\":\"SOA\",\"ttl\":300,"
                    + "\"value\":\"localhost. hostmaster.corp.example. 1793527200 3600 600 86400 300\"}]}");
        });

        Answer answer = newClient().getAnswer("nope.corp.example");

        assertEquals("NXDOMAIN", answer.rcode());
        assertEquals(1, answer.authority().size());
        assertEquals("SOA", answer.authority().get(0).type());
    }

    @Test
    void namespaceIsSentWhenTheSessionIsScoped() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"zones\":[],\"total\":0}");
        });

        new EuclidEds(baseUrl(), "test-token", "eu-central-1", "863459426936", "alice", null, null, null, "prod")
                .listZones();

        assertEquals("prod", received.get().header("x-euclid-namespace"));
    }

    private static String zoneJson(long recordCount) {
        return "{\"name\":\"corp.example\",\"ern\":\"ern:eds:eu-central-1:863459426936:prod:zone:corp.example\","
                + "\"namespace\":\"prod\",\"ttl\":600,\"description\":\"office network\",\"recordCount\":" + recordCount + ","
                + "\"serial\":1793527200,\"created\":\"2026-11-01T10:00:00Z\",\"modified\":\"2026-11-01T10:00:00Z\"}";
    }

    private static String recordJson() {
        return "{\"zone\":\"corp.example\",\"name\":\"api.corp.example\",\"type\":\"A\",\"ttl\":60,"
                + "\"values\":[\"10.1.2.3\",\"10.1.2.4\"],\"created\":\"2026-11-01T10:00:00Z\","
                + "\"modified\":\"2026-11-01T10:00:00Z\"}";
    }

    private EuclidEds newClient() {
        return new EuclidEds(baseUrl(), "test-token", "eu-central-1", "863459426936", "alice", null, null, null, null);
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
