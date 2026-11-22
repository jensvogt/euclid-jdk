package de.jensvogt.euclid.module.emo;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.dto.emo.ListDashboardsResponse;
import de.jensvogt.euclid.dto.emo.MetricQueryRequest;
import de.jensvogt.euclid.dto.emo.SaveDashboardResponse;
import de.jensvogt.euclid.dto.emo.model.Dashboard;
import de.jensvogt.euclid.dto.emo.model.MetricDataPoint;
import de.jensvogt.euclid.exception.EuclidServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms EuclidEmo routes the series queries ({@code list}, {@code average}) and the dashboard
 * actions to the right request with a correctly-shaped body - unset filters left out, so the server
 * applies its defaults - parses the corresponding response as EmoServer.cpp writes it, and surfaces
 * non-2xx responses as {@link EuclidServiceException}.
 */
class EuclidEmoQueryDashboardTest {

    private static final List<String> CAPTURED_HEADERS = List.of("x-euclid-target", "x-euclid-action",
            "x-euclid-account-id", "x-euclid-user-id");

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void listMetricDataSendsEveryFilterAndParsesRows() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"items\":[{\"name\":\"jvm-memory-used\","
                    + "\"labels\":{\"area\":\"heap\",\"id\":\"G1 Eden Space\"},\"labelName\":\"area\","
                    + "\"labelValue\":\"heap\",\"value\":12.5,\"minValue\":10.0,\"maxValue\":20.0,\"samples\":4,"
                    + "\"type\":\"GAUGE\",\"resolution\":\"HOUR\",\"timestamp\":\"2026-11-06T10:00:00Z\"}]}");
        });

        List<MetricDataPoint> points = newClient().listMetricData(MetricQueryRequest.builder()
                .name("jvm-memory-used")
                .labels(Map.of("area", "heap"))
                .labelName("id")
                .labelValue("G1 Eden Space")
                .resolution("HOUR")
                .from("2026-11-06T00:00:00Z")
                .to("2026-11-07T00:00:00Z")
                .limit(500L)
                .build());

        assertEquals("list", received.get().header("x-euclid-action"));
        assertEquals("emo", received.get().header("x-euclid-target"));
        assertBodyContains(received.get().body(), "\"name\":\"jvm-memory-used\"", "\"labels\":{\"area\":\"heap\"}",
                "\"labelName\":\"id\"", "\"labelValue\":\"G1 Eden Space\"", "\"resolution\":\"HOUR\"",
                "\"from\":\"2026-11-06T00:00:00Z\"", "\"to\":\"2026-11-07T00:00:00Z\"", "\"limit\":500");
        assertEquals(1, points.size());
        MetricDataPoint point = points.get(0);
        assertEquals("jvm-memory-used", point.name());
        assertEquals(Map.of("area", "heap", "id", "G1 Eden Space"), point.labels());
        assertEquals("area", point.labelName());
        assertEquals("heap", point.labelValue());
        assertEquals(12.5, point.value());
        assertEquals(10.0, point.minValue());
        assertEquals(20.0, point.maxValue());
        assertEquals(4, point.samples());
        assertEquals("GAUGE", point.type());
        assertEquals("HOUR", point.resolution());
        assertEquals("2026-11-06T10:00:00Z", point.timestamp());
    }

    @Test
    void emptyQueryLeavesEveryFilterToTheServer() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"items\":[]}");
        });

        List<MetricDataPoint> points = newClient().listMetricData(MetricQueryRequest.builder().build());

        assertEquals("{}", received.get().body());
        assertTrue(points.isEmpty());
    }

    @Test
    void averageMetricSendsQueryAndReadsAverage() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"average\":0.42}");
        });

        double average = newClient().averageMetric(MetricQueryRequest.builder()
                .name("system-load-per-core")
                .from("2026-11-06T00:00:00Z")
                .build());

        assertEquals("average", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"name\":\"system-load-per-core\"",
                "\"from\":\"2026-11-06T00:00:00Z\"");
        assertFalse(received.get().body().contains("limit"));
        assertEquals(0.42, average);
    }

    @Test
    void saveDashboardSendsNameAndLayoutKeepingItsNulls() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200,
                    "{\"name\":\"ops\",\"userId\":\"alice\",\"modified\":\"2026-11-06T10:00:00Z\"}");
        });
        Map<String, Object> layout = new LinkedHashMap<>();
        layout.put("panels", List.of(Map.of("metric", "system-load-per-core")));
        layout.put("title", null);

        SaveDashboardResponse saved = newClient().saveDashboard("ops", layout);

        assertEquals("save-dashboard", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"name\":\"ops\"",
                "\"dashboard\":{\"panels\":[{\"metric\":\"system-load-per-core\"}],\"title\":null}");
        assertEquals("ops", saved.name());
        assertEquals("alice", saved.userId());
        assertEquals("2026-11-06T10:00:00Z", saved.modified());
    }

    @Test
    void getDashboardParsesLayoutAsPlainJava() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"name\":\"ops\",\"dashboard\":{\"columns\":2,"
                    + "\"panels\":[{\"metric\":\"system-load-per-core\"}],\"title\":null},"
                    + "\"created\":\"2026-11-01T09:00:00Z\",\"modified\":\"2026-11-06T10:00:00Z\"}");
        });

        Dashboard dashboard = newClient().getDashboard("ops");

        assertEquals("get-dashboard", received.get().header("x-euclid-action"));
        assertEquals("{\"name\":\"ops\"}", received.get().body());
        assertEquals("ops", dashboard.name());
        assertEquals(2, dashboard.dashboard().get("columns"));
        assertEquals(List.of(Map.of("metric", "system-load-per-core")), dashboard.dashboard().get("panels"));
        assertTrue(dashboard.dashboard().containsKey("title"));
        assertNull(dashboard.dashboard().get("title"));
        assertEquals("2026-11-01T09:00:00Z", dashboard.created());
        assertEquals("2026-11-06T10:00:00Z", dashboard.modified());
    }

    @Test
    void listDashboardsParsesOwnerAndSummaries() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"userId\":\"alice\",\"items\":[{\"name\":\"ops\","
                    + "\"created\":\"2026-11-01T09:00:00Z\",\"modified\":\"2026-11-06T10:00:00Z\"}]}");
        });

        ListDashboardsResponse response = newClient().listDashboards();

        assertEquals("list-dashboards", received.get().header("x-euclid-action"));
        assertEquals("alice", response.userId());
        assertEquals(1, response.dashboards().size());
        assertEquals("ops", response.dashboards().get(0).name());
        assertEquals("2026-11-01T09:00:00Z", response.dashboards().get(0).created());
        assertEquals("2026-11-06T10:00:00Z", response.dashboards().get(0).modified());
    }

    @Test
    void deleteDashboardSendsNameAndSurfacesNotFound() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 404, "{\"error\":\"No dashboard called 'ops'\"}");
        });

        EuclidEmo emo = newClient();
        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> emo.deleteDashboard("ops"));

        assertEquals("delete-dashboard", received.get().header("x-euclid-action"));
        assertEquals("{\"name\":\"ops\"}", received.get().body());
        assertEquals("delete-dashboard", exception.action());
        assertEquals(404, exception.statusCode());
    }

    @Test
    void deleteDashboardAcceptsAnEmptyOkResponse() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, "");
        });

        newClient().deleteDashboard("ops");
    }

    @Test
    void nonAdministratorIsForbiddenFromSeriesQueries() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 403, "{\"error\":\"Administrator privileges required\"}");
        });

        EuclidEmo emo = newClient();
        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> emo.averageMetric(MetricQueryRequest.builder().name("x").build()));

        assertEquals("emo", exception.service());
        assertEquals("average", exception.action());
        assertEquals(403, exception.statusCode());
    }

    private EuclidEmo newClient() {
        return new EuclidEmo(baseUrl(), "test-token", "eu-central-1", "863459426936", "alice", null, null, null, null);
    }

    private static void assertBodyContains(String body, String... fragments) {
        for (String fragment : fragments) {
            assertTrue(body.contains(fragment), "expected body to contain " + fragment + " but was " + body);
        }
    }

    private static SignableRequest captureRequest(HttpExchange exchange) throws IOException {
        SignableRequest req = new SignableRequest(exchange.getRequestMethod(), exchange.getRequestURI().toString());
        Headers requestHeaders = exchange.getRequestHeaders();
        for (String name : CAPTURED_HEADERS) {
            String value = requestHeaders.getFirst(name);
            if (value != null) {
                req.header(name, value);
            }
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
        // A zero-length response is announced as -1, which is how HttpServer says "no body".
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        try (var os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
