package de.jensvogt.euclid.module.eqs;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.jensvogt.euclid.auth.SigV4;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.dto.com.Variant;
import de.jensvogt.euclid.dto.eqs.CreateQueueResponse;
import de.jensvogt.euclid.dto.eqs.GetMessageAttributeResponse;
import de.jensvogt.euclid.dto.eqs.GetMessageCountResponse;
import de.jensvogt.euclid.dto.eqs.GetMessageMetadataResponse;
import de.jensvogt.euclid.dto.eqs.GetQueueErnResponse;
import de.jensvogt.euclid.dto.eqs.GetQueueMetadataResponse;
import de.jensvogt.euclid.dto.eqs.ListMessagesResponse;
import de.jensvogt.euclid.dto.eqs.ListQueueResponse;
import de.jensvogt.euclid.dto.eqs.ReceiveMessagesResponse;
import de.jensvogt.euclid.dto.eqs.SendMessageResponse;
import de.jensvogt.euclid.dto.eqs.model.Message;
import de.jensvogt.euclid.dto.eqs.model.Queue;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms EuclidEqs authenticates the way it claims to (SigV4-signed when an access key is
 * configured, bearer token otherwise, mirroring euclid-cli's HttpClient.cpp), routes every
 * operation to the right action with a correctly-shaped request body, parses the corresponding
 * response, and surfaces non-2xx responses as {@link EuclidServiceException}.
 */
class EuclidEqsTest {

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
    void sendMessageSignsWithSigV4WhenAccessKeyConfigured() throws Exception {
        String accessKeyId = "AKIDEXAMPLE";
        String secretAccessKey = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";

        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"messageId\":\"msg-1\"}");
        });

        EuclidEqs sqs = new EuclidEqs(baseUrl(), "unused-token", "eu-central-1", "863459426936", "alice",
                accessKeyId, secretAccessKey, null, null);
        sqs.sendMessage("ern:sqs:eu-central-1:863459426936:queue/test", "hello");

        SignableRequest req = received.get();
        assertTrue(req.header("authorization").startsWith("AWS4-HMAC-SHA256 "));

        Optional<SigV4.VerifyResult> result = SigV4.verify(req,
                id -> id.equals(accessKeyId) ? Optional.of(secretAccessKey) : Optional.empty());
        assertTrue(result.isPresent(), "server-side verification of the client's own signature must succeed");
        assertEquals(accessKeyId, result.get().accessKeyId());
    }

    @Test
    void sendMessageUsesBearerTokenWhenNoAccessKeyConfigured() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"messageId\":\"msg-1\"}");
        });

        EuclidEqs sqs = new EuclidEqs(baseUrl(), "my-jwt-token", "eu-central-1", "863459426936", "alice",
                null, null, null, null);
        sqs.sendMessage("ern:sqs:eu-central-1:863459426936:queue/test", "hello");

        assertEquals("Bearer my-jwt-token", received.get().header("authorization"));
    }

    @Test
    void listQueuesUsesDefaultsAndParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"queues\":[{\"name\":\"orders\","
                    + "\"owner\":\"alice\",\"ern\":\"ern:sqs:eu-central-1:863459426936:queue/orders\","
                    + "\"tags\":{\"env\":\"prod\"},\"size\":100,\"delay\":5,\"available\":3,\"delayed\":1,"
                    + "\"invisible\":0,\"visibility\":30,\"maxMessageLength\":1048576,\"maxReceiveCount\":3,"
                    + "\"deadLetterQueueArn\":null,\"priority\":\"MIDDLE\",\"created\":\"2026-01-01\","
                    + "\"modified\":\"2026-01-02\"}],\"total\":1}");
        });

        ListQueueResponse response = newClient().listQueues();
        List<Queue> queues = response.queues();
        assertEquals(1, response.total(), "the server's total must survive rather than be dropped");

        assertEquals("list-queues", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"prefix\":\"\"", "\"pageSize\":10", "\"pageIndex\":0",
                "\"sortColumn\":\"name\"");

        assertEquals(1, queues.size());
        Queue queue = queues.getFirst();
        assertEquals("orders", queue.name());
        assertEquals("ern:sqs:eu-central-1:863459426936:queue/orders", queue.ern());
        assertEquals("prod", queue.tags().get("env"));
        assertEquals(3, queue.available());
        assertEquals(1, queue.delayed());
        assertEquals("MIDDLE", queue.priority());
        assertNullSafe(queue.deadLetterQueueArn());
    }

    @Test
    void listQueuesWithExplicitParameters() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"queues\":[]}");
        });

        List<Queue> queues = newClient().listQueues("ord", 25, 2, "created").queues();

        assertBodyContains(received.get().body(), "\"prefix\":\"ord\"", "\"pageSize\":25", "\"pageIndex\":2",
                "\"sortColumn\":\"created\"");
        assertTrue(queues.isEmpty());
    }

    @Test
    void listMessagesParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"messages\":[" + messageJson("msg-1", "rh-1") + "],\"total\":1}");
        });

        ListMessagesResponse response = newClient().listMessages("queue-ern");

        assertEquals("list-messages", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"queueErn\":\"queue-ern\"", "\"pageSize\":10",
                "\"pageIndex\":0", "\"sortColumn\":\"created\"");
        assertEquals(1, response.total());
        assertEquals("msg-1", response.messages().getFirst().messageId());
    }

    @Test
    void createQueueUsesDefaultsAndParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"name\":\"orders\",\"ern\":\"ern:sqs:eu-central-1:863459426936:queue/orders\"}");
        });

        CreateQueueResponse response = newClient().createQueue("orders");

        assertEquals("create-queue", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"name\":\"orders\"", "\"visibility\":30", "\"maxRetries\":3",
                "\"maxMessageLength\":1048576", "\"dlqName\":\"\"", "\"delay\":0");
        assertEquals("orders", response.name());
        assertEquals("ern:sqs:eu-central-1:863459426936:queue/orders", response.ern());
    }

    @Test
    void createQueueWithExplicitParameters() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"name\":\"orders\",\"ern\":\"ern:orders\"}");
        });

        newClient().createQueue("orders", 60, 5, 2048, "orders-dlq", 10);

        assertBodyContains(received.get().body(), "\"visibility\":60", "\"maxRetries\":5", "\"maxMessageLength\":2048",
                "\"dlqName\":\"orders-dlq\"", "\"delay\":10");
    }

    @Test
    void deleteQueueSendsErn() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newClient().deleteQueue("queue-ern");

        assertEquals("delete-queue", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"ern\":\"queue-ern\"");
    }

    @Test
    void getQueueErnParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"ern\":\"ern:sqs:eu-central-1:863459426936:queue/orders\"}");
        });

        GetQueueErnResponse response = newClient().getQueueErn("orders");

        assertEquals("get-queue-ern", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"name\":\"orders\"");
        assertEquals("ern:sqs:eu-central-1:863459426936:queue/orders", response.ern());
    }

    @Test
    void getQueueMetadataParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"region\":\"eu-central-1\",\"accountId\":\"863459426936\","
                    + "\"owner\":\"alice\",\"nameSpace\":\"default\",\"name\":\"orders\",\"ern\":\"queue-ern\","
                    + "\"size\":42,\"messages\":7}");
        });

        GetQueueMetadataResponse response = newClient().getQueueMetadata("queue-ern");

        assertEquals("get-queue-metadata", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"ern\":\"queue-ern\"");
        assertEquals("orders", response.name());
        assertEquals(42, response.size());
        assertEquals(7, response.messages());
    }

    @Test
    void purgeQueueSendsErn() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newClient().purgeQueue("queue-ern");

        assertEquals("purge-queue", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"ern\":\"queue-ern\"");
    }

    @Test
    void purgeAllQueuesUsesInstanceRegionAndAccountByDefault() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newClient().purgeAllQueues();

        assertEquals("purge-all-queues", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"region\":\"eu-central-1\"", "\"accountId\":\"863459426936\"");
    }

    @Test
    void purgeAllQueuesWithExplicitRegionAndAccount() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newClient().purgeAllQueues("us-east-1", "111111111111");

        assertBodyContains(received.get().body(), "\"region\":\"us-east-1\"", "\"accountId\":\"111111111111\"");
    }

    @Test
    void sendMessageWithDefaultsUsesMiddlePriorityAndEmptyAttributes() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"messageId\":\"msg-1\",\"md5Body\":\"abc\",\"md5Attributes\":\"def\"}");
        });

        SendMessageResponse response = newClient().sendMessage("queue-ern", "hello");

        assertEquals("send-message", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"ern\":\"queue-ern\"", "\"body\":\"hello\"",
                "\"attributes\":{}", "\"priority\":\"MIDDLE\"");
        assertEquals("msg-1", response.messageId());
        assertEquals("abc", response.md5Body());
        assertEquals("def", response.md5Attributes());
    }

    @Test
    void sendMessageWithAttributesAndPriority() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"messageId\":\"msg-1\"}");
        });

        newClient().sendMessage("queue-ern", "hello", Map.of("count", new Variant("long", 5)), "HIGH");

        assertBodyContains(received.get().body(), "\"priority\":\"HIGH\"", "\"count\":{\"type\":\"long\",\"value\":5}");
    }

    @Test
    void receiveMessagesReturnsEmptyWithoutPollingWhenNoneAvailable() throws Exception {
        AtomicInteger receiveMessagesCalls = new AtomicInteger();
        server = startServer(exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            if ("get-message-count".equals(action)) {
                sendResponse(exchange, 200, "{\"ern\":\"queue-ern\",\"available\":0,\"delayed\":0,\"invisible\":0}");
            } else if ("receive-messages".equals(action)) {
                receiveMessagesCalls.incrementAndGet();
                sendResponse(exchange, 200, "{\"messages\":[],\"total\":0}");
            } else {
                sendResponse(exchange, 500, "{\"error\":\"unexpected action " + action + "\"}");
            }
        });

        ReceiveMessagesResponse response = newClient().receiveMessages("queue-ern", 10, 0);

        assertTrue(response.messages().isEmpty());
        assertEquals(0, receiveMessagesCalls.get(), "should short-circuit on the message count instead of polling");
    }

    @Test
    void receiveMessagesReturnsMessagesWhenAvailable() throws Exception {
        AtomicReference<SignableRequest> receiveRequest = new AtomicReference<>();
        server = startServer(exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            if ("get-message-count".equals(action)) {
                sendResponse(exchange, 200, "{\"ern\":\"queue-ern\",\"available\":1,\"delayed\":0,\"invisible\":0}");
            } else if ("receive-messages".equals(action)) {
                receiveRequest.set(captureRequest(exchange));
                sendResponse(exchange, 200, "{\"messages\":[" + messageJson("msg-1", "rh-1") + "],\"total\":1}");
            } else {
                sendResponse(exchange, 500, "{\"error\":\"unexpected action " + action + "\"}");
            }
        });

        ReceiveMessagesResponse response = newClient().receiveMessages("queue-ern", 5, 0);

        assertBodyContains(receiveRequest.get().body(), "\"maxCount\":5", "\"waitTime\":0");
        assertEquals(1, response.messages().size());
        assertEquals("msg-1", response.messages().getFirst().messageId());
        assertEquals("rh-1", response.messages().getFirst().receiptHandle());
    }

    @Test
    void receiveMessagesSendsTheWaitToTheServerAndAsksOnce() throws Exception {
        AtomicInteger callCount = new AtomicInteger();
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            callCount.incrementAndGet();
            received.set(captureRequest(exchange));
            // What a server that honoured the wait does: hold the request, then answer empty.
            sleepQuietly(1200);
            sendResponse(exchange, 200, "{\"messages\":[],\"total\":0}");
        });

        ReceiveMessagesResponse response = newClient().receiveMessages("queue-ern", 10, 1);

        assertTrue(response.messages().isEmpty());
        assertEquals(1, callCount.get(), "the waiting is the server's, so one request covers the whole window");
        assertBodyContains(received.get().body(), "\"waitTime\":1");
    }

    @Test
    void receiveMessagesAsksAgainWhenTheServerDeclinesToWait() throws Exception {
        // A server with no long-poll slot free answers at once with whatever is in the queue
        // rather than queueing behind the waiters, so an empty answer arrives with the window
        // barely started. The wait is the caller's, so it has to be seen through.
        AtomicInteger callCount = new AtomicInteger();
        server = startServer(exchange -> {
            if (callCount.getAndIncrement() == 0) {
                sendResponse(exchange, 200, "{\"messages\":[],\"total\":0}");
            } else {
                sendResponse(exchange, 200, "{\"messages\":[" + messageJson("msg-1", "rh-1") + "],\"total\":1}");
            }
        });

        ReceiveMessagesResponse response = newClient().receiveMessages("queue-ern", 10, 5);

        assertTrue(callCount.get() >= 2, "should have asked again after the server answered early");
        assertEquals(1, response.messages().size());
        assertEquals("msg-1", response.messages().getFirst().messageId());
    }

    @Test
    void receiveMessagesBacksOffRatherThanSpinningWhenTheServerNeverWaits() throws Exception {
        AtomicInteger callCount = new AtomicInteger();
        server = startServer(exchange -> {
            callCount.incrementAndGet();
            sendResponse(exchange, 200, "{\"messages\":[],\"total\":0}");
        });

        ReceiveMessagesResponse response = newClient().receiveMessages("queue-ern", 10, 2);

        assertTrue(response.messages().isEmpty());
        // Two seconds at the 500ms backoff is a handful of requests. Without one it would be
        // thousands - a server already short of threads being asked as fast as it can answer.
        assertTrue(callCount.get() <= 6,
                "should have backed off between attempts, but asked " + callCount.get() + " times");
    }

    @Test
    void receiveAllMessagesDrainsMultipleBatches() throws Exception {
        AtomicInteger messageCountCalls = new AtomicInteger();
        server = startServer(exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            if ("get-message-count".equals(action)) {
                long available = messageCountCalls.getAndIncrement() == 0 ? 2 : 0;
                sendResponse(exchange, 200, "{\"ern\":\"queue-ern\",\"available\":" + available + ",\"delayed\":0,\"invisible\":0}");
            } else if ("receive-messages".equals(action)) {
                sendResponse(exchange, 200, "{\"messages\":[" + messageJson("msg-1", "rh-1") + ","
                        + messageJson("msg-2", "rh-2") + "],\"total\":2}");
            } else {
                sendResponse(exchange, 500, "{\"error\":\"unexpected action " + action + "\"}");
            }
        });

        ReceiveMessagesResponse response = newClient().receiveAllMessages("queue-ern");

        assertEquals(2, response.total());
        assertEquals(List.of("msg-1", "msg-2"), response.messages().stream().map(Message::messageId).toList());
        assertEquals(2, messageCountCalls.get(), "should stop once the count check reports nothing left");
    }

    @Test
    void deleteMessageSendsReceiptHandle() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newClient().deleteMessage("rh-1");

        assertEquals("delete-message", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"receiptHandle\":\"rh-1\"");
    }

    // Deleting by ID reaches a message that was never received - AVAILABLE or DELAYED - which the
    // receipt-handle route cannot, since a handle only exists once a message has been received.
    @Test
    void deleteMessageByIdSendsMessageIdWithNoReceiptHandle() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newClient().deleteMessageById("msg-1");

        assertEquals("delete-message", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"messageId\":\"msg-1\"", "\"receiptHandle\":\"\"");
    }

    @Test
    void createQueueSendsPriority() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"name\":\"orders\",\"ern\":\"queue-ern\"}");
        });

        newClient().createQueue("orders", 30, 3, 1024, "dlq", 5, "HIGH");

        assertBodyContains(received.get().body(), "\"name\":\"orders\"", "\"priority\":\"HIGH\"",
                "\"dlqName\":\"dlq\"", "\"delay\":5");
    }

    // The overloads without a priority still have to send one - the queue's default is set at
    // creation and every message inherits it unless send-message overrides it.
    @Test
    void createQueueDefaultsToMiddlePriority() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"name\":\"orders\",\"ern\":\"queue-ern\"}");
        });

        newClient().createQueue("orders");

        assertBodyContains(received.get().body(), "\"priority\":\"MIDDLE\"");
    }

    @Test
    void listRequestsCarrySortDirection() throws Exception {
        Map<String, String> bodyByAction = new ConcurrentHashMap<>();
        server = startServer(exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            bodyByAction.put(action, new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendResponse(exchange, 200, "{\"total\":0,\"queues\":[],\"messages\":[]}");
        });

        EuclidEqs eqs = newClient();
        eqs.listQueues("", 10, 0, "name", "desc");
        eqs.listMessages("queue-ern", 10, 0, "created", "desc");

        assertBodyContains(bodyByAction.get("list-queues"), "\"sortDirection\":\"desc\"");
        assertBodyContains(bodyByAction.get("list-messages"), "\"sortDirection\":\"desc\"");
    }

    // The overloads without a direction have to keep sending one rather than dropping the field.
    @Test
    void listRequestsDefaultToAscending() throws Exception {
        Map<String, String> bodyByAction = new ConcurrentHashMap<>();
        server = startServer(exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            bodyByAction.put(action, new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendResponse(exchange, 200, "{\"total\":0,\"queues\":[],\"messages\":[]}");
        });

        EuclidEqs eqs = newClient();
        eqs.listQueues();
        eqs.listMessages("queue-ern");

        assertBodyContains(bodyByAction.get("list-queues"), "\"sortDirection\":\"asc\"");
        assertBodyContains(bodyByAction.get("list-messages"), "\"sortDirection\":\"asc\"");
    }

    @Test
    void getQueueErnParsesTheResolvedName() throws Exception {
        server = startServer(exchange -> {
            captureRequest(exchange);
            sendResponse(exchange, 200, "{\"name\":\"orders\",\"ern\":\"queue-ern\"}");
        });

        GetQueueErnResponse response = newClient().getQueueErn("orders");

        assertEquals("orders", response.name());
        assertEquals("queue-ern", response.ern());
    }

    @Test
    void getMessageCountParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"ern\":\"queue-ern\",\"available\":3,\"delayed\":2,\"invisible\":1,\"total\":6}");
        });

        GetMessageCountResponse response = newClient().getMessageCount("queue-ern");

        assertEquals("get-message-count", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"ern\":\"queue-ern\"");
        assertEquals(3, response.available());
        assertEquals(2, response.delayed());
        assertEquals(1, response.invisible());
        // The server sends the total rather than the client re-deriving it from the three counts.
        assertEquals(6, response.total());
    }

    @Test
    void setVisibilitySendsMessageIdAndVisibility() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newClient().setVisibility("msg-1", 45);

        assertEquals("set-visibility", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"messageId\":\"msg-1\"", "\"visibility\":45");
    }

    @Test
    void getMessageAttributeParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"messageId\":\"msg-1\",\"name\":\"count\","
                    + "\"value\":{\"type\":\"long\",\"value\":5}}");
        });

        GetMessageAttributeResponse response = newClient().getMessageAttribute("msg-1", "count");

        assertEquals("get-message-attribute", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"messageId\":\"msg-1\"", "\"name\":\"count\"");
        assertEquals("count", response.name());
        assertEquals("long", response.value().type());
        assertEquals("5", response.value().value());
    }

    @Test
    void getMessageMetadataParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"messageId\":\"msg-1\",\"queueErn\":\"queue-ern\","
                    + "\"receiptHandle\":\"rh-1\",\"status\":\"VISIBLE\",\"priority\":\"MIDDLE\",\"size\":11,"
                    + "\"receivedCount\":2,\"visibilityTimeout\":30,\"contentType\":\"text/plain\","
                    + "\"md5Body\":\"abc\",\"md5Attributes\":\"def\",\"created\":\"2026-01-01\","
                    + "\"modified\":\"2026-01-02\"}");
        });

        GetMessageMetadataResponse response = newClient().getMessageMetadata("msg-1");

        assertEquals("get-message-metadata", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"messageId\":\"msg-1\"");
        assertEquals("queue-ern", response.queueErn());
        assertEquals("VISIBLE", response.status());
        assertEquals(2, response.receivedCount());
        assertEquals(30, response.visibilityTimeout());
    }

    @Test
    void setMessageAttributeParsesResponse() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"messageId\":\"msg-1\",\"name\":\"count\","
                    + "\"value\":{\"type\":\"long\",\"value\":5}}");
        });

        GetMessageAttributeResponse response = newClient().setMessageAttribute("msg-1", "count", new Variant("long", 5));

        assertEquals("set-message-attribute", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"messageId\":\"msg-1\"", "\"key\":\"count\"",
                "\"value\":{\"type\":\"long\",\"value\":5}");
        assertEquals("count", response.name());
        assertEquals("5", response.value().value());
    }

    @Test
    void addQueueTagSendsErnKeyAndValue() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newClient().addQueueTag("queue-ern", "env", "prod");

        assertEquals("add-queue-tag", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"ern\":\"queue-ern\"", "\"key\":\"env\"", "\"value\":\"prod\"");
    }

    @Test
    void setQueueTagSendsErnKeyAndValue() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newClient().setQueueTag("queue-ern", "env", "staging");

        assertEquals("set-queue-tag", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"ern\":\"queue-ern\"", "\"key\":\"env\"", "\"value\":\"staging\"");
    }

    @Test
    void deleteQueueTagSendsErnAndKey() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{}");
        });

        newClient().deleteQueueTag("queue-ern", "env");

        assertEquals("delete-queue-tag", received.get().header("x-euclid-action"));
        assertBodyContains(received.get().body(), "\"ern\":\"queue-ern\"", "\"key\":\"env\"");
    }

    @Test
    void nonSuccessResponseThrowsEuclidServiceException() throws Exception {
        server = startServer(exchange -> sendResponse(exchange, 500, "{\"error\":\"boom\"}"));

        EuclidEqs sqs = newClient();
        EuclidServiceException exception =
                assertThrows(EuclidServiceException.class, () -> sqs.createQueue("orders"));

        assertEquals("eqs", exception.service());
        assertEquals("create-queue", exception.action());
        assertEquals(500, exception.statusCode());
        assertTrue(exception.responseBody().contains("boom"));
    }

    @Test
    void createQueueSendsNamespaceHeaderWhenConfigured() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"name\":\"orders\",\"ern\":\"ern:orders\"}");
        });

        new EuclidEqs(baseUrl(), "test-token", "eu-central-1", "863459426936", "alice", null, null, null, "prod")
                .createQueue("orders");

        assertEquals("prod", received.get().header("x-euclid-namespace"));
    }

    @Test
    void createQueueOmitsNamespaceHeaderWhenUnset() throws Exception {
        AtomicReference<SignableRequest> received = new AtomicReference<>();
        server = startServer(exchange -> {
            received.set(captureRequest(exchange));
            sendResponse(exchange, 200, "{\"name\":\"orders\",\"ern\":\"ern:orders\"}");
        });

        newClient().createQueue("orders");

        assertEquals("", received.get().header("x-euclid-namespace"));
    }

    private EuclidEqs newClient() {
        return new EuclidEqs(baseUrl(), "test-token", "eu-central-1", "863459426936", "alice", null, null, null, null);
    }

    private static String messageJson(String messageId, String receiptHandle) {
        return "{\"ern\":\"msg-ern\",\"queueErn\":\"queue-ern\",\"messageId\":\"" + messageId + "\","
                + "\"status\":\"VISIBLE\",\"priority\":\"MIDDLE\",\"body\":\"hello\",\"md5Body\":\"abc\","
                + "\"receiptHandle\":\"" + receiptHandle + "\",\"attributes\":{},\"md5Attributes\":\"def\","
                + "\"lastReceived\":null,\"created\":\"2026-01-01\",\"modified\":\"2026-01-02\"}";
    }

    private static void assertBodyContains(String body, String... fragments) {
        for (String fragment : fragments) {
            assertTrue(body.contains(fragment), "expected body to contain " + fragment + " but was " + body);
        }
    }

    private static void assertNullSafe(String value) {
        assertFalse(value != null && !value.isEmpty(), "expected null/empty but was " + value);
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

    /** Stands in for a server that holds a long poll open for the window it was asked for. */
    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
