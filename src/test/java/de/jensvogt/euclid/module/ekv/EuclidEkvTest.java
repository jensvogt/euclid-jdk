package de.jensvogt.euclid.module.ekv;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.jensvogt.euclid.auth.SigV4;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.dto.ekv.CreateTableRequest;
import de.jensvogt.euclid.dto.ekv.ListTablesResponse;
import de.jensvogt.euclid.dto.ekv.QueryRequest;
import de.jensvogt.euclid.dto.ekv.QueryResponse;
import de.jensvogt.euclid.dto.ekv.ScanResponse;
import de.jensvogt.euclid.dto.ekv.model.Item;
import de.jensvogt.euclid.dto.ekv.model.KeyType;
import de.jensvogt.euclid.dto.ekv.model.SortOperator;
import de.jensvogt.euclid.dto.ekv.model.TableDescription;
import de.jensvogt.euclid.exception.EuclidServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms EuclidEkv authenticates the way it claims to (SigV4-signed when an access key is
 * configured, bearer token otherwise, mirroring euclid-cli's HttpClient.cpp), routes every table
 * and item action to the right request with a correctly-shaped body, parses the corresponding
 * response, and surfaces non-2xx responses as {@link EuclidServiceException}.
 * <p>
 * The item cases carry most of the weight here. EKV fixes no schema for what an item holds, so the
 * client's job is to change nothing on the way through: a number stays a number, a null attribute
 * stays a present-and-empty attribute rather than being dropped, and the two timestamps the server
 * adds do not come back as attributes an unsuspecting read-modify-write would then store.
 */
class EuclidEkvTest {

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
    void createTableSignsWithSigV4WhenAccessKeyConfigured() throws Exception {
        String accessKeyId = "AKIDEXAMPLE";
        String secretAccessKey = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";

        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 201, tableJson(0));
        });

        new EuclidEkv(baseUrl(), "unused-token", "eu-central-1", "863459426936", "alice",
                accessKeyId, secretAccessKey, null, null)
                .createTable("suppliers", "supplierId");

        SignableRequest req = received.get();
        assertTrue(req.header("authorization").startsWith("AWS4-HMAC-SHA256 "));

        Optional<SigV4.VerifyResult> result = SigV4.verify(req,
                id -> id.equals(accessKeyId) ? Optional.of(secretAccessKey) : Optional.empty());
        assertTrue(result.isPresent(), "server-side verification of the client's own signature must succeed");
        assertEquals(accessKeyId, result.get().accessKeyId());
        assertEquals("ekv", req.header("x-euclid-target"));
    }

    @Test
    void createTableUsesTheBearerTokenWithoutAnAccessKey() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 201, tableJson(0));
        });

        newClient().createTable("suppliers", "supplierId");

        assertEquals("Bearer test-token", received.get().header("authorization"));
    }

    @Test
    void createTableSendsTheKeyAndParsesTheDescription() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 201, tableJson(0));
        });

        TableDescription table = newClient().createTable("suppliers", "supplierId");

        assertEquals("create-table", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"name\":\"suppliers\"", "\"partitionKey\":\"supplierId\"",
                "\"partitionKeyType\":\"string\"");
        assertEquals("suppliers", table.name());
        assertEquals("ern:ekv:table/suppliers", table.ern());
        assertEquals("supplierId", table.partitionKey());
        assertEquals(KeyType.STRING, table.partitionKeyType());
        assertEquals(0, table.itemCount());
    }

    // A table without a sort key reports an empty one; "this table is not sorted" must not read
    // back as "this table is sorted by a string".
    @Test
    void aTableWithoutASortKeySaysSo() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 201, tableJson(0));
        });

        TableDescription table = newClient().createTable("suppliers", "supplierId");

        assertNull(table.sortKey());
        assertNull(table.sortKeyType());
        assertFalse(table.hasSortKey());
    }

    @Test
    void createTableCanDeclareATypedSortKey() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 201, sortedTableJson(0));
        });

        TableDescription table = newClient().createTable(CreateTableRequest.builder().name("deliveries")
                .partitionKey("supplierId").sortKey("sequence").sortKeyType(KeyType.NUMBER).build());

        assertBodyContains(received.get().body(), "\"sortKey\":\"sequence\"", "\"sortKeyType\":\"number\"");
        assertEquals("sequence", table.sortKey());
        assertEquals(KeyType.NUMBER, table.sortKeyType());
        assertTrue(table.hasSortKey());
    }

    @Test
    void createTableSurfacesAConflictForANameThatExists() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 409, "{\"error\":\"Table exists already: suppliers\"}");
        });

        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> newClient().createTable("suppliers", "supplierId"));

        assertEquals("ekv", exception.service());
        assertEquals("create-table", exception.action());
        assertEquals(409, exception.statusCode());
    }

    @Test
    void describeTableSendsTheNameAndParsesTheItemCount() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, tableJson(42));
        });

        TableDescription table = newClient().describeTable("suppliers");

        assertEquals("describe-table", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"name\":\"suppliers\"");
        assertEquals(42, table.itemCount());
    }

    @Test
    void describeTableSurfacesATableThatDoesNotExist() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 404, "{\"error\":\"Table does not exist: nope\"}");
        });

        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> newClient().describeTable("nope"));

        assertEquals("describe-table", exception.action());
        assertEquals(404, exception.statusCode());
    }

    @Test
    void listTablesParsesThePageAndTheTotal() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"tables\":[" + tableJson(3) + "," + sortedTableJson(9) + "],\"total\":7}");
        });

        ListTablesResponse response = newClient().listTables("sup", 5, 1, "name", "desc");

        assertEquals("list-tables", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"prefix\":\"sup\"", "\"pageSize\":5", "\"pageIndex\":1",
                "\"sortDirection\":\"desc\"");
        assertEquals(2, response.tables().size());
        assertEquals("suppliers", response.tables().getFirst().name());
        assertEquals(9, response.tables().get(1).itemCount());
        assertEquals(7, response.total());
    }

    @Test
    void listTablesToleratesAnEmptyResult() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, "{\"total\":0}");
        });

        assertTrue(newClient().listTables().tables().isEmpty());
    }

    @Test
    void deleteTableReportsWhatWentWithIt() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"deletedItems\":17}");
        });

        long deleted = newClient().deleteTable("suppliers");

        assertEquals("delete-table", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"name\":\"suppliers\"");
        assertEquals(17, deleted);
    }

    @Test
    void putItemSendsTheTableAndTheItem() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, itemJson());
        });

        newClient().putItem("suppliers", Map.of("supplierId", "4711", "name", "Acme"));

        assertEquals("put-item", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"table\":\"suppliers\"", "\"supplierId\":\"4711\"",
                "\"name\":\"Acme\"");
    }

    // An attribute that is present and empty is not the same as one that is absent, and the store
    // holds the difference - so a null has to reach the server rather than being serialized away.
    @Test
    void putItemKeepsANullAttribute() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, itemJson());
        });

        Map<String, Object> item = new HashMap<>();
        item.put("supplierId", "4711");
        item.put("discontinued", null);
        newClient().putItem("suppliers", item);

        assertBodyContains(received.get().body(), "\"discontinued\":null");
    }

    // The types are the whole point: an item is stored as written, not as whatever a client
    // guessed on the way through.
    @Test
    void anItemKeepsItsTypesOnTheWayBack() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, "{\"supplierId\":\"4711\",\"orders\":3,\"rating\":4.5,\"active\":true,"
                    + "\"tags\":[\"a\",\"b\"],\"address\":{\"city\":\"Bonn\"},\"note\":null,"
                    + "\"_created\":\"2026-01-01\",\"_modified\":\"2026-01-02\"}");
        });

        Item item = newClient().getItem("suppliers", Map.of("supplierId", "4711"));

        assertEquals(3, item.get("orders"));
        assertInstanceOf(Integer.class, item.get("orders"), "a whole number must not arrive as a double");
        assertEquals(4.5, item.get("rating"));
        assertEquals(Boolean.TRUE, item.get("active"));
        assertEquals(List.of("a", "b"), item.get("tags"));
        assertEquals(Map.of("city", "Bonn"), item.get("address"));
        assertNull(item.get("note"));
        assertTrue(item.attributes().containsKey("note"), "a stored null is present, not absent");
    }

    // The timestamps travel as attributes, but an item read, changed and written back would then
    // store them - put-item replaces rather than merges, so that would stick.
    @Test
    void theTimestampsAreLiftedOutOfTheAttributes() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, itemJson());
        });

        Item item = newClient().putItem("suppliers", Map.of("supplierId", "4711"));

        assertEquals("2026-01-01T00:00:00Z", item.created());
        assertEquals("2026-01-02T00:00:00Z", item.modified());
        assertFalse(item.attributes().containsKey("_created"), "the timestamps must not read back as attributes");
        assertFalse(item.attributes().containsKey("_modified"), "the timestamps must not read back as attributes");
        assertEquals(Map.of("supplierId", "4711", "name", "Acme"), item.attributes());
    }

    @Test
    void getItemSendsTheKey() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, itemJson());
        });

        Item item = newClient().getItem("suppliers", Map.of("supplierId", "4711"));

        assertEquals("get-item", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"table\":\"suppliers\"", "\"key\":{\"supplierId\":\"4711\"}");
        assertEquals("4711", item.get("supplierId"));
    }

    // "There is no such item" and "here is an item with nothing in it" are different, and the
    // server says so with a 404 rather than an empty answer.
    @Test
    void getItemSurfacesAnItemThatIsNotThere() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 404, "{\"error\":\"No such item\"}");
        });

        EuclidServiceException exception = assertThrows(EuclidServiceException.class,
                () -> newClient().getItem("suppliers", Map.of("supplierId", "4711")));

        assertEquals("get-item", exception.action());
        assertEquals(404, exception.statusCode());
    }

    @Test
    void deleteItemReportsWhetherThereWasOne() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"deleted\":true}");
        });

        assertTrue(newClient().deleteItem("suppliers", Map.of("supplierId", "4711")));
        assertEquals("delete-item", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"key\":{\"supplierId\":\"4711\"}");
    }

    @Test
    void deleteItemReportsAKeyThatMatchedNothing() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, "{\"deleted\":false}");
        });

        assertFalse(newClient().deleteItem("suppliers", Map.of("supplierId", "4711")));
    }

    @Test
    void queryTakesTheWholePartitionByDefault() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"items\":[" + itemJson() + "],\"count\":1}");
        });

        QueryResponse response = newClient().query("deliveries", "4711");

        assertEquals("query", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"partitionKey\":\"4711\"", "\"sortOperator\":\"\"",
                "\"forward\":true");
        assertEquals(1, response.count());
        assertEquals("4711", response.items().getFirst().get("supplierId"));
    }

    @Test
    void queryNarrowsByTheSortKey() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"items\":[],\"count\":0}");
        });

        newClient().query("deliveries", "4711", SortOperator.BEGINS_WITH, "2026-09");

        assertBodyContains(received.get().body(), "\"sortOperator\":\"begins-with\"", "\"sortValue\":\"2026-09\"");
    }

    // A number key has to stay a number on the wire, or the server refuses it as a string where it
    // wanted a number - which is exactly the confusion a typed key exists to prevent.
    @Test
    void queryKeepsANumericKeyANumber() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"items\":[],\"count\":0}");
        });

        newClient().query(QueryRequest.builder().table("deliveries").partitionKey(4711)
                .sortOperator(SortOperator.BETWEEN).sortValue(10).sortUpper(20).forward(false)
                .pageSize(50).pageIndex(2).build());

        assertBodyContains(received.get().body(), "\"partitionKey\":4711", "\"sortOperator\":\"between\"",
                "\"sortValue\":10", "\"sortUpper\":20", "\"forward\":false", "\"pageSize\":50", "\"pageIndex\":2");
    }

    @Test
    void queryToleratesAnEmptyResult() throws Exception {
        server = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, "{\"count\":0}");
        });

        assertTrue(newClient().query("deliveries", "4711").items().isEmpty());
    }

    @Test
    void scanParsesThePageAndTheTotal() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"items\":[" + itemJson() + "],\"count\":1,\"total\":98}");
        });

        ScanResponse response = newClient().scan("suppliers", 25, 3);

        assertEquals("scan", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"table\":\"suppliers\"", "\"pageSize\":25", "\"pageIndex\":3");
        assertEquals(1, response.items().size());
        assertEquals(1, response.count());
        assertEquals(98, response.total());
    }

    @Test
    void scanReadsTheWholeTableWhenUnpaged() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"items\":[],\"count\":0,\"total\":0}");
        });

        newClient().scan("suppliers");

        assertBodyContains(received.get().body(), "\"pageSize\":0", "\"pageIndex\":0");
    }

    @Test
    void namespaceIsSentWhenTheSessionIsScoped() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"total\":0}");
        });

        new EuclidEkv(baseUrl(), "test-token", "eu-central-1", "863459426936", "alice", null, null, null, "prod")
                .listTables();

        assertEquals("prod", received.get().header("x-euclid-namespace"));
    }

    @Test
    void namespaceHeaderIsOmittedWhenUnset() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"total\":0}");
        });

        newClient().listTables();

        assertEquals("", received.get().header("x-euclid-namespace"));
    }

    private static String tableJson(int itemCount) {
        return "{\"name\":\"suppliers\",\"ern\":\"ern:ekv:table/suppliers\",\"partitionKey\":\"supplierId\","
                + "\"partitionKeyType\":\"string\",\"sortKey\":\"\",\"sortKeyType\":\"\",\"itemCount\":" + itemCount
                + ",\"created\":\"2026-01-01\",\"modified\":\"2026-01-02\"}";
    }

    private static String sortedTableJson(int itemCount) {
        return "{\"name\":\"deliveries\",\"ern\":\"ern:ekv:table/deliveries\",\"partitionKey\":\"supplierId\","
                + "\"partitionKeyType\":\"string\",\"sortKey\":\"sequence\",\"sortKeyType\":\"number\","
                + "\"itemCount\":" + itemCount + ",\"created\":\"2026-01-01\",\"modified\":\"2026-01-02\"}";
    }

    private static String itemJson() {
        return "{\"supplierId\":\"4711\",\"name\":\"Acme\",\"_created\":\"2026-01-01T00:00:00Z\","
                + "\"_modified\":\"2026-01-02T00:00:00Z\"}";
    }

    private EuclidEkv newClient() {
        return new EuclidEkv(baseUrl(), "test-token", "eu-central-1", "863459426936", "alice", null, null, null, null);
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
