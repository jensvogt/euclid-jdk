package de.jensvogt.euclid.module.ens;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jensvogt.euclid.auth.SigV4;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.dto.com.Variant;
import de.jensvogt.euclid.dto.ens.AddTopicTagRequest;
import de.jensvogt.euclid.dto.ens.CreateTopicRequest;
import de.jensvogt.euclid.dto.ens.CreateTopicResponse;
import de.jensvogt.euclid.dto.ens.DeleteTopicRequest;
import de.jensvogt.euclid.dto.ens.DeleteTopicTagRequest;
import de.jensvogt.euclid.dto.ens.GetMessageAttributeRequest;
import de.jensvogt.euclid.dto.ens.GetMessageAttributeResponse;
import de.jensvogt.euclid.dto.ens.GetMessageCountRequest;
import de.jensvogt.euclid.dto.ens.GetMessageCountResponse;
import de.jensvogt.euclid.dto.ens.GetTopicErnRequest;
import de.jensvogt.euclid.dto.ens.GetTopicErnResponse;
import de.jensvogt.euclid.dto.ens.GetTopicMetadataRequest;
import de.jensvogt.euclid.dto.ens.GetTopicMetadataResponse;
import de.jensvogt.euclid.dto.ens.ListMessagesRequest;
import de.jensvogt.euclid.dto.ens.ListMessagesResponse;
import de.jensvogt.euclid.dto.ens.ListSubscriptionsRequest;
import de.jensvogt.euclid.dto.ens.ListSubscriptionsResponse;
import de.jensvogt.euclid.dto.ens.ListTopicsRequest;
import de.jensvogt.euclid.dto.ens.ListTopicsResponse;
import de.jensvogt.euclid.dto.ens.PublishMessageRequest;
import de.jensvogt.euclid.dto.ens.PublishMessageResponse;
import de.jensvogt.euclid.dto.ens.PurgeAllTopicsRequest;
import de.jensvogt.euclid.dto.ens.PurgeTopicRequest;
import de.jensvogt.euclid.dto.ens.SetMessageAttributeRequest;
import de.jensvogt.euclid.dto.ens.SetTopicTagRequest;
import de.jensvogt.euclid.dto.ens.SubscribeRequest;
import de.jensvogt.euclid.dto.ens.SubscribeResponse;
import de.jensvogt.euclid.dto.ens.UnsubscribeRequest;
import de.jensvogt.euclid.dto.ens.model.Message;
import de.jensvogt.euclid.dto.ens.model.Subscription;
import de.jensvogt.euclid.dto.ens.model.Topic;
import de.jensvogt.euclid.exception.EuclidServiceException;
import de.jensvogt.euclid.http.EuclidHttpClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ENS (pub/sub topic) operations for an authenticated {@link de.jensvogt.euclid.module.eam.EuclidSession}.
 * Mirrors euclid-cli's {@code EnsCli}.
 */
public final class EuclidEns {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String TARGET = "ens";

    private final String baseUrl;
    private final String token;
    private final String region;
    private final String accountId;
    private final String userId;
    private final String accessKeyId;
    private final String secretAccessKey;
    private final String nameSpace;
    private final EuclidHttpClient httpClient;

    /**
     * Constructs an instance of the EuclidEns class with the specified parameters for
     * interacting with the Euclid API.
     *
     * @param baseUrl         the base URL of the Euclid API
     * @param token           a bearer token for authentication
     * @param region          the region of the Euclid service instance
     * @param accountId       the account ID used for accessing the Euclid service
     * @param userId          the user ID associated with the Euclid service
     * @param accessKeyId     the access key ID for SigV4 authentication
     * @param secretAccessKey the secret access key for SigV4 authentication
     * @param caCertPath      path to a custom Certificate Authority (CA) certificate file
     *                        for secure HTTPS connections
     * @param nameSpace       the session's active namespace, or {@code null}/empty if unscoped -
     *                        sent as {@code x-euclid-namespace}; without it, every topic this
     *                        client creates or looks up lands in the unnamed/default namespace
     *                        regardless of what namespace the session was scoped to at login
     */
    public EuclidEns(String baseUrl, String token, String region, String accountId, String userId,
                      String accessKeyId, String secretAccessKey, String caCertPath, String nameSpace) {
        this.baseUrl = baseUrl;
        this.token = token;
        this.region = region;
        this.accountId = accountId;
        this.userId = userId;
        this.accessKeyId = accessKeyId;
        this.nameSpace = nameSpace;
        this.secretAccessKey = secretAccessKey;
        this.httpClient = new EuclidHttpClient(caCertPath);
    }

    /**
     * Creates a topic with the specified name and the default maximum message length (1MB).
     *
     * @param name the name of the topic to create
     * @return a {@code CreateTopicResponse} containing details of the created topic
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public CreateTopicResponse createTopic(String name) throws IOException, InterruptedException {
        return createTopic(name, 1024 * 1024);
    }

    /**
     * Creates a new topic with the specified name and maximum message length.
     *
     * @param name             the name of the topic to create
     * @param maxMessageLength the maximum allowed size, in bytes, of a single message
     * @return a {@code CreateTopicResponse} containing details of the created topic
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public CreateTopicResponse createTopic(String name, long maxMessageLength) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                CreateTopicRequest.builder().name(name).maxMessageLength(maxMessageLength).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "ens", "create-topic",
                requestHeaders("create-topic", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("ens", "create-topic", response.statusCode(), response.body());
        }

        return extractCreateTopicResponse(response.body());
    }

    /**
     * Retrieves a list of all available topics using default filtering, pagination, and sorting.
     *
     * @return the topics
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ListTopicsResponse listTopics() throws IOException, InterruptedException {
        return listTopics("", 10, 0, "name");
    }

    /**
     * Retrieves a paginated and optionally filtered list of topics.
     *
     * @param prefix     topic name prefix
     * @param pageSize   page size
     * @param pageIndex  page index (zero-based)
     * @param sortColumn sorting column
     * @return the matching topics
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ListTopicsResponse listTopics(String prefix, long pageSize, long pageIndex, String sortColumn)
            throws IOException, InterruptedException {
        return listTopics(prefix, pageSize, pageIndex, sortColumn, "asc");
    }

    /**
     * Lists topics, optionally filtered by name prefix and paginated, in a chosen sort direction.
     *
     * @param prefix only topics whose name starts with this prefix are returned
     * @param pageSize the maximum number of topics to return in a single page
     * @param pageIndex the zero-based index of the page to return
     * @param sortColumn the column results are sorted by
     * @param sortDirection the direction to sort in, {@code "asc"} or {@code "desc"}
     * @return the matching topics
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ListTopicsResponse listTopics(String prefix, long pageSize, long pageIndex, String sortColumn,
                                  String sortDirection) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                ListTopicsRequest.builder().prefix(prefix).pageSize(pageSize).pageIndex(pageIndex)
                        .sortColumn(sortColumn).sortDirection(sortDirection).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "ens", "list-topics",
                requestHeaders("list-topics", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("ens", "list-topics", response.statusCode(), response.body());
        }

        return extractListTopicsResponse(response.body());
    }

    /**
     * Resolves a topic's ERN by name.
     *
     * @param name the topic name
     * @return a {@code GetTopicErnResponse} containing the topic's ERN
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public GetTopicErnResponse getTopicErn(String name) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(GetTopicErnRequest.builder().name(name).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "ens", "get-topic-ern",
                requestHeaders("get-topic-ern", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("ens", "get-topic-ern", response.statusCode(), response.body());
        }

        return extractGetTopicErnResponse(response.body());
    }

    /**
     * Retrieves metadata for the specified topic.
     *
     * @param ern the ERN of the topic
     * @return a {@code GetTopicMetadataResponse} containing the topic's metadata
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public GetTopicMetadataResponse getTopicMetadata(String ern) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(GetTopicMetadataRequest.builder().ern(ern).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "ens", "get-topic-metadata",
                requestHeaders("get-topic-metadata", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("ens", "get-topic-metadata", response.statusCode(), response.body());
        }

        return extractGetTopicMetadataResponse(response.body());
    }

    /**
     * Deletes a topic identified by its ERN.
     *
     * @param ern the ERN of the topic to delete
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public void deleteTopic(String ern) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(DeleteTopicRequest.builder().ern(ern).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "ens", "delete-topic",
                requestHeaders("delete-topic", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("ens", "delete-topic", response.statusCode(), response.body());
        }
    }

    /**
     * Deletes all messages from a topic identified by its ERN.
     *
     * @param ern the ERN of the topic to purge
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public void purgeTopic(String ern) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(PurgeTopicRequest.builder().ern(ern).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "ens", "purge-topic",
                requestHeaders("purge-topic", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("ens", "purge-topic", response.statusCode(), response.body());
        }
    }

    /**
     * Deletes all messages from every topic in this instance's region/account, across every namespace.
     *
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public void purgeAllTopics() throws IOException, InterruptedException {
        purgeAllTopics(region, accountId, "");
    }

    /**
     * Deletes all messages from every topic in the given region/account, across every namespace.
     *
     * @param region    the region of the topics to purge
     * @param accountId the account ID of the topics to purge
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public void purgeAllTopics(String region, String accountId) throws IOException, InterruptedException {
        purgeAllTopics(region, accountId, "");
    }

    /**
     * Deletes all messages from every topic in the given region/account, optionally restricted to
     * a single namespace.
     *
     * @param region    the region of the topics to purge
     * @param accountId the account ID of the topics to purge
     * @param nameSpace the namespace to restrict the purge to, or empty to purge every namespace
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public void purgeAllTopics(String region, String accountId, String nameSpace) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                PurgeAllTopicsRequest.builder().region(region).accountId(accountId).nameSpace(nameSpace).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "ens", "purge-all-topics",
                requestHeaders("purge-all-topics", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("ens", "purge-all-topics", response.statusCode(), response.body());
        }
    }

    /**
     * Publishes a message with the specified body and no attributes to a topic.
     *
     * @param ern  the ERN of the topic to publish to
     * @param body the message body
     * @return a {@code PublishMessageResponse} containing the published message's details
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public PublishMessageResponse publishMessage(String ern, String body) throws IOException, InterruptedException {
        return publishMessage(ern, body, new HashMap<>());
    }

    /**
     * Publishes a message with the specified body and attributes to a topic.
     *
     * @param ern        the ERN of the topic to publish to
     * @param body       the message body
     * @param attributes typed message attributes
     * @return a {@code PublishMessageResponse} containing the published message's details
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public PublishMessageResponse publishMessage(String ern, String body, Map<String, Variant> attributes)
            throws IOException, InterruptedException {
        String requestBody = OBJECT_MAPPER.writeValueAsString(
                PublishMessageRequest.builder().ern(ern).body(body).attributes(attributes).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", requestBody, "ens", "publish-message",
                requestHeaders("publish-message", requestBody));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("ens", "publish-message", response.statusCode(), response.body());
        }

        return extractPublishMessageResponse(response.body());
    }

    /**
     * Lists a topic's messages without receiving them, using default pagination and sorting.
     *
     * @param topicErn the ERN of the topic whose messages are listed
     * @return a {@code ListMessagesResponse} containing the listed messages and total count
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ListMessagesResponse listMessages(String topicErn) throws IOException, InterruptedException {
        return listMessages(topicErn, 10, 0, "created");
    }

    /**
     * Lists a topic's messages without receiving them, paginated.
     *
     * @param topicErn   the ERN of the topic whose messages are listed
     * @param pageSize   page size
     * @param pageIndex  page index (zero-based)
     * @param sortColumn sorting column
     * @return a {@code ListMessagesResponse} containing the listed messages and total count
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ListMessagesResponse listMessages(String topicErn, long pageSize, long pageIndex, String sortColumn)
            throws IOException, InterruptedException {
        return listMessages(topicErn, pageSize, pageIndex, sortColumn, "asc");
    }

    /**
     * Lists a topic's messages without receiving them, paginated, in a chosen sort direction.
     *
     * @param topicErn the ERN of the topic whose messages are listed
     * @param pageSize the maximum number of messages to return in a single page
     * @param pageIndex the zero-based index of the page to return
     * @param sortColumn the column results are sorted by
     * @param sortDirection the direction to sort in, {@code "asc"} or {@code "desc"}
     * @return a {@code ListMessagesResponse} containing the messages and their total
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ListMessagesResponse listMessages(String topicErn, long pageSize, long pageIndex, String sortColumn,
                                             String sortDirection) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                ListMessagesRequest.builder().topicErn(topicErn).pageSize(pageSize).pageIndex(pageIndex)
                        .sortColumn(sortColumn).sortDirection(sortDirection).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "ens", "list-messages",
                requestHeaders("list-messages", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("ens", "list-messages", response.statusCode(), response.body());
        }

        return extractListMessagesResponse(response.body());
    }

    /**
     * Retrieves the message counters for the specified topic.
     *
     * @param ern the ERN of the topic
     * @return a {@code GetMessageCountResponse} containing the topic's message counters
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public GetMessageCountResponse getMessageCount(String ern) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(GetMessageCountRequest.builder().ern(ern).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "ens", "get-message-count",
                requestHeaders("get-message-count", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("ens", "get-message-count", response.statusCode(), response.body());
        }

        return extractGetMessageCountResponse(response.body());
    }

    /**
     * Retrieves a single message attribute by key.
     *
     * @param messageId the unique identifier of the message
     * @param key       the key of the attribute to retrieve
     * @return a {@code GetMessageAttributeResponse} containing the requested attribute
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public GetMessageAttributeResponse getMessageAttribute(String messageId, String key)
            throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                GetMessageAttributeRequest.builder().messageId(messageId).key(key).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "ens", "get-message-attribute",
                requestHeaders("get-message-attribute", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("ens", "get-message-attribute", response.statusCode(), response.body());
        }

        return extractGetMessageAttributeResponse(response.body());
    }

    /**
     * Sets the value of a single message attribute, creating it if it doesn't exist yet.
     *
     * @param messageId the unique identifier of the message to update
     * @param key       the key of the attribute to set
     * @param value     the attribute's new value
     * @return a {@code GetMessageAttributeResponse} reflecting the attribute's new value
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public GetMessageAttributeResponse setMessageAttribute(String messageId, String key, Variant value)
            throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                SetMessageAttributeRequest.builder().messageId(messageId).key(key).value(value).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "ens", "set-message-attribute",
                requestHeaders("set-message-attribute", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("ens", "set-message-attribute", response.statusCode(), response.body());
        }

        return extractGetMessageAttributeResponse(response.body());
    }

    /**
     * Adds a tag to a topic.
     *
     * @param ern   the ERN of the topic to tag
     * @param key   the tag key
     * @param value the tag value
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public void addTopicTag(String ern, String key, String value) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(AddTopicTagRequest.builder().ern(ern).key(key).value(value).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "ens", "add-topic-tag",
                requestHeaders("add-topic-tag", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("ens", "add-topic-tag", response.statusCode(), response.body());
        }
    }

    /**
     * Sets the value of an existing topic tag. The tag must already exist.
     *
     * @param ern   the ERN of the topic to tag
     * @param key   the tag key
     * @param value the tag's new value
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public void setTopicTag(String ern, String key, String value) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(SetTopicTagRequest.builder().ern(ern).key(key).value(value).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "ens", "set-topic-tag",
                requestHeaders("set-topic-tag", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("ens", "set-topic-tag", response.statusCode(), response.body());
        }
    }

    /**
     * Deletes a tag from a topic.
     *
     * @param ern the ERN of the topic to untag
     * @param key the tag key to delete
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public void deleteTopicTag(String ern, String key) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(DeleteTopicTagRequest.builder().ern(ern).key(key).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "ens", "delete-topic-tag",
                requestHeaders("delete-topic-tag", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("ens", "delete-topic-tag", response.statusCode(), response.body());
        }
    }

    /**
     * Subscribes an EQS queue to a topic using the default "SQS" delivery protocol.
     *
     * @param sourceErn the ERN of the topic messages are published to
     * @param targetErn the ERN of the EQS queue to deliver to
     * @return a {@code SubscribeResponse} describing the newly created subscription
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public SubscribeResponse subscribe(String sourceErn, String targetErn) throws IOException, InterruptedException {
        return subscribe(sourceErn, "SQS", targetErn);
    }

    /**
     * Subscribes a target resource to a topic, so messages published to the topic are also
     * delivered to the target. Only type "SQS" is supported for now, so {@code targetErn} must be
     * the ERN of an EQS queue.
     *
     * @param sourceErn the ERN of the topic messages are published to
     * @param type      the delivery protocol; only "SQS" is currently supported
     * @param targetErn the ERN of the delivery target
     * @return a {@code SubscribeResponse} describing the newly created subscription
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public SubscribeResponse subscribe(String sourceErn, String type, String targetErn) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                SubscribeRequest.builder().sourceErn(sourceErn).type(type).targetErn(targetErn).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "ens", "subscribe",
                requestHeaders("subscribe", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("ens", "subscribe", response.statusCode(), response.body());
        }

        return extractSubscribeResponse(response.body());
    }

    /**
     * Deletes a subscription, identified by the ERN returned by {@link #subscribe(String, String)}.
     * Deleting an ERN with no matching subscription is not an error.
     *
     * @param ern the ERN of the subscription to delete
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public void unsubscribe(String ern) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(UnsubscribeRequest.builder().ern(ern).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "ens", "unsubscribe",
                requestHeaders("unsubscribe", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("ens", "unsubscribe", response.statusCode(), response.body());
        }
    }

    /**
     * Lists the subscriptions of a topic.
     *
     * @param topicErn the ERN of the topic
     * @return a {@code ListSubscriptionsResponse} containing the topic's subscriptions and total count
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ListSubscriptionsResponse listSubscriptions(String topicErn) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(ListSubscriptionsRequest.builder().topicErn(topicErn).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "ens", "list-subscriptions",
                requestHeaders("list-subscriptions", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("ens", "list-subscriptions", response.statusCode(), response.body());
        }

        return extractListSubscriptionsResponse(response.body());
    }

    private static CreateTopicResponse extractCreateTopicResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return CreateTopicResponse.builder().name(textOrNull(root, "name")).ern(textOrNull(root, "ern")).build();
    }

    private static GetTopicErnResponse extractGetTopicErnResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return GetTopicErnResponse.builder().name(textOrNull(root, "name")).ern(textOrNull(root, "ern")).build();
    }

    private static GetTopicMetadataResponse extractGetTopicMetadataResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return GetTopicMetadataResponse.builder().region(textOrNull(root, "region")).accountId(textOrNull(root, "accountId"))
                .owner(textOrNull(root, "owner")).nameSpace(textOrNull(root, "nameSpace")).name(textOrNull(root, "name"))
                .ern(textOrNull(root, "ern")).size(root.path("size").asLong(0)).messages(root.path("messages").asLong(0)).build();
    }

    private static PublishMessageResponse extractPublishMessageResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return PublishMessageResponse.builder().messageId(textOrNull(root, "messageId"))
                .md5Body(textOrNull(root, "md5Body")).md5Attributes(textOrNull(root, "md5Attributes")).build();
    }

    private static ListMessagesResponse extractListMessagesResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return ListMessagesResponse.builder().messages(toMessageList(root.get("messages")))
                .total(root.path("total").asLong(0)).build();
    }

    private static GetMessageCountResponse extractGetMessageCountResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return GetMessageCountResponse.builder().ern(textOrNull(root, "ern")).available(root.path("available").asLong(0))
                .send(root.path("send").asLong(0)).resend(root.path("resend").asLong(0)).build();
    }

    private static GetMessageAttributeResponse extractGetMessageAttributeResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        JsonNode valueNode = root.get("value");
        Variant value = valueNode == null ? null : new Variant(textOrNull(valueNode, "type"), textOrNull(valueNode, "value"));
        return GetMessageAttributeResponse.builder().messageId(textOrNull(root, "messageId")).key(textOrNull(root, "key"))
                .value(value).build();
    }

    private static SubscribeResponse extractSubscribeResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return SubscribeResponse.builder().ern(textOrNull(root, "ern")).sourceErn(textOrNull(root, "sourceErn"))
                .type(textOrNull(root, "type")).targetErn(textOrNull(root, "targetErn")).build();
    }

    private static ListSubscriptionsResponse extractListSubscriptionsResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return ListSubscriptionsResponse.builder().subscriptions(toSubscriptionList(root.get("subscriptions")))
                .total(root.path("total").asLong(0)).build();
    }

    private static List<Subscription> toSubscriptionList(JsonNode subscriptionsNode) {
        List<Subscription> subscriptions = new ArrayList<>();
        if (subscriptionsNode != null && subscriptionsNode.isArray()) {
            for (JsonNode subscriptionNode : subscriptionsNode) {
                subscriptions.add(new Subscription(
                        textOrNull(subscriptionNode, "ern"),
                        textOrNull(subscriptionNode, "sourceErn"),
                        textOrNull(subscriptionNode, "type"),
                        textOrNull(subscriptionNode, "targetErn"),
                        textOrNull(subscriptionNode, "created"),
                        textOrNull(subscriptionNode, "modified")));
            }
        }
        return subscriptions;
    }

    /**
     * Builds the headers for one request/action: routing headers plus authentication.
     * <p>
     * Signs with SigV4 (accessKeyId/secretAccessKey) when both are configured, mirroring how
     * euclid-cli authenticates service calls; falls back to the bearer token otherwise.
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
        if (nameSpace != null && !nameSpace.isEmpty()) {
            headers.put("x-euclid-namespace", nameSpace);
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

    private static List<Message> toMessageList(JsonNode messagesNode) {
        List<Message> messages = new ArrayList<>();
        if (messagesNode != null && messagesNode.isArray()) {
            for (JsonNode messageNode : messagesNode) {
                messages.add(new Message(
                        textOrNull(messageNode, "ern"),
                        textOrNull(messageNode, "topicErn"),
                        textOrNull(messageNode, "messageId"),
                        textOrNull(messageNode, "status"),
                        textOrNull(messageNode, "body"),
                        textOrNull(messageNode, "md5Body"),
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

    private static ListTopicsResponse extractListTopicsResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        JsonNode topicsNode = root.get("topics");
        List<Topic> topics = new ArrayList<>();
        if (topicsNode != null && topicsNode.isArray()) {
            for (JsonNode topicNode : topicsNode) {
                topics.add(new Topic(
                        textOrNull(topicNode, "name"),
                        textOrNull(topicNode, "owner"),
                        textOrNull(topicNode, "ern"),
                        toStringMap(topicNode.get("tags")),
                        topicNode.path("size").asLong(0),
                        topicNode.path("messages").asLong(0),
                        topicNode.path("maxMessageLength").asLong(1024 * 1024),
                        textOrNull(topicNode, "created"),
                        textOrNull(topicNode, "modified")));
            }
        }
        return ListTopicsResponse.builder().topics(topics).total(root.path("total").asLong(0)).build();
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
