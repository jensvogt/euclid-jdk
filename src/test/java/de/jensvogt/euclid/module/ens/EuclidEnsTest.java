package de.jensvogt.euclid.module.ens;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.jensvogt.euclid.auth.SigV4;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.dto.com.Variant;
import de.jensvogt.euclid.dto.ens.CreateTopicResponse;
import de.jensvogt.euclid.dto.ens.GetMessageAttributeResponse;
import de.jensvogt.euclid.dto.ens.GetMessageCountResponse;
import de.jensvogt.euclid.dto.ens.GetTopicErnResponse;
import de.jensvogt.euclid.dto.ens.GetTopicMetadataResponse;
import de.jensvogt.euclid.dto.ens.ListMessagesResponse;
import de.jensvogt.euclid.dto.ens.ListSubscriptionsResponse;
import de.jensvogt.euclid.dto.ens.ListTopicsResponse;
import de.jensvogt.euclid.dto.ens.PublishMessageResponse;
import de.jensvogt.euclid.dto.ens.SubscribeResponse;
import de.jensvogt.euclid.dto.ens.model.Topic;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms EuclidEns authenticates the way it claims to (SigV4-signed when an access key is
 * configured, bearer token otherwise, mirroring euclid-cli's HttpClient.cpp), routes every
 * topic/message/subscription action to the right request with a correctly-shaped body, parses
 * the corresponding response, and surfaces non-2xx responses as {@link EuclidServiceException}.
 */
class EuclidEnsTest {

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
    void publishMessageSignsWithSigV4WhenAccessKeyConfigured() throws Exception {
        String accessKeyId = "AKIDEXAMPLE";
        String secretAccessKey = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";

        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"messageId\":\"msg-1\"}");
        });

        EuclidEns ens = new EuclidEns(baseUrl(), "unused-token", "eu-central-1", "863459426936", "alice",
                accessKeyId, secretAccessKey, null, null);
        ens.publishMessage("ern:ens:eu-central-1:863459426936:topic/test", "hello");

        SignableRequest req = received.get();
        assertTrue(req.header("authorization").startsWith("AWS4-HMAC-SHA256 "));

        Optional<SigV4.VerifyResult> result = SigV4.verify(req,
                id -> id.equals(accessKeyId) ? Optional.of(secretAccessKey) : Optional.empty());
        assertTrue(result.isPresent(), "server-side verification of the client's own signature must succeed");
        assertEquals(accessKeyId, result.get().accessKeyId());
    }

    @Test
    void publishMessageUsesBearerTokenWhenNoAccessKeyConfigured() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"messageId\":\"msg-1\"}");
        });

        EuclidEns ens = new EuclidEns(baseUrl(), "my-jwt-token", "eu-central-1", "863459426936", "alice",
                null, null, null, null);
        ens.publishMessage("ern:ens:eu-central-1:863459426936:topic/test", "hello");

        assertEquals("Bearer my-jwt-token", received.get().header("authorization"));
    }

    @Test
    void createTopicUsesDefaultsAndParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"name\":\"orders\",\"ern\":\"ern:ens:eu-central-1:863459426936:topic/orders\"}");
        });

        CreateTopicResponse response = newClient().createTopic("orders");

        assertEquals("create-topic", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"name\":\"orders\"", "\"maxMessageLength\":1048576");
        assertEquals("orders", response.name());
        assertEquals("ern:ens:eu-central-1:863459426936:topic/orders", response.ern());
    }

    @Test
    void createTopicWithExplicitMaxMessageLength() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"name\":\"orders\",\"ern\":\"ern:orders\"}");
        });

        newClient().createTopic("orders", 2048);

        assertBodyContains(received.get().body(), "\"maxMessageLength\":2048");
    }

    @Test
    void listTopicsUsesDefaultsAndParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"topics\":[{\"name\":\"orders\",\"owner\":\"alice\","
                    + "\"ern\":\"ern:ens:eu-central-1:863459426936:topic/orders\",\"tags\":{\"env\":\"prod\"},"
                    + "\"size\":100,\"messages\":3,\"maxMessageLength\":1048576,"
                    + "\"created\":\"2026-01-01\",\"modified\":\"2026-01-02\"}],\"total\":1}");
        });

        ListTopicsResponse response = newClient().listTopics();
        List<Topic> topics = response.topics();
        assertEquals(1, response.total(), "the server's total must survive rather than be dropped");

        assertEquals("list-topics", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"prefix\":\"\"", "\"pageSize\":10", "\"pageIndex\":0",
                "\"sortColumn\":\"name\"");
        assertEquals(1, topics.size());
        Topic topic = topics.getFirst();
        assertEquals("orders", topic.name());
        assertEquals("prod", topic.tags().get("env"));
        assertEquals(3, topic.messages());
    }

    @Test
    void listTopicsWithExplicitParameters() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"topics\":[]}");
        });

        List<Topic> topics = newClient().listTopics("ord", 25, 2, "created").topics();

        assertBodyContains(received.get().body(), "\"prefix\":\"ord\"", "\"pageSize\":25", "\"pageIndex\":2",
                "\"sortColumn\":\"created\"");
        assertTrue(topics.isEmpty());
    }

    @Test
    void getTopicErnParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"name\":\"orders\",\"ern\":\"ern:ens:eu-central-1:863459426936:topic/orders\"}");
        });

        GetTopicErnResponse response = newClient().getTopicErn("orders");

        assertEquals("get-topic-ern", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"name\":\"orders\"");
        assertEquals("ern:ens:eu-central-1:863459426936:topic/orders", response.ern());
    }

    @Test
    void getTopicMetadataParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"region\":\"eu-central-1\",\"accountId\":\"863459426936\","
                    + "\"owner\":\"alice\",\"nameSpace\":\"prod\",\"name\":\"orders\",\"ern\":\"topic-ern\","
                    + "\"size\":42,\"messages\":7}");
        });

        GetTopicMetadataResponse response = newClient().getTopicMetadata("topic-ern");

        assertEquals("get-topic-metadata", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"ern\":\"topic-ern\"");
        assertEquals("orders", response.name());
        assertEquals(42, response.size());
        assertEquals(7, response.messages());
    }

    @Test
    void deleteTopicSendsErn() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newClient().deleteTopic("topic-ern");

        assertEquals("delete-topic", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"ern\":\"topic-ern\"");
    }

    @Test
    void purgeTopicSendsErn() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newClient().purgeTopic("topic-ern");

        assertEquals("purge-topic", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"ern\":\"topic-ern\"");
    }

    @Test
    void purgeAllTopicsUsesInstanceRegionAndAccountByDefault() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newClient().purgeAllTopics();

        assertEquals("purge-all-topics", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"region\":\"eu-central-1\"", "\"accountId\":\"863459426936\"",
                "\"nameSpace\":\"\"");
    }

    @Test
    void purgeAllTopicsWithExplicitRegionAndAccount() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newClient().purgeAllTopics("us-east-1", "111111111111");

        assertBodyContains(received.get().body(), "\"region\":\"us-east-1\"", "\"accountId\":\"111111111111\"",
                "\"nameSpace\":\"\"");
    }

    @Test
    void purgeAllTopicsWithExplicitNamespace() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newClient().purgeAllTopics("us-east-1", "111111111111", "prod");

        assertBodyContains(received.get().body(), "\"nameSpace\":\"prod\"");
    }

    @Test
    void publishMessageWithDefaultsSendsEmptyAttributes() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"messageId\":\"msg-1\",\"md5Body\":\"abc\",\"md5Attributes\":\"def\"}");
        });

        PublishMessageResponse response = newClient().publishMessage("topic-ern", "hello");

        assertEquals("publish-message", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"ern\":\"topic-ern\"", "\"body\":\"hello\"", "\"attributes\":{}");
        assertEquals("msg-1", response.messageId());
        assertEquals("abc", response.md5Body());
        assertEquals("def", response.md5Attributes());
    }

    @Test
    void publishMessageWithAttributes() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"messageId\":\"msg-1\"}");
        });

        newClient().publishMessage("topic-ern", "hello", Map.of("count", new Variant("long", 5)));

        assertBodyContains(received.get().body(), "\"count\":{\"type\":\"long\",\"value\":5}");
    }

    @Test
    void listMessagesUsesDefaultsAndParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"messages\":[" + messageJson("msg-1") + "],\"total\":1}");
        });

        ListMessagesResponse response = newClient().listMessages("topic-ern");

        assertEquals("list-messages", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"topicErn\":\"topic-ern\"", "\"pageSize\":10",
                "\"pageIndex\":0", "\"sortColumn\":\"created\"");
        assertEquals(1, response.total());
        assertEquals("msg-1", response.messages().getFirst().messageId());
    }

    @Test
    void listMessagesWithExplicitParameters() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"messages\":[],\"total\":0}");
        });

        newClient().listMessages("topic-ern", 25, 2, "messageId");

        assertBodyContains(received.get().body(), "\"pageSize\":25", "\"pageIndex\":2", "\"sortColumn\":\"messageId\"");
    }

    @Test
    void getMessageCountParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"ern\":\"topic-ern\",\"available\":3,\"send\":10,\"resend\":2}");
        });

        GetMessageCountResponse response = newClient().getMessageCount("topic-ern");

        assertEquals("get-message-count", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"ern\":\"topic-ern\"");
        assertEquals(3, response.available());
        assertEquals(10, response.send());
        assertEquals(2, response.resend());
    }

    @Test
    void getMessageAttributeParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"messageId\":\"msg-1\",\"key\":\"count\","
                    + "\"value\":{\"type\":\"long\",\"value\":5}}");
        });

        GetMessageAttributeResponse response = newClient().getMessageAttribute("msg-1", "count");

        assertEquals("get-message-attribute", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"messageId\":\"msg-1\"", "\"key\":\"count\"");
        assertEquals("count", response.key());
        assertEquals("long", response.value().type());
        assertEquals("5", response.value().value());
    }

    @Test
    void setMessageAttributeParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"messageId\":\"msg-1\",\"key\":\"count\","
                    + "\"value\":{\"type\":\"long\",\"value\":5}}");
        });

        GetMessageAttributeResponse response = newClient().setMessageAttribute("msg-1", "count", new Variant("long", 5));

        assertEquals("set-message-attribute", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"messageId\":\"msg-1\"", "\"key\":\"count\"",
                "\"value\":{\"type\":\"long\",\"value\":5}");
        assertEquals("count", response.key());
    }

    @Test
    void addTopicTagSendsErnKeyAndValue() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newClient().addTopicTag("topic-ern", "env", "prod");

        assertEquals("add-topic-tag", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"ern\":\"topic-ern\"", "\"key\":\"env\"", "\"value\":\"prod\"");
    }

    @Test
    void setTopicTagSendsErnKeyAndValue() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newClient().setTopicTag("topic-ern", "env", "staging");

        assertEquals("set-topic-tag", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"ern\":\"topic-ern\"", "\"key\":\"env\"", "\"value\":\"staging\"");
    }

    @Test
    void deleteTopicTagSendsErnAndKeyToEnsTarget() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newClient().deleteTopicTag("topic-ern", "env");

        assertEquals("ens", received.get().header("x-euclid-target"));
        assertEquals("delete-topic-tag", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"ern\":\"topic-ern\"", "\"key\":\"env\"");
    }

    @Test
    void subscribeUsesDefaultSqsType() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"ern\":\"sub-ern\",\"sourceErn\":\"topic-ern\",\"type\":\"SQS\","
                    + "\"targetErn\":\"queue-ern\"}");
        });

        SubscribeResponse response = newClient().subscribe("topic-ern", "queue-ern");

        assertEquals("subscribe", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"sourceErn\":\"topic-ern\"", "\"type\":\"SQS\"",
                "\"targetErn\":\"queue-ern\"");
        assertEquals("sub-ern", response.ern());
    }

    @Test
    void subscribeWithExplicitType() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"ern\":\"sub-ern\"}");
        });

        newClient().subscribe("topic-ern", "HTTP", "https://example.com/webhook");

        assertBodyContains(received.get().body(), "\"type\":\"HTTP\"", "\"targetErn\":\"https://example.com/webhook\"");
    }

    @Test
    void unsubscribeSendsErn() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newClient().unsubscribe("sub-ern");

        assertEquals("unsubscribe", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"ern\":\"sub-ern\"");
    }

    @Test
    void listSubscriptionsParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"subscriptions\":[{\"ern\":\"sub-ern\",\"sourceErn\":\"topic-ern\","
                    + "\"type\":\"SQS\",\"targetErn\":\"queue-ern\",\"created\":\"2026-01-01\","
                    + "\"modified\":\"2026-01-02\"}],\"total\":1}");
        });

        ListSubscriptionsResponse response = newClient().listSubscriptions("topic-ern");

        assertEquals("list-subscriptions", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"topicErn\":\"topic-ern\"");
        assertEquals(1, response.total());
        assertEquals("sub-ern", response.subscriptions().getFirst().ern());
    }

    @Test
    void nonSuccessResponseThrowsEuclidServiceException() throws Exception {
        server = startServer(exchange -> sendResponse(exchange, 500, "{\"error\":\"boom\"}"));

        EuclidEns ens = newClient();
        EuclidServiceException exception =
                assertThrows(EuclidServiceException.class, () -> ens.createTopic("orders"));

        assertEquals("ens", exception.service());
        assertEquals("create-topic", exception.action());
        assertEquals(500, exception.statusCode());
        assertTrue(exception.responseBody().contains("boom"));
    }

    @Test
    void createTopicSendsNamespaceHeaderWhenConfigured() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"name\":\"orders\",\"ern\":\"ern:orders\"}");
        });

        new EuclidEns(baseUrl(), "test-token", "eu-central-1", "863459426936", "alice", null, null, null, "prod")
                .createTopic("orders");

        assertEquals("prod", received.get().header("x-euclid-namespace"));
    }

    @Test
    void createTopicOmitsNamespaceHeaderWhenUnset() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"name\":\"orders\",\"ern\":\"ern:orders\"}");
        });

        newClient().createTopic("orders");

        assertEquals("", received.get().header("x-euclid-namespace"));
    }

    @Test
    void listRequestsCarrySortDirection() throws Exception {
        Map<String, String> bodyByAction = new ConcurrentHashMap<>();
        server = startServer(exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            bodyByAction.put(action, new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendResponse(exchange, 200, "{\"total\":0,\"topics\":[],\"messages\":[]}");
        });

        EuclidEns ens = newClient();
        ens.listTopics("", 10, 0, "name", "desc");
        ens.listMessages("topic-ern", 10, 0, "created", "desc");

        assertBodyContains(bodyByAction.get("list-topics"), "\"sortDirection\":\"desc\"");
        assertBodyContains(bodyByAction.get("list-messages"), "\"sortDirection\":\"desc\"");
    }

    // The overloads without a direction have to keep sending one rather than dropping the field.
    @Test
    void listRequestsDefaultToAscending() throws Exception {
        Map<String, String> bodyByAction = new ConcurrentHashMap<>();
        server = startServer(exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            bodyByAction.put(action, new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendResponse(exchange, 200, "{\"total\":0,\"topics\":[],\"messages\":[]}");
        });

        EuclidEns ens = newClient();
        ens.listTopics();
        ens.listMessages("topic-ern");

        assertBodyContains(bodyByAction.get("list-topics"), "\"sortDirection\":\"asc\"");
        assertBodyContains(bodyByAction.get("list-messages"), "\"sortDirection\":\"asc\"");
    }

    private EuclidEns newClient() {
        return new EuclidEns(baseUrl(), "test-token", "eu-central-1", "863459426936", "alice", null, null, null, null);
    }

    private static String messageJson(String messageId) {
        return "{\"ern\":\"msg-ern\",\"topicErn\":\"topic-ern\",\"messageId\":\"" + messageId + "\","
                + "\"status\":\"AVAILABLE\",\"body\":\"hello\",\"md5Body\":\"abc\",\"attributes\":{},"
                + "\"md5Attributes\":\"def\",\"lastReceived\":null,\"created\":\"2026-01-01\",\"modified\":\"2026-01-02\"}";
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
