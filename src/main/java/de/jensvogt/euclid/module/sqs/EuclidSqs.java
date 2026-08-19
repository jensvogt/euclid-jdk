package de.jensvogt.euclid.module.sqs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jensvogt.euclid.dto.sqs.CreateQueueRequest;
import de.jensvogt.euclid.dto.sqs.CreateQueueResponse;
import de.jensvogt.euclid.dto.sqs.DeleteMessageRequest;
import de.jensvogt.euclid.dto.sqs.DeleteQueueRequest;
import de.jensvogt.euclid.dto.sqs.GetMessageCountRequest;
import de.jensvogt.euclid.dto.sqs.GetMessageCountResponse;
import de.jensvogt.euclid.dto.sqs.GetQueueErnRequest;
import de.jensvogt.euclid.dto.sqs.GetQueueErnResponse;
import de.jensvogt.euclid.dto.sqs.ListQueueRequest;
import de.jensvogt.euclid.dto.sqs.PurgeAllQueuesRequest;
import de.jensvogt.euclid.dto.sqs.PurgeQueueRequest;
import de.jensvogt.euclid.dto.sqs.ReceiveMessagesRequest;
import de.jensvogt.euclid.dto.sqs.ReceiveMessagesResponse;
import de.jensvogt.euclid.dto.sqs.SendMessageRequest;
import de.jensvogt.euclid.dto.sqs.SendMessageResponse;
import de.jensvogt.euclid.dto.sqs.model.Message;
import de.jensvogt.euclid.dto.sqs.model.Queue;
import de.jensvogt.euclid.dto.sqs.model.Variant;
import de.jensvogt.euclid.http.EuclidHttpClient;
import de.jensvogt.euclid.exception.EuclidAuthenticationException;
import de.jensvogt.euclid.auth.SigV4;
import de.jensvogt.euclid.auth.SignableRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQS operations for an authenticated {@link de.jensvogt.euclid.module.access.EuclidSession}.
 */
public final class EuclidSqs {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final long RECEIVE_POLL_INTERVAL_MS = 500;
    private static final String TARGET = "sqs";

    private final String baseUrl;
    private final String token;
    private final String region;
    private final String accountId;
    private final String userId;
    private final String accessKeyId;
    private final String secretAccessKey;
    private final EuclidHttpClient httpClient;

    public EuclidSqs(String baseUrl, String token, String region, String accountId, String userId,
                      String accessKeyId, String secretAccessKey, String caCertPath) {
        this.baseUrl = baseUrl;
        this.token = token;
        this.region = region;
        this.accountId = accountId;
        this.userId = userId;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.httpClient = new EuclidHttpClient(caCertPath);
    }

    public List<Queue> listQueues() throws IOException, InterruptedException {
        return listQueues("", 10, 0, "name");
    }

    public List<Queue> listQueues(String prefix, long pageSize, long pageIndex, String sortColumn)
            throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                ListQueueRequest.builder().prefix(prefix).pageSize(pageSize).pageIndex(pageIndex)
                        .sortColumn(sortColumn).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "sqs", "list-queues",
                requestHeaders("list-queues", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }

        return extractQueues(response.body());
    }

    public CreateQueueResponse createQueue(String name) throws IOException, InterruptedException {
        return createQueue(name, 30, 3, 1024 * 1024, "");
    }

    public CreateQueueResponse createQueue(String name, long visibility, long maxRetries, long maxMessageLength,
                                            String dlqName) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                CreateQueueRequest.builder().name(name).visibility(visibility).maxRetries(maxRetries)
                        .maxMessageLength(maxMessageLength).dlqName(dlqName).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "sqs", "create-queue",
                requestHeaders("create-queue", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }

        return extractCreateQueueResponse(response.body());
    }

    public void deleteQueue(String ern) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(DeleteQueueRequest.builder().ern(ern).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "sqs", "delete-queue",
                requestHeaders("delete-queue", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }
    }

    public GetQueueErnResponse getQueueErn(String name) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(GetQueueErnRequest.builder().name(name).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "sqs", "get-queue-ern",
                requestHeaders("get-queue-ern", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }

        return extractGetQueueErnResponse(response.body());
    }

    public void purgeQueue(String ern) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(PurgeQueueRequest.builder().ern(ern).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "sqs", "purge-queue",
                requestHeaders("purge-queue", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }
    }

    public void purgeAllQueues() throws IOException, InterruptedException {
        purgeAllQueues(region, accountId);
    }

    public void purgeAllQueues(String region, String accountId) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                PurgeAllQueuesRequest.builder().region(region).accountId(accountId).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "sqs", "purge-all-queues",
                requestHeaders("purge-all-queues", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }
    }

    public SendMessageResponse sendMessage(String ern, String body) throws IOException, InterruptedException {
        return sendMessage(ern, body, new HashMap<>());
    }

    public SendMessageResponse sendMessage(String ern, String body, Map<String, Variant> attributes)
            throws IOException, InterruptedException {
        String requestBody = OBJECT_MAPPER.writeValueAsString(
                SendMessageRequest.builder().ern(ern).body(body).attributes(attributes).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", requestBody, "sqs", "send-message",
                requestHeaders("send-message", requestBody));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }

        return extractSendMessageResponse(response.body());
    }

    public ReceiveMessagesResponse receiveMessages(String ern) throws IOException, InterruptedException {
        return receiveMessages(ern, 10, 0);
    }

    public ReceiveMessagesResponse receiveMessages(String ern, long maxMessages, long waitTime)
            throws IOException, InterruptedException {

        if (waitTime <= 0) {
            long available = getMessageCount(ern).available();
            if (available <= 0) {
                return ReceiveMessagesResponse.builder().messages(new ArrayList<>()).total(0).build();
            }
            return doReceiveMessages(ern, maxMessages);
        }

        long deadline = System.currentTimeMillis() + waitTime * 1000;
        while (true) {
            ReceiveMessagesResponse response = doReceiveMessages(ern, maxMessages);
            if (!response.messages().isEmpty()) {
                return response;
            }

            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                return response;
            }
            Thread.sleep(Math.min(RECEIVE_POLL_INTERVAL_MS, remaining));
        }
    }

    public ReceiveMessagesResponse receiveAllMessages(String ern) throws IOException, InterruptedException {
        return receiveAllMessages(ern, 10);
    }

    public ReceiveMessagesResponse receiveAllMessages(String ern, long batchSize) throws IOException, InterruptedException {
        List<Message> messages = new ArrayList<>();
        while (true) {
            List<Message> batch = receiveMessages(ern, batchSize, 0).messages();
            if (batch.isEmpty()) {
                break;
            }
            messages.addAll(batch);
        }
        return ReceiveMessagesResponse.builder().messages(messages).total(messages.size()).build();
    }

    private ReceiveMessagesResponse doReceiveMessages(String ern, long maxMessages)
            throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                ReceiveMessagesRequest.builder().ern(ern).maxCount(maxMessages).waitTime(0).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "sqs", "receive-messages",
                requestHeaders("receive-messages", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }

        return extractReceiveMessagesResponse(response.body());
    }

    public void deleteMessage(String receiptHandle) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                DeleteMessageRequest.builder().receiptHandle(receiptHandle).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "sqs", "delete-message",
                requestHeaders("delete-message", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }
    }

    public GetMessageCountResponse getMessageCount(String ern) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(GetMessageCountRequest.builder().ern(ern).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "sqs", "get-message-count",
                requestHeaders("get-message-count", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }

        return extractGetMessageCountResponse(response.body());
    }

    private static SendMessageResponse extractSendMessageResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return SendMessageResponse.builder().messageId(textOrNull(root, "messageId")).build();
    }

    private static CreateQueueResponse extractCreateQueueResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return CreateQueueResponse.builder().name(textOrNull(root, "name")).ern(textOrNull(root, "ern")).build();
    }

    private static GetQueueErnResponse extractGetQueueErnResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return GetQueueErnResponse.builder().ern(textOrNull(root, "ern")).build();
    }

    private static GetMessageCountResponse extractGetMessageCountResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return GetMessageCountResponse.builder().ern(textOrNull(root, "ern")).available(root.path("available").asLong(0))
                .delayed(root.path("delayed").asLong(0)).invisible(root.path("invisible").asLong(0)).build();
    }

    /**
     * Builds the headers for one request/action: routing headers plus authentication.
     * <p>
     * Signs with SigV4 (accessKeyId/secretAccessKey) when both are configured, mirroring how
     * euclid-cli authenticates service calls; falls back to the bearer token otherwise. The
     * access module itself always uses the bearer token instead (see {@code EuclidSession}),
     * since there's no access key yet at login time.
     */
    private Map<String, String> requestHeaders(String action, String body) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        if (region != null) {
            headers.put("x-euclid-region", region);
        }
        if (accountId != null) {
            headers.put("x-euclid-account-id", accountId);
        }
        if (userId != null) {
            headers.put("x-euclid-user-id", userId);
        }

        if (accessKeyId != null && !accessKeyId.isEmpty() && secretAccessKey != null && !secretAccessKey.isEmpty()) {
            SignableRequest signable = new SignableRequest("POST", "/");
            headers.forEach(signable::header);
            signable.header("host", hostHeader());
            signable.header("x-euclid-target", TARGET);
            signable.header("x-euclid-action", action);
            signable.body(body);
            SigV4.sign(signable, accessKeyId, secretAccessKey, region, TARGET);
            headers.put("x-amz-date", signable.header("x-amz-date"));
            headers.put("x-amz-content-sha256", signable.header("x-amz-content-sha256"));
            headers.put("Authorization", signable.header("authorization"));
        } else {
            headers.put("Authorization", "Bearer " + token);
        }
        return headers;
    }

    // The literal "Host" header java.net.http will put on the wire, derived the same way it
    // derives it (from the URI's authority) so the value we sign here matches what the server
    // actually receives.
    private String hostHeader() {
        URI uri = URI.create(baseUrl);
        int port = uri.getPort();
        return port == -1 ? uri.getHost() : uri.getHost() + ":" + port;
    }

    private static ReceiveMessagesResponse extractReceiveMessagesResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        JsonNode messagesNode = root.get("messages");
        List<Message> messages = new ArrayList<>();
        if (messagesNode != null && messagesNode.isArray()) {
            for (JsonNode messageNode : messagesNode) {
                messages.add(new Message(
                        textOrNull(messageNode, "ern"),
                        textOrNull(messageNode, "body"),
                        textOrNull(messageNode, "md5sum"),
                        textOrNull(messageNode, "receiptHandle"),
                        toVariantMap(messageNode.get("attributes")),
                        textOrNull(messageNode, "lastReceived"),
                        textOrNull(messageNode, "created"),
                        textOrNull(messageNode, "modified")));
            }
        }
        return ReceiveMessagesResponse.builder().messages(messages).total(root.path("total").asLong(0)).build();
    }

    private static Map<String, Variant> toVariantMap(JsonNode node) {
        Map<String, Variant> map = new LinkedHashMap<>();
        if (node != null && node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                JsonNode valueNode = entry.getValue();
                map.put(entry.getKey(), new Variant(textOrNull(valueNode, "type"), textOrNull(valueNode, "value")));
            });
        }
        return map;
    }

    private static List<Queue> extractQueues(String responseBody) throws IOException {
        JsonNode queuesNode = OBJECT_MAPPER.readTree(responseBody).get("queues");
        List<Queue> queues = new ArrayList<>();
        if (queuesNode != null && queuesNode.isArray()) {
            for (JsonNode queueNode : queuesNode) {
                queues.add(new Queue(
                        textOrNull(queueNode, "region"),
                        textOrNull(queueNode, "name"),
                        textOrNull(queueNode, "owner"),
                        textOrNull(queueNode, "ern"),
                        toStringMap(queueNode.get("tags")),
                        queueNode.path("delay").asLong(0),
                        queueNode.path("size").asLong(0),
                        queueNode.path("messages").asLong(0),
                        queueNode.path("delayed").asLong(0),
                        queueNode.path("busy").asLong(0),
                        queueNode.path("visibility").asLong(30),
                        queueNode.path("maxMessageLength").asLong(1024 * 1024),
                        queueNode.path("maxReceiveCount").asLong(3),
                        textOrNull(queueNode, "deadLetterQueueArn"),
                        textOrNull(queueNode, "created"),
                        textOrNull(queueNode, "modified")));
            }
        }
        return queues;
    }

    private static Map<String, String> toStringMap(JsonNode node) {
        Map<String, String> map = new LinkedHashMap<>();
        if (node != null && node.isObject()) {
            node.fields().forEachRemaining(entry -> map.put(entry.getKey(), entry.getValue().asText()));
        }
        return map;
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
