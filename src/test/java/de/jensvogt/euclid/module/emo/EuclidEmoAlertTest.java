package de.jensvogt.euclid.module.emo;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.dto.emo.CreateAlertRuleRequest;
import de.jensvogt.euclid.dto.emo.ListAlertHistoryRequest;
import de.jensvogt.euclid.dto.emo.ListAlertsRequest;
import de.jensvogt.euclid.dto.emo.UpdateAlertRuleRequest;
import de.jensvogt.euclid.dto.emo.model.Alert;
import de.jensvogt.euclid.dto.emo.model.AlertRule;
import de.jensvogt.euclid.dto.emo.model.AlertTransition;
import de.jensvogt.euclid.dto.emo.model.MetricInfo;
import de.jensvogt.euclid.exception.EuclidServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms EuclidEmo routes the metric listing and every alert rule action to the right request
 * with a correctly-shaped body - in particular that unset fields are left out and a removed
 * threshold is sent as an explicit null, which is how EmoServer.cpp tells the two apart - parses
 * the corresponding response, and surfaces non-2xx responses as {@link EuclidServiceException}.
 */
class EuclidEmoAlertTest {

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
    void listMetricsSendsHoursAndPrefixAndParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"items\":[{\"name\":\"system-load-per-core\",\"type\":\"GAUGE\","
                    + "\"series\":3,\"labelNames\":[\"instance\"],\"lastSeen\":\"2026-11-06T10:00:00Z\"}],\"total\":1}");
        });

        List<MetricInfo> metrics = newClient().listMetrics("system-", 48);

        assertEquals("list-metrics", received.get().header("x-euclid-action"));
        assertEquals("emo", received.get().header("x-euclid-target"));
        assertBodyContains(received.get().body(), "\"hours\":48", "\"prefix\":\"system-\"");
        assertEquals(1, metrics.size());
        MetricInfo metric = metrics.get(0);
        assertEquals("system-load-per-core", metric.name());
        assertEquals("GAUGE", metric.type());
        assertEquals(3, metric.series());
        assertEquals(List.of("instance"), metric.labelNames());
        assertEquals("2026-11-06T10:00:00Z", metric.lastSeen());
    }

    @Test
    void listMetricsWithoutArgumentsAsksForTheLastDayAndNoPrefix() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"items\":[],\"total\":0}");
        });

        assertTrue(newClient().listMetrics().isEmpty());

        assertEquals("{\"hours\":24}", received.get().body());
    }

    @Test
    void createAlertRuleSendsOnlyWhatWasSetAndParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, ruleJson());
        });

        AlertRule rule = newClient().createAlertRule(CreateAlertRuleRequest.builder()
                .ruleId("load-high").name("system-load-per-core").labels(Map.of("instance", "web-1"))
                .warning(0.8).critical(1.5).forPeriods(3L).enabled(false).build());

        assertEquals("create-alert-rule", received.get().header("x-euclid-action"));
        assertEquals("emo", received.get().header("x-euclid-target"));
        String body = received.get().body();
        assertBodyContains(body, "\"ruleId\":\"load-high\"", "\"name\":\"system-load-per-core\"",
                "\"labels\":{\"instance\":\"web-1\"}", "\"warning\":0.8", "\"critical\":1.5",
                "\"forPeriods\":3", "\"enabled\":false");
        assertFalse(body.contains("statistic"), "unset fields must be left to the server's defaults: " + body);
        assertFalse(body.contains("clearPeriods"), "unset fields must be left to the server's defaults: " + body);
        assertFalse(body.contains("null"), "nothing unset is sent as null: " + body);

        assertEquals("load-high", rule.ruleId());
        assertEquals("system-load-per-core", rule.name());
        assertEquals(Map.of("instance", "web-1"), rule.labels());
        assertEquals("RAW", rule.resolution());
        assertEquals("value", rule.statistic());
        assertEquals("above", rule.comparison());
        assertEquals(0.8, rule.warning());
        assertNull(rule.critical(), "a null threshold is no threshold, not one at zero");
        assertEquals(3, rule.forPeriods());
        assertEquals(1, rule.clearPeriods());
        assertEquals(0, rule.noDataAfterPeriods());
        assertFalse(rule.enabled());
        assertEquals("CPU", rule.description());
        assertEquals("863459426936", rule.accountId());
        assertEquals("", rule.namespace());
        assertEquals("2026-11-06T10:00:00Z", rule.created());
    }

    @Test
    void createAlertRuleConflictSurfacesTheServersMessage() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 409, "{\"error\":\"An alert rule 'load-high' already exists\"}");
        });

        EuclidEmo emo = newClient();
        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> emo.createAlertRule(CreateAlertRuleRequest.builder()
                        .ruleId("load-high").name("system-load-per-core").critical(1.5).build()));

        assertEquals("emo", exception.service());
        assertEquals("create-alert-rule", exception.action());
        assertEquals(409, exception.statusCode());
        assertTrue(exception.responseBody().contains("already exists"));
    }

    @Test
    void updateAlertRuleSendsRemovedThresholdAsNullAndLeavesTheRestOut() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, ruleJson());
        });

        AlertRule rule = newClient().updateAlertRule(UpdateAlertRuleRequest.builder()
                .ruleId("load-high").critical(2.0).removeCritical().removeWarning().warning(0.8)
                .removeCritical().labels(Map.of()).build());

        assertEquals("update-alert-rule", received.get().header("x-euclid-action"));
        String body = received.get().body();
        assertBodyContains(body, "\"ruleId\":\"load-high\"", "\"critical\":null", "\"warning\":0.8",
                "\"labels\":{}");
        assertFalse(body.contains("removeWarning"), "the removal flags are not wire fields: " + body);
        assertFalse(body.contains("removeCritical"), "the removal flags are not wire fields: " + body);
        assertFalse(body.contains("enabled"), "unset fields must not be touched: " + body);
        assertFalse(body.contains("description"), "unset fields must not be touched: " + body);
        assertEquals("load-high", rule.ruleId());
    }

    @Test
    void updateAlertRuleOfUnknownRuleIsNotFound() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 404, "{\"error\":\"No alert rule 'missing'\"}");
        });

        EuclidEmo emo = newClient();
        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> emo.updateAlertRule(UpdateAlertRuleRequest.builder().ruleId("missing").enabled(true).build()));

        assertEquals("update-alert-rule", exception.action());
        assertEquals(404, exception.statusCode());
    }

    @Test
    void deleteAlertRuleSendsRuleIdAndAcceptsAnEmptyResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "");
        });

        newClient().deleteAlertRule("load-high");

        assertEquals("delete-alert-rule", received.get().header("x-euclid-action"));
        assertEquals("{\"ruleId\":\"load-high\"}", received.get().body());
    }

    @Test
    void listAlertRulesFiltersByRuleIdAndParsesItems() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"items\":[" + ruleJson() + "]}");
        });

        List<AlertRule> rules = newClient().listAlertRules("load-high");

        assertEquals("list-alert-rules", received.get().header("x-euclid-action"));
        assertEquals("{\"ruleId\":\"load-high\"}", received.get().body());
        assertEquals(1, rules.size());
        assertEquals("load-high", rules.get(0).ruleId());
    }

    @Test
    void listAlertRulesWithoutFilterSendsAnEmptyObject() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"items\":[]}");
        });

        assertTrue(newClient().listAlertRules().isEmpty());

        assertEquals("{}", received.get().body());
    }

    @Test
    void listAlertsSendsFiltersAndParsesItems() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"items\":[{\"ruleId\":\"load-high\",\"name\":\"system-load-per-core\","
                    + "\"labels\":{\"instance\":\"web-1\"},\"state\":\"CRITICAL\",\"severity\":\"critical\","
                    + "\"since\":\"2026-11-06T09:55:00Z\",\"value\":1.9,\"lastSeen\":\"2026-11-06T10:00:00Z\","
                    + "\"statistic\":\"value\",\"comparison\":\"above\",\"warning\":null,\"critical\":1.5,"
                    + "\"description\":\"CPU\",\"namespace\":\"\"}]}");
        });

        List<Alert> alerts = newClient().listAlerts(ListAlertsRequest.builder()
                .ruleId("load-high").active(true).build());

        assertEquals("list-alerts", received.get().header("x-euclid-action"));
        assertEquals("{\"ruleId\":\"load-high\",\"active\":true}", received.get().body());
        assertEquals(1, alerts.size());
        Alert alert = alerts.get(0);
        assertEquals("load-high", alert.ruleId());
        assertEquals(Map.of("instance", "web-1"), alert.labels());
        assertEquals("CRITICAL", alert.state());
        assertEquals("critical", alert.severity());
        assertEquals(1.9, alert.value());
        assertNull(alert.warning());
        assertEquals(1.5, alert.critical());
        assertEquals("2026-11-06T09:55:00Z", alert.since());
    }

    @Test
    void listAlertHistorySendsFiltersAndParsesItems() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"items\":[{\"ruleId\":\"load-high\",\"name\":\"system-load-per-core\","
                    + "\"labels\":{\"instance\":\"web-1\"},\"previous\":\"CRITICAL\",\"state\":\"OK\","
                    + "\"severity\":\"ok\",\"value\":1.9,\"threshold\":null,\"statistic\":\"value\","
                    + "\"reason\":\"rule-deleted\",\"timestamp\":\"2026-11-06T10:05:00Z\","
                    + "\"accountId\":\"863459426936\",\"namespace\":\"\"}]}");
        });

        List<AlertTransition> history = newClient().listAlertHistory(ListAlertHistoryRequest.builder()
                .ruleId("load-high").from("2026-11-06T00:00:00Z").limit(10L).build());

        assertEquals("list-alert-history", received.get().header("x-euclid-action"));
        assertEquals("{\"ruleId\":\"load-high\",\"from\":\"2026-11-06T00:00:00Z\",\"limit\":10}",
                received.get().body());
        assertEquals(1, history.size());
        AlertTransition transition = history.get(0);
        assertEquals("CRITICAL", transition.previous());
        assertEquals("OK", transition.state());
        assertEquals("ok", transition.severity());
        assertNull(transition.threshold());
        assertEquals("rule-deleted", transition.reason());
        assertEquals("2026-11-06T10:05:00Z", transition.timestamp());
        assertEquals("863459426936", transition.accountId());
    }

    @Test
    void nonAdministratorIsForbidden() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 403, "{\"error\":\"Administrator privileges required\"}");
        });

        EuclidEmo emo = newClient();
        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> emo.listAlerts(ListAlertsRequest.builder().build()));

        assertEquals("emo", exception.service());
        assertEquals("list-alerts", exception.action());
        assertEquals(403, exception.statusCode());
        assertTrue(exception.responseBody().contains("Administrator privileges required"));
    }

    private static String ruleJson() {
        return "{\"ruleId\":\"load-high\",\"name\":\"system-load-per-core\",\"labels\":{\"instance\":\"web-1\"},"
                + "\"resolution\":\"RAW\",\"statistic\":\"value\",\"comparison\":\"above\",\"warning\":0.8,"
                + "\"critical\":null,\"forPeriods\":3,\"clearPeriods\":1,\"noDataAfterPeriods\":0,\"enabled\":false,"
                + "\"description\":\"CPU\",\"accountId\":\"863459426936\",\"namespace\":\"\","
                + "\"created\":\"2026-11-06T10:00:00Z\",\"modified\":\"2026-11-06T10:00:00Z\"}";
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
        // A zero-length response is announced as -1, which is how HttpServer says "no body".
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        try (var os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
