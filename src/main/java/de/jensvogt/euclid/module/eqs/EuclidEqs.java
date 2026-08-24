package de.jensvogt.euclid.module.eqs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jensvogt.euclid.dto.eqs.CreateQueueRequest;
import de.jensvogt.euclid.dto.eqs.CreateQueueResponse;
import de.jensvogt.euclid.dto.eqs.DeleteMessageRequest;
import de.jensvogt.euclid.dto.eqs.DeleteQueueRequest;
import de.jensvogt.euclid.dto.eqs.GetMessageAttributeRequest;
import de.jensvogt.euclid.dto.eqs.GetMessageAttributeResponse;
import de.jensvogt.euclid.dto.eqs.GetMessageCountRequest;
import de.jensvogt.euclid.dto.eqs.GetMessageCountResponse;
import de.jensvogt.euclid.dto.eqs.GetMessageMetadataRequest;
import de.jensvogt.euclid.dto.eqs.GetMessageMetadataResponse;
import de.jensvogt.euclid.dto.eqs.GetQueueErnRequest;
import de.jensvogt.euclid.dto.eqs.GetQueueErnResponse;
import de.jensvogt.euclid.dto.eqs.GetQueueMetadataRequest;
import de.jensvogt.euclid.dto.eqs.GetQueueMetadataResponse;
import de.jensvogt.euclid.dto.eqs.ListMessagesRequest;
import de.jensvogt.euclid.dto.eqs.ListMessagesResponse;
import de.jensvogt.euclid.dto.eqs.ListQueueRequest;
import de.jensvogt.euclid.dto.eqs.PurgeAllQueuesRequest;
import de.jensvogt.euclid.dto.eqs.PurgeQueueRequest;
import de.jensvogt.euclid.dto.eqs.ReceiveMessagesRequest;
import de.jensvogt.euclid.dto.eqs.ReceiveMessagesResponse;
import de.jensvogt.euclid.dto.eqs.SendMessageRequest;
import de.jensvogt.euclid.dto.eqs.SendMessageResponse;
import de.jensvogt.euclid.dto.eqs.SetMessageVisibilityRequest;
import de.jensvogt.euclid.dto.eqs.model.Message;
import de.jensvogt.euclid.dto.eqs.model.Queue;
import de.jensvogt.euclid.dto.eqs.model.Variant;
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
 * EQS operations for an authenticated {@link de.jensvogt.euclid.module.eam.EuclidSession}.
 */
public final class EuclidEqs {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final long RECEIVE_POLL_INTERVAL_MS = 500;
    private static final String TARGET = "eqs";

    private final String baseUrl;
    private final String token;
    private final String region;
    private final String accountId;
    private final String userId;
    private final String accessKeyId;
    private final String secretAccessKey;
    private final EuclidHttpClient httpClient;

    public EuclidEqs(String baseUrl, String token, String region, String accountId, String userId,
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
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "list-queues",
                requestHeaders("list-queues", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }

        return extractQueues(response.body());
    }

    public ListMessagesResponse listMessages(String queueErn) throws IOException, InterruptedException {
        return listMessages(queueErn, 10, 0, "created");
    }

    public ListMessagesResponse listMessages(String queueErn, long pageSize, long pageIndex, String sortColumn)
            throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                ListMessagesRequest.builder().queueErn(queueErn).pageSize(pageSize).pageIndex(pageIndex)
                        .sortColumn(sortColumn).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "list-messages",
                requestHeaders("list-messages", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }

        return extractListMessagesResponse(response.body());
    }

    public CreateQueueResponse createQueue(String name) throws IOException, InterruptedException {
        return createQueue(name, 30, 3, 1024 * 1024, "", 0);
    }

    public CreateQueueResponse createQueue(String name, long visibility, long maxRetries, long maxMessageLength,
                                            String dlqName) throws IOException, InterruptedException {
        return createQueue(name, visibility, maxRetries, maxMessageLength, dlqName, 0);
    }

    public CreateQueueResponse createQueue(String name, long visibility, long maxRetries, long maxMessageLength,
                                            String dlqName, long delay) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                CreateQueueRequest.builder().name(name).visibility(visibility).maxRetries(maxRetries)
                        .maxMessageLength(maxMessageLength).dlqName(dlqName).delay(delay).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "create-queue",
                requestHeaders("create-queue", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }

        return extractCreateQueueResponse(response.body());
    }

    public void deleteQueue(String ern) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(DeleteQueueRequest.builder().ern(ern).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "delete-queue",
                requestHeaders("delete-queue", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }
    }

    public GetQueueErnResponse getQueueErn(String name) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(GetQueueErnRequest.builder().name(name).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "get-queue-ern",
                requestHeaders("get-queue-ern", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }

        return extractGetQueueErnResponse(response.body());
    }

    public GetQueueMetadataResponse getQueueMetadata(String ern) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(GetQueueMetadataRequest.builder().ern(ern).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "get-queue-metadata",
                requestHeaders("get-queue-metadata", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }

        return extractGetQueueMetadataResponse(response.body());
    }

    public void purgeQueue(String ern) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(PurgeQueueRequest.builder().ern(ern).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "purge-queue",
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
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "purge-all-queues",
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
        return sendMessage(ern, body, attributes, "MIDDLE");
    }

    public SendMessageResponse sendMessage(String ern, String body, Map<String, Variant> attributes, String priority)
            throws IOException, InterruptedException {
        String requestBody = OBJECT_MAPPER.writeValueAsString(
                SendMessageRequest.builder().ern(ern).body(body).attributes(attributes).priority(priority).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", requestBody, "eqs", "send-message",
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
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "receive-messages",
                requestHeaders("receive-messages", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }

        return extractReceiveMessagesResponse(response.body());
    }

    public void deleteMessage(String receiptHandle) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                DeleteMessageRequest.builder().receiptHandle(receiptHandle).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "delete-message",
                requestHeaders("delete-message", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }
    }

    public GetMessageCountResponse getMessageCount(String ern) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(GetMessageCountRequest.builder().ern(ern).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "get-message-count",
                requestHeaders("get-message-count", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }

        return extractGetMessageCountResponse(response.body());
    }

    public void setVisibility(String messageId, long visibility) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                SetMessageVisibilityRequest.builder().messageId(messageId).visibility(visibility).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "set-visibility",
                requestHeaders("set-visibility", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }
    }

    public GetMessageAttributeResponse getMessageAttribute(String messageId, String name)
            throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                GetMessageAttributeRequest.builder().messageId(messageId).name(name).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "get-message-attribute",
                requestHeaders("get-message-attribute", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }

        return extractGetMessageAttributeResponse(response.body());
    }

    public GetMessageMetadataResponse getMessageMetadata(String messageId) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                GetMessageMetadataRequest.builder().messageId(messageId).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "get-message-metadata",
                requestHeaders("get-message-metadata", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }

        return extractGetMessageMetadataResponse(response.body());
    }

    private static SendMessageResponse extractSendMessageResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return SendMessageResponse.builder().messageId(textOrNull(root, "messageId"))
                .md5Body(textOrNull(root, "md5Body")).md5Attributes(textOrNull(root, "md5Attributes")).build();
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

    private static ListMessagesResponse extractListMessagesResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return ListMessagesResponse.builder().messages(toMessageList(root.get("messages")))
                .total(root.path("total").asLong(0)).build();
    }

    private static GetQueueMetadataResponse extractGetQueueMetadataResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return GetQueueMetadataResponse.builder().region(textOrNull(root, "region")).accountId(textOrNull(root, "accountId"))
                .owner(textOrNull(root, "owner")).nameSpace(textOrNull(root, "nameSpace")).name(textOrNull(root, "name"))
                .ern(textOrNull(root, "ern")).size(root.path("size").asLong(0)).messages(root.path("messages").asLong(0)).build();
    }

    private static GetMessageAttributeResponse extractGetMessageAttributeResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        JsonNode valueNode = root.get("value");
        Variant value = valueNode == null ? null : new Variant(textOrNull(valueNode, "type"), textOrNull(valueNode, "value"));
        return GetMessageAttributeResponse.builder().messageId(textOrNull(root, "messageId")).name(textOrNull(root, "name"))
                .value(value).build();
    }

    private static GetMessageMetadataResponse extractGetMessageMetadataResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return GetMessageMetadataResponse.builder().messageId(textOrNull(root, "messageId")).queueErn(textOrNull(root, "queueErn"))
                .receiptHandle(textOrNull(root, "receiptHandle")).status(textOrNull(root, "status"))
                .priority(textOrNull(root, "priority"))
                .size(root.path("size").asLong(0)).receivedCount(root.path("receivedCount").asLong(0))
                .visibilityTimeout(root.path("visibilityTimeout").asLong(0)).contentType(textOrNull(root, "contentType"))
                .md5Body(textOrNull(root, "md5Body")).md5Attributes(textOrNull(root, "md5Attributes"))
                .created(textOrNull(root, "created")).modified(textOrNull(root, "modified")).build();
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
        return ReceiveMessagesResponse.builder().messages(toMessageList(root.get("messages")))
                .total(root.path("total").asLong(0)).build();
    }

    private static List<Message> toMessageList(JsonNode messagesNode) {
        List<Message> messages = new ArrayList<>();
        if (messagesNode != null && messagesNode.isArray()) {
            for (JsonNode messageNode : messagesNode) {
                messages.add(new Message(
                        textOrNull(messageNode, "ern"),
                        textOrNull(messageNode, "queueErn"),
                        textOrNull(messageNode, "messageId"),
                        textOrNull(messageNode, "status"),
                        textOrNull(messageNode, "priority"),
                        textOrNull(messageNode, "body"),
                        textOrNull(messageNode, "md5sum"),
                        textOrNull(messageNode, "receiptHandle"),
                        toVariantMap(messageNode.get("attributes")),
                        textOrNull(messageNode, "md5Attributes"),
                        textOrNull(messageNode, "lastReceived"),
                        textOrNull(messageNode, "created"),
                        textOrNull(messageNode, "modified")));
            }
        }
        return messages;
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
