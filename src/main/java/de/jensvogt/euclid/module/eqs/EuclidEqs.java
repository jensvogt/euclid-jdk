package de.jensvogt.euclid.module.eqs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jensvogt.euclid.auth.CredentialsFileTokens;
import de.jensvogt.euclid.auth.TokenRefreshable;
import de.jensvogt.euclid.dto.com.Variant;
import de.jensvogt.euclid.dto.eqs.AddQueueTagRequest;
import de.jensvogt.euclid.dto.eqs.CreateQueueRequest;
import de.jensvogt.euclid.dto.eqs.CreateQueueResponse;
import de.jensvogt.euclid.dto.eqs.DeleteMessageRequest;
import de.jensvogt.euclid.dto.eqs.DeleteQueueRequest;
import de.jensvogt.euclid.dto.eqs.DeleteQueueTagRequest;
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
import de.jensvogt.euclid.dto.eqs.ListQueueResponse;
import de.jensvogt.euclid.dto.eqs.PurgeAllQueuesRequest;
import de.jensvogt.euclid.dto.eqs.PurgeQueueRequest;
import de.jensvogt.euclid.dto.eqs.ReceiveMessagesRequest;
import de.jensvogt.euclid.dto.eqs.ReceiveMessagesResponse;
import de.jensvogt.euclid.dto.eqs.SendMessageRequest;
import de.jensvogt.euclid.dto.eqs.SendMessageResponse;
import de.jensvogt.euclid.dto.eqs.SetMessageAttributeRequest;
import de.jensvogt.euclid.dto.eqs.SetMessageVisibilityRequest;
import de.jensvogt.euclid.dto.eqs.SetQueueTagRequest;
import de.jensvogt.euclid.dto.eqs.model.Message;
import de.jensvogt.euclid.dto.eqs.model.Queue;
import de.jensvogt.euclid.http.EuclidHttpClient;
import de.jensvogt.euclid.ws.EuclidEventStream;
import de.jensvogt.euclid.exception.EuclidServiceException;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.auth.SigningScheme;
import de.jensvogt.euclid.auth.SigningSchemeSelectable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * EQS operations for an authenticated {@link de.jensvogt.euclid.module.eam.EuclidSession}.
 */
public final class EuclidEqs implements TokenRefreshable, SigningSchemeSelectable {

    /**
     * A statically instantiated, thread-safe Jackson {@code ObjectMapper} used for
     * JSON serialization and deserialization throughout the application.
     * <p>
     * This mapper serves as the primary utility for converting between Java objects
     * and JSON representations, as well as providing additional configuration options
     * for handling JSON-specific operations such as formatting, custom serializers,
     * and deserializers.
     * <p>
     * Its static nature ensures reuse across multiple calls, improving performance
     * by avoiding repeated instantiation.
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Defines the polling interval, in milliseconds, used for receiving messages from a queue.
     * <p>
     * This constant specifies the time between consecutive polling attempts when consuming
     * messages. Adjusting this value can impact the trade-off between resource usage and
     * latency in message processing. The value is fixed at 500 milliseconds.
     */
    private static final long RECEIVE_POLL_INTERVAL_MS = 500;

    /**
     * Represents the constant target identifier used within the EuclidEqs class.
     * This specific string value is tied to the term "eqs" and may serve as an
     * identifier, namespace, or discriminator for operations related to its scope.
     * <p>
     * Being a static and final constant, the value of TARGET remains unchanged
     * throughout the runtime of the application.
     */
    private static final String TARGET = "eqs";

    /**
     * The base URL used as the foundational endpoint for interacting with the Euclid service APIs.
     * This URL serves as the root address for all API requests and is typically specific to the
     * region, environment, or deployment of the Euclid service being accessed.
     * Must be a valid URI format.
     */
    private final String baseUrl;

    /**
     * Supplies the bearer token for each request, used when no SigV4 access key is configured.
     *
     * <p>A supplier rather than a string so that a token which expires can be replaced without
     * rebuilding the client - see {@link TokenRefreshable#token(Supplier)}. A client built inside an
     * application euclid deployed follows the credentials file euclid rewrites; anywhere else it
     * holds a supplier returning the token it was given - see
     * {@link CredentialsFileTokens#forClient(String, String)}.
     */
    private volatile Supplier<String> token;

    /**
     * Represents the geographical region or location where the operations will be performed.
     * This field is typically used to specify a particular AWS region or datacenter for the service.
     * It is immutable and set during the initialization of the class.
     */
    private final String region;

    /**
     * The unique identifier for an account used to interact with queues via the EuclidEqs service.
     * This identifier is typically passed as a parameter to various operations within the service
     * to specify the account context under which the operation is executed. It is a required value
     * for performing actions such as queue management, message operations, and account-specific
     * configurations.
     */
    private final String accountId;

    /**
     * Represents the unique identifier for a user in the EuclidEqs system.
     * This identifier is used to associate actions, resources, and operations
     * with a specific user within the context of the application.
     */
    private final String userId;

    /**
     * Represents the access key ID used for authentication in the EuclidEqs service.
     * This key, along with the corresponding secret access key, is utilized for
     * signing requests with SigV4 for secure communication with the service.
     * It serves as a unique identifier for the credentials of the requesting user.
     * <p>
     * The access key ID is configured during the initialization of the EuclidEqs instance
     * and is required when the service is set to use SigV4-based authentication instead
     * of the bearer token.
     * <p>
     * This field is immutable and cannot be changed after the object is constructed.
     */
    private final String accessKeyId;

    /**
     * The secret access key used for authenticated communication with the service.
     * This key, in conjunction with the access key ID, is used to sign requests
     * and ensure secure access to resources.
     *
     * <p>Be cautious when handling this variable, as the exposure of the secret access key
     * could lead to unauthorized access to sensitive resources. It should be stored securely
     * and never logged or exposed in plaintext.
     */
    private final String secretAccessKey;

    /**
     * Represents an HTTP client used for sending and receiving HTTP requests
     * and responses in the EuclidEqs service. This client is responsible
     * for managing the underlying HTTP communication with the service's endpoints.
     * <p>
     * This instance is configured to support authenticated requests, utilizing
     * either SigV4 signing with access key credentials or a bearer token,
     * based on the current authentication setup provided during initialization.
     * <p>
     * The httpClient is immutable and initialized during the construction of
     * the EuclidEqs instance, ensuring a consistent configuration throughout
     * the lifecycle of the object.
     */
    private final EuclidHttpClient httpClient;

    /**
     * Path to a custom Certificate Authority (CA) certificate file for secure HTTPS connections,
     * retained (rather than only handed to {@link #httpClient}) so a {@link EuclidEventStream}
     * can be built lazily with the same TLS trust settings the first time {@link #receiveMessages}
     * needs one.
     */
    private final String caCertPath;

    /**
     * The session's active namespace, or {@code null}/empty if unscoped. Sent as the
     * {@code x-euclid-namespace} header on every request, exactly as {@link
     * de.jensvogt.euclid.module.eam.EuclidSession} does for its own EAM calls - without it, every
     * queue this client creates or looks up lands in the unnamed/default namespace regardless of
     * what namespace the session was scoped to at login.
     */
    private final String nameSpace;

    /**
     * The scheme requests are signed with when an access key is configured.
     *
     * <p>Defaults to SigV4, which is what euclid has always accepted; a caller pointed at a server
     * that understands RFC 9421 switches it with {@link #signingScheme(SigningScheme)}. Volatile
     * because that call can come from a different thread than the requests it affects.
     */
    private volatile SigningScheme signingScheme = SigningScheme.SIGV4;

    /**
     * Lazily-created websocket connection used by {@link #receiveMessages} to wake up as soon as
     * an "eqs.message.sent" event arrives for the queue being waited on, instead of only polling
     * every {@link #RECEIVE_POLL_INTERVAL_MS}. {@code null} until first needed.
     */
    private volatile EuclidEventStream eventStream;

    /**
     * Set once a websocket connection attempt fails, so {@link #receiveMessages} falls back to
     * plain polling for the rest of this instance's lifetime instead of retrying (and paying the
     * connect timeout) on every wait iteration.
     */
    private volatile boolean webSocketUnavailable;

    /**
     * Constructs an instance of the EuclidEqs class with the specified parameters for
     * interacting with the Euclid API.
     *
     * @param baseUrl        The base URL of the Euclid API.
     * @param token          A bearer token for authentication.
     * @param region         The region of the Euclid service instance.
     * @param accountId      The account ID used for accessing the Euclid service.
     * @param userId         The user ID associated with the Euclid service.
     * @param accessKeyId    The access key ID for SigV4 authentication.
     * @param secretAccessKey The secret access key for SigV4 authentication.
     * @param caCertPath     Path to a custom Certificate Authority (CA) certificate file
     *                       for secure HTTPS connections.
     * @param nameSpace      The session's active namespace, or {@code null}/empty if unscoped.
     */
    public EuclidEqs(String baseUrl, String token, String region, String accountId, String userId,
                     String accessKeyId, String secretAccessKey, String caCertPath, String nameSpace) {
        this.baseUrl = baseUrl;
        this.token = CredentialsFileTokens.forClient(token, userId);
        this.region = region;
        this.accountId = accountId;
        this.userId = userId;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.caCertPath = caCertPath;
        this.nameSpace = nameSpace;
        // The header factory is what lets a request whose token or signature expired in flight be
        // built again and sent once more - see EuclidHttpClient#headerFactory.
        this.httpClient = new EuclidHttpClient(caCertPath).headerFactory(this::requestHeaders);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void token(Supplier<String> token) {
        this.token = Objects.requireNonNull(token, "token supplier must not be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void signingScheme(SigningScheme signingScheme) {
        this.signingScheme = Objects.requireNonNull(signingScheme, "signing scheme must not be null");
    }

    /**
     * Retrieves a list of all available queues.
     *
     * @return A list of Queue objects representing the queues currently available.
     * @throws IOException If an I/O error occurs during the operation.
     * @throws InterruptedException If the operation is interrupted.
     */
    public ListQueueResponse listQueues() throws IOException, InterruptedException {
        return listQueues("", 10, 0, "name");
    }

    /**
     * Retrieves a paginated and optionally filtered list of queues.
     *
     * @param prefix A string to filter queues by their prefix. Use null or an empty string for no filtering.
     * @param pageSize The number of queues to include in each page of the result.
     * @param pageIndex The index of the page to retrieve, starting from 0.
     * @param sortColumn The field by which the queues should be sorted. Use null or an empty string for default sorting.
     * @return A list of {@code Queue} objects representing the queues matching the specified criteria.
     * @throws IOException If an I/O error occurs during the request.
     * @throws InterruptedException If the operation is interrupted while waiting for a response.
     */
    public ListQueueResponse listQueues(String prefix, long pageSize, long pageIndex, String sortColumn)
            throws IOException, InterruptedException {
        return listQueues(prefix, pageSize, pageIndex, sortColumn, "asc");
    }

    /**
     * Retrieves a paginated and optionally filtered list of queues, in a chosen sort direction.
     *
     * @param prefix A string to filter queues by their prefix. Use null or an empty string for no filtering.
     * @param pageSize The number of queues to include in each page of the result.
     * @param pageIndex The index of the page to retrieve, starting from 0.
     * @param sortColumn The field by which the queues should be sorted. Use null or an empty string for default sorting.
     * @param sortDirection The direction to sort in, {@code "asc"} or {@code "desc"}.
     * @return A list of {@code Queue} objects representing the queues matching the specified criteria.
     * @throws IOException If an I/O error occurs during the request.
     * @throws InterruptedException If the operation is interrupted while waiting for a response.
     */
    public ListQueueResponse listQueues(String prefix, long pageSize, long pageIndex, String sortColumn,
                                        String sortDirection)
            throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                ListQueueRequest.builder().prefix(prefix).pageSize(pageSize).pageIndex(pageIndex)
                        .sortColumn(sortColumn).sortDirection(sortDirection).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "list-queues",
                requestHeaders("list-queues", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eqs", "list-queues", response.statusCode(), response.body());
        }

        return extractListQueueResponse(response.body());
    }

    /**
     * Retrieves a list of messages from a specific queue using default pagination and sorting options.
     *
     * @param queueErn The unique identifier (ARN) of the queue from which messages are to be retrieved.
     * @return A {@code ListMessagesResponse} object containing the messages retrieved from the queue.
     * @throws IOException If an I/O error occurs during the operation.
     * @throws InterruptedException If the operation is interrupted during execution.
     */
    public ListMessagesResponse listMessages(String queueErn) throws IOException, InterruptedException {
        return listMessages(queueErn, 10, 0, "created");
    }

    /**
     * Retrieves a paginated list of messages from a specified queue with optional sorting.
     *
     * @param queueErn     The unique resource name (ERN) of the queue to fetch messages from.
     * @param pageSize     The number of messages to retrieve per page.
     * @param pageIndex    The index of the page to retrieve, starting from 0.
     * @param sortColumn   The column used to sort the messages, may be null to use default sorting.
     * @return A ListMessagesResponse object containing the retrieved messages and metadata.
     * @throws IOException              If an I/O error occurs during the operation.
     * @throws InterruptedException     If the operation is interrupted.
     */
    public ListMessagesResponse listMessages(String queueErn, long pageSize, long pageIndex, String sortColumn)
            throws IOException, InterruptedException {
        return listMessages(queueErn, pageSize, pageIndex, sortColumn, "asc");
    }

    /**
     * Retrieves a paginated list of messages from a specified queue, in a chosen sort direction.
     *
     * @param queueErn      The unique resource name (ERN) of the queue to fetch messages from.
     * @param pageSize      The number of messages to retrieve per page.
     * @param pageIndex     The index of the page to retrieve, starting from 0.
     * @param sortColumn    The column used to sort the messages, may be null to use default sorting.
     * @param sortDirection The direction to sort in, {@code "asc"} or {@code "desc"}.
     * @return A ListMessagesResponse object containing the retrieved messages and metadata.
     * @throws IOException              If an I/O error occurs during the operation.
     * @throws InterruptedException     If the operation is interrupted.
     */
    public ListMessagesResponse listMessages(String queueErn, long pageSize, long pageIndex, String sortColumn,
                                             String sortDirection) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                ListMessagesRequest.builder().queueErn(queueErn).pageSize(pageSize).pageIndex(pageIndex)
                        .sortColumn(sortColumn).sortDirection(sortDirection).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "list-messages",
                requestHeaders("list-messages", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eqs", "list-messages", response.statusCode(), response.body());
        }

        return extractListMessagesResponse(response.body());
    }

    /**
     * Creates a queue with the specified name and default parameters.
     *
     * @param name the name of the queue to be created
     * @return a CreateQueueResponse object containing details of the created queue
     * @throws IOException if an I/O error occurs during queue creation
     * @throws InterruptedException if the operation is interrupted
     */
    public CreateQueueResponse createQueue(String name) throws IOException, InterruptedException {
        return createQueue(name, 30, 3, 1024 * 1024, "", 0);
    }

    /**
     * Creates a new queue with the specified properties.
     *
     * @param name           The name of the queue to be created.
     * @param visibility     The visibility timeout for the queue in seconds.
     * @param maxRetries     The maximum number of retry attempts for messages in the queue.
     * @param maxMessageLength The maximum allowed length of messages in the queue.
     * @param dlqName        The name of the dead-letter queue associated with this queue.
     * @return A CreateQueueResponse object containing details of the created queue.
     * @throws IOException              If an I/O error occurs during the operation.
     * @throws InterruptedException     If the thread executing the method is interrupted.
     */
    public CreateQueueResponse createQueue(String name, long visibility, long maxRetries, long maxMessageLength,
                                            String dlqName) throws IOException, InterruptedException {
        return createQueue(name, visibility, maxRetries, maxMessageLength, dlqName, 0);
    }

    /**
     * Creates a queue with the specified parameters.
     *
     * @param name                The name of the queue to be created.
     * @param visibility          The visibility timeout for the queue in seconds.
     * @param maxRetries          The maximum number of retry attempts for failed messages.
     * @param maxMessageLength    The maximum allowed length of messages in the queue.
     * @param dlqName             The name of the dead-letter queue associated with this queue.
     * @param delay               The delay in seconds before a message becomes visible in the queue.
     * @return                    A {@link CreateQueueResponse} object containing details of the created queue.
     * @throws IOException        If an I/O error occurs during the request.
     * @throws InterruptedException If the request is interrupted.
     */
    public CreateQueueResponse createQueue(String name, long visibility, long maxRetries, long maxMessageLength,
                                            String dlqName, long delay) throws IOException, InterruptedException {
        return createQueue(name, visibility, maxRetries, maxMessageLength, dlqName, delay, "MIDDLE");
    }

    /**
     * Creates a queue with the specified parameters and a default message priority.
     *
     * @param name                The name of the queue to be created.
     * @param visibility          The visibility timeout for the queue in seconds.
     * @param maxRetries          The maximum number of retry attempts for failed messages.
     * @param maxMessageLength    The maximum allowed length of messages in the queue.
     * @param dlqName             The name of the dead-letter queue associated with this queue.
     * @param delay               The delay in seconds before a message becomes visible in the queue.
     * @param priority            The priority every message of this queue gets unless
     *                            {@link #sendMessage(String, String, Map, String)} overrides it.
     * @return                    A {@link CreateQueueResponse} object containing details of the created queue.
     * @throws IOException        If an I/O error occurs during the request.
     * @throws InterruptedException If the request is interrupted.
     */
    public CreateQueueResponse createQueue(String name, long visibility, long maxRetries, long maxMessageLength,
                                            String dlqName, long delay, String priority)
            throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                CreateQueueRequest.builder().name(name).visibility(visibility).maxRetries(maxRetries)
                        .maxMessageLength(maxMessageLength).dlqName(dlqName).delay(delay).priority(priority).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "create-queue",
                requestHeaders("create-queue", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eqs", "create-queue", response.statusCode(), response.body());
        }

        return extractCreateQueueResponse(response.body());
    }

    /**
     * Deletes a queue specified by its ERN (Entity Resource Name).
     *
     * @param ern The Entity Resource Name (ERN) of the queue to be deleted.
     * @throws IOException If an I/O error occurs during the request.
     * @throws InterruptedException If the operation is interrupted.
     * @throws EuclidServiceException If the server responds with a failure status code.
     */
    public void deleteQueue(String ern) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(DeleteQueueRequest.builder().ern(ern).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "delete-queue",
                requestHeaders("delete-queue", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eqs", "delete-queue", response.statusCode(), response.body());
        }
    }

    /**
     * Retrieves the ARN (Amazon Resource Name) of a specific queue based on the provided name.
     * This method sends a POST request to the service endpoint to fetch the queue's details.
     *
     * @param name the name of the queue whose ARN is to be retrieved
     * @return a {@code GetQueueErnResponse} object containing details about the queue's ARN
     * @throws IOException if an I/O error occurs during the request
     * @throws InterruptedException if the operation is interrupted while waiting for the response
     */
    public GetQueueErnResponse getQueueErn(String name) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(GetQueueErnRequest.builder().name(name).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "get-queue-ern",
                requestHeaders("get-queue-ern", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eqs", "get-queue-ern", response.statusCode(), response.body());
        }

        return extractGetQueueErnResponse(response.body());
    }

    /**
     * Retrieves metadata information for the specified queue.
     *
     * @param ern The identifier of the queue whose metadata is to be retrieved.
     * @return A {@link GetQueueMetadataResponse} object containing the metadata of the specified queue.
     * @throws IOException If an I/O error occurs during the operation.
     * @throws InterruptedException If the operation is interrupted during execution.
     */
    public GetQueueMetadataResponse getQueueMetadata(String ern) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(GetQueueMetadataRequest.builder().ern(ern).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "get-queue-metadata",
                requestHeaders("get-queue-metadata", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eqs", "get-queue-metadata", response.statusCode(), response.body());
        }

        return extractGetQueueMetadataResponse(response.body());
    }

    /**
     * Purges the specified queue identified by the provided ERN (External Resource Name).
     * This method sends a request to the server to clear all messages in the queue.
     *
     * @param ern The External Resource Name of the queue to be purged. It must not be null or empty.
     * @throws IOException If there is an issue with the input/output during the process.
     * @throws InterruptedException If the thread executing the method is interrupted.
     */
    public void purgeQueue(String ern) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(PurgeQueueRequest.builder().ern(ern).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "purge-queue",
                requestHeaders("purge-queue", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eqs", "purge-queue", response.statusCode(), response.body());
        }
    }

    /**
     * Purges all message queues associated with a given region and account ID.
     * <p>
     * This method clears all messages from the queues owned by the specified
     * account within the specified region. It invokes an internal mechanism
     * to perform this operation and may throw exceptions if the process
     * encounters issues such as I/O errors or interruptions.
     *
     * @throws IOException if an I/O error occurs during the purging process.
     * @throws InterruptedException if the thread executing the operation is interrupted.
     */
    public void purgeAllQueues() throws IOException, InterruptedException {
        purgeAllQueues(region, accountId);
    }

    /**
     * Purges all message queues for the specified region and account.
     *
     * @param region    The region for which queues need to be purged.
     * @param accountId The account ID for which queues need to be purged.
     * @throws IOException          If an I/O error occurs during the operation.
     * @throws InterruptedException If the operation is interrupted.
     */
    public void purgeAllQueues(String region, String accountId) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                PurgeAllQueuesRequest.builder().region(region).accountId(accountId).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "purge-all-queues",
                requestHeaders("purge-all-queues", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eqs", "purge-all-queues", response.statusCode(), response.body());
        }
    }

    /**
     * Sends a message with the specified content to the specified endpoint.
     *
     * @param ern  The endpoint resource name to which the message will be sent.
     * @param body The content of the message to be sent.
     * @return A SendMessageResponse object containing the response details of the message.
     * @throws IOException If an I/O exception occurs during the operation.
     * @throws InterruptedException If the thread is interrupted while waiting for a response.
     */
    public SendMessageResponse sendMessage(String ern, String body) throws IOException, InterruptedException {
        return sendMessage(ern, body, new HashMap<>());
    }

    /**
     * Sends a message to the specified endpoint with the provided body and attributes.
     *
     * @param ern The endpoint ARN (Amazon Resource Name) to which the message will be sent.
     * @param body The content of the message to be sent.
     * @param attributes A map containing additional attributes for the message.
     * @return A SendMessageResponse object containing the result of the send message operation.
     * @throws IOException If an I/O error occurs during the operation.
     * @throws InterruptedException If the operation is interrupted.
     */
    public SendMessageResponse sendMessage(String ern, String body, Map<String, Variant> attributes)
            throws IOException, InterruptedException {
        return sendMessage(ern, body, attributes, "MIDDLE");
    }

    /**
     * Sends a message to the specified endpoint with the provided details.
     *
     * @param ern The endpoint resource name to which the message will be sent.
     * @param body The message content to be transmitted.
     * @param attributes A map of additional attributes to include with the message.
     * @param priority The priority level of the message.
     * @return A SendMessageResponse object containing the response details from the operation.
     * @throws IOException If an I/O error occurs during the operation.
     * @throws InterruptedException If the operation is interrupted.
     */
    public SendMessageResponse sendMessage(String ern, String body, Map<String, Variant> attributes, String priority)
            throws IOException, InterruptedException {
        String requestBody = OBJECT_MAPPER.writeValueAsString(
                SendMessageRequest.builder().ern(ern).body(body).attributes(attributes).priority(priority).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", requestBody, "eqs", "send-message",
                requestHeaders("send-message", requestBody));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eqs", "send-message", response.statusCode(), response.body());
        }

        return extractSendMessageResponse(response.body());
    }

    /**
     * Retrieves messages from a specified resource.
     *
     * @param ern The endpoint resource name (ERN) from which messages are to be received.
     * @return A ReceiveMessagesResponse object containing the messages and associated metadata.
     * @throws IOException If an I/O error occurs during the retrieval process.
     * @throws InterruptedException If the operation is interrupted while waiting for messages.
     */
    public ReceiveMessagesResponse receiveMessages(String ern) throws IOException, InterruptedException {
        return receiveMessages(ern, 10, 0);
    }

    /**
     * Retrieves messages from a message queue or similar service. The method attempts
     * to receive up to the specified maximum number of messages within an optional
     * wait time. If no messages are immediately available and a wait time is provided,
     * it will repeatedly poll until messages are received or the wait time elapses -
     * waking up early, instead of on the next poll tick, as soon as a matching
     * "eqs.message.sent" websocket event arrives for this queue (see {@link EuclidEventStream}),
     * falling back to plain polling if a websocket connection can't be established.
     *
     * @param ern         The external resource name (ERN) identifying the message queue or topic.
     * @param maxMessages The maximum number of messages to retrieve in a single call.
     * @param waitTime    The time in seconds to wait for messages if none are immediately available.
     *                    If the value is less than or equal to 0, the method will return immediately
     *                    with any available messages.
     * @return A {@code ReceiveMessagesResponse} object containing the list of messages received
     *         and the total number of messages retrieved.
     * @throws IOException              If an I/O error occurs while attempting to retrieve messages.
     * @throws InterruptedException     If the thread is interrupted while waiting for messages.
     */
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
            waitForQueueActivity(ern, Math.min(RECEIVE_POLL_INTERVAL_MS, remaining));
        }
    }

    /**
     * Waits up to {@code timeoutMillis} for either a matching "eqs.message.sent" websocket event
     * for {@code ern} to arrive (waking up immediately once it does) or the timeout to elapse -
     * whichever happens first. Falls back to a plain sleep, permanently for the rest of this
     * instance's lifetime, the first time a websocket connection can't be established.
     *
     * @param ern           the queue ERN to wait for activity on
     * @param timeoutMillis how long to wait, in milliseconds
     * @throws InterruptedException if interrupted while waiting
     */
    private void waitForQueueActivity(String ern, long timeoutMillis) throws InterruptedException {
        if (!webSocketUnavailable) {
            try {
                eventStream().awaitEvent("eqs.message.sent", Map.of("queueErn", ern), timeoutMillis);
                return;
            } catch (IOException e) {
                webSocketUnavailable = true;
            }
        }
        Thread.sleep(timeoutMillis);
    }

    private EuclidEventStream eventStream() {
        EuclidEventStream stream = eventStream;
        if (stream == null) {
            synchronized (this) {
                stream = eventStream;
                if (stream == null) {
                    stream = new EuclidEventStream(baseUrl, token.get(), region, accountId, userId, accessKeyId, secretAccessKey, caCertPath, TARGET);
                    // The stream outlives any one token by far - it is the connection that stays
                    // open for as long as this client does - so it is given the same supplier
                    // rather than the string, and reconnects with whatever is current.
                    stream.token(token);
                    eventStream = stream;
                }
            }
        }
        return stream;
    }

    /**
     * Retrieves all messages associated with the specified entity reference number (ERN).
     * This method provides a default timeout of 10 seconds for message retrieval.
     *
     * @param ern the entity reference number used to identify the messages to retrieve
     * @return a {@code ReceiveMessagesResponse} object containing the messages retrieved
     * @throws IOException if an I/O error occurs during message retrieval
     * @throws InterruptedException if the thread is interrupted during the operation
     */
    public ReceiveMessagesResponse receiveAllMessages(String ern) throws IOException, InterruptedException {
        return receiveAllMessages(ern, 10);
    }

    /**
     * Retrieves all messages from a specified endpoint in batches until no more messages are available.
     *
     * @param ern The endpoint resource name from which to receive messages.
     * @param batchSize The maximum number of messages to receive in a single batch.
     * @return A {@code ReceiveMessagesResponse} containing all messages retrieved and their total count.
     * @throws IOException If an input or output exception occurs during message retrieval.
     * @throws InterruptedException If the operation is interrupted while waiting for the response.
     */
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

    /**
     * Receives messages from the specified endpoint.
     *
     * @param ern The endpoint resource name (ERN) from which to receive messages.
     * @param maxMessages The maximum number of messages to retrieve in a single request.
     * @return A {@code ReceiveMessagesResponse} containing the messages retrieved from the endpoint.
     * @throws IOException If an I/O error occurs during the request.
     * @throws InterruptedException If the request is interrupted.
     */
    private ReceiveMessagesResponse doReceiveMessages(String ern, long maxMessages)
            throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                ReceiveMessagesRequest.builder().ern(ern).maxCount(maxMessages).waitTime(0).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "receive-messages",
                requestHeaders("receive-messages", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eqs", "receive-messages", response.statusCode(), response.body());
        }

        return extractReceiveMessagesResponse(response.body());
    }

    /**
     * Deletes a message from the queue using the provided receipt handle. This is the
     * SQS-compatible way to delete, and only works for a message that was received: the handle is
     * a lease, and the delete fails once its visibility timeout has expired.
     *
     * @param receiptHandle The unique identifier associated with the message to be deleted.
     * @throws IOException If an I/O error occurs during the deletion request.
     * @throws InterruptedException If the request is interrupted while waiting for a response.
     */
    public void deleteMessage(String receiptHandle) throws IOException, InterruptedException {
        deleteMessage(DeleteMessageRequest.builder().receiptHandle(receiptHandle).build());
    }

    /**
     * Deletes a message by its ID, including one that has never been received - a message still
     * AVAILABLE or DELAYED. This bypasses the receipt-handle lease {@link #deleteMessage(String)}
     * goes through, and is a Euclid extension with no AWS SQS equivalent.
     *
     * @param messageId The ID of the message to be deleted.
     * @throws IOException If an I/O error occurs during the deletion request.
     * @throws InterruptedException If the request is interrupted while waiting for a response.
     */
    public void deleteMessageById(String messageId) throws IOException, InterruptedException {
        deleteMessage(DeleteMessageRequest.builder().messageId(messageId).build());
    }

    /**
     * Sends a delete-message action. The server takes either a receipt handle or a message ID and
     * rejects a request carrying neither, so the two public entry points each fill in one.
     *
     * @param request the delete request, addressing the message one way or the other
     * @throws IOException If an I/O error occurs during the deletion request.
     * @throws InterruptedException If the request is interrupted while waiting for a response.
     */
    private void deleteMessage(DeleteMessageRequest request) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(request);
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "delete-message",
                requestHeaders("delete-message", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eqs", "delete-message", response.statusCode(), response.body());
        }
    }

    /**
     * Retrieves the message count for the specified identifier (ERN).
     *
     * @param ern A string representing the unique identifier for which the message count is to be retrieved.
     *            This parameter cannot be null or empty.
     * @return A {@code GetMessageCountResponse} object containing the message count and other related details.
     * @throws IOException If an I/O error occurs during the HTTP request.
     * @throws InterruptedException If the operation is interrupted while waiting for the HTTP response.
     */
    public GetMessageCountResponse getMessageCount(String ern) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(GetMessageCountRequest.builder().ern(ern).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "get-message-count",
                requestHeaders("get-message-count", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eqs", "get-message-count", response.statusCode(), response.body());
        }

        return extractGetMessageCountResponse(response.body());
    }

    /**
     * Updates the visibility timeout for a specific message identified by its messageId.
     *
     * @param messageId The unique identifier of the message for which the visibility timeout is being updated.
     * @param visibility The new visibility timeout value, in seconds.
     * @throws IOException If an I/O error occurs while sending the request or processing the response.
     * @throws InterruptedException If the operation is interrupted while waiting for a response.
     */
    public void setVisibility(String messageId, long visibility) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                SetMessageVisibilityRequest.builder().messageId(messageId).visibility(visibility).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "set-visibility",
                requestHeaders("set-visibility", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eqs", "set-visibility", response.statusCode(), response.body());
        }
    }

    /**
     * Retrieves the specified attribute of a message by its message ID and attribute name.
     *
     * @param messageId the unique identifier of the message whose attribute needs to be fetched
     * @param name the name of the attribute to retrieve for the specified message
     * @return a {@code GetMessageAttributeResponse} object containing the requested attribute's details
     * @throws IOException if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted while waiting for the response
     */
    public GetMessageAttributeResponse getMessageAttribute(String messageId, String name)
            throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                GetMessageAttributeRequest.builder().messageId(messageId).name(name).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "get-message-attribute",
                requestHeaders("get-message-attribute", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eqs", "get-message-attribute", response.statusCode(), response.body());
        }

        return extractGetMessageAttributeResponse(response.body());
    }

    /**
     * Retrieves the metadata for a specific message by its identifier.
     *
     * @param messageId the unique identifier of the message whose metadata is to be retrieved
     * @return a {@link GetMessageMetadataResponse} object containing the metadata of the requested message
     * @throws IOException if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted
     */
    public GetMessageMetadataResponse getMessageMetadata(String messageId) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                GetMessageMetadataRequest.builder().messageId(messageId).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "get-message-metadata",
                requestHeaders("get-message-metadata", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eqs", "get-message-metadata", response.statusCode(), response.body());
        }

        return extractGetMessageMetadataResponse(response.body());
    }

    /**
     * Sets the value of a single message attribute, creating it if it doesn't exist yet.
     *
     * @param messageId the unique identifier of the message to update
     * @param key       the key of the attribute to set
     * @param value     the attribute's new value
     * @return a {@code GetMessageAttributeResponse} reflecting the attribute's new value
     * @throws IOException if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted while waiting for the response
     */
    public GetMessageAttributeResponse setMessageAttribute(String messageId, String key, Variant value)
            throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                SetMessageAttributeRequest.builder().messageId(messageId).key(key).value(value).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "set-message-attribute",
                requestHeaders("set-message-attribute", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eqs", "set-message-attribute", response.statusCode(), response.body());
        }

        return extractGetMessageAttributeResponse(response.body());
    }

    /**
     * Adds a tag to a queue.
     *
     * @param ern   the ERN of the queue to tag
     * @param key   the tag key
     * @param value the tag value
     * @throws IOException if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted while waiting for the response
     */
    public void addQueueTag(String ern, String key, String value) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(AddQueueTagRequest.builder().ern(ern).key(key).value(value).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "add-queue-tag",
                requestHeaders("add-queue-tag", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eqs", "add-queue-tag", response.statusCode(), response.body());
        }
    }

    /**
     * Sets the value of an existing queue tag. The tag must already exist.
     *
     * @param ern   the ERN of the queue to tag
     * @param key   the tag key
     * @param value the tag's new value
     * @throws IOException if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted while waiting for the response
     */
    public void setQueueTag(String ern, String key, String value) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(SetQueueTagRequest.builder().ern(ern).key(key).value(value).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "set-queue-tag",
                requestHeaders("set-queue-tag", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eqs", "set-queue-tag", response.statusCode(), response.body());
        }
    }

    /**
     * Deletes a tag from a queue.
     *
     * @param ern the ERN of the queue to untag
     * @param key the tag key to delete
     * @throws IOException if an I/O error occurs during the HTTP request
     * @throws InterruptedException if the operation is interrupted while waiting for the response
     */
    public void deleteQueueTag(String ern, String key) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(DeleteQueueTagRequest.builder().ern(ern).key(key).build());
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, "eqs", "delete-queue-tag",
                requestHeaders("delete-queue-tag", body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eqs", "delete-queue-tag", response.statusCode(), response.body());
        }
    }

    /**
     * Extracts a SendMessageResponse object from the provided JSON response body.
     *
     * @param responseBody the JSON string containing the response data
     * @return a SendMessageResponse object constructed from the parsed JSON
     * @throws IOException if an error occurs while processing the JSON string
     */
    private static SendMessageResponse extractSendMessageResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return SendMessageResponse.builder().messageId(textOrNull(root, "messageId"))
                .md5Body(textOrNull(root, "md5Body")).md5Attributes(textOrNull(root, "md5Attributes")).build();
    }

    /**
     * Extracts a {@link CreateQueueResponse} object from the provided JSON response body.
     *
     * @param responseBody the JSON response body as a string
     * @return the {@link CreateQueueResponse} built from the extracted fields in the JSON response
     * @throws IOException if an error occurs while parsing the JSON response body
     */
    private static CreateQueueResponse extractCreateQueueResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return CreateQueueResponse.builder().name(textOrNull(root, "name")).ern(textOrNull(root, "ern")).build();
    }

    /**
     * Extracts a GetQueueErnResponse object from the given JSON response body.
     *
     * @param responseBody The JSON response body as a String, from which the GetQueueErnResponse will be extracted.
     * @return A GetQueueErnResponse object containing the extracted data.
     * @throws IOException If an error occurs while parsing the JSON response body.
     */
    private static GetQueueErnResponse extractGetQueueErnResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return GetQueueErnResponse.builder().name(textOrNull(root, "name")).ern(textOrNull(root, "ern")).build();
    }

    /**
     * Extracts and constructs a {@link GetMessageCountResponse} object from the given JSON response body.
     *
     * @param responseBody the JSON response body as a string from which to extract message count details
     * @return a {@link GetMessageCountResponse} object containing the parsed message count details
     * @throws IOException if an error occurs while parsing the JSON response body
     */
    private static GetMessageCountResponse extractGetMessageCountResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return GetMessageCountResponse.builder().ern(textOrNull(root, "ern")).available(root.path("available").asLong(0))
                .delayed(root.path("delayed").asLong(0)).invisible(root.path("invisible").asLong(0))
                .total(root.path("total").asLong(0)).build();
    }

    /**
     * Extracts a ListMessagesResponse object from a JSON response body.
     *
     * @param responseBody The JSON response body as a String.
     * @return A ListMessagesResponse object containing the parsed messages and total count.
     * @throws IOException If an error occurs during JSON parsing.
     */
    private static ListMessagesResponse extractListMessagesResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return ListMessagesResponse.builder().messages(toMessageList(root.get("messages")))
                .total(root.path("total").asLong(0)).build();
    }

    /**
     * Extracts and constructs a {@link GetQueueMetadataResponse} object from the provided JSON response body.
     *
     * @param responseBody The JSON response body as a string containing metadata about the queue.
     *                     It must be a valid JSON string for proper processing.
     * @return A {@link GetQueueMetadataResponse} object constructed using the extracted metadata information
     *         from the response body.
     * @throws IOException If there is an issue with parsing the response body, such as invalid JSON format.
     */
    private static GetQueueMetadataResponse extractGetQueueMetadataResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return GetQueueMetadataResponse.builder().region(textOrNull(root, "region")).accountId(textOrNull(root, "accountId"))
                .owner(textOrNull(root, "owner")).nameSpace(textOrNull(root, "nameSpace")).name(textOrNull(root, "name"))
                .ern(textOrNull(root, "ern")).size(root.path("size").asLong(0)).messages(root.path("messages").asLong(0)).build();
    }

    /**
     * Extracts the {@code GetMessageAttributeResponse} object from the provided response body.
     *
     * @param responseBody The JSON response body as a string.
     * @return The {@code GetMessageAttributeResponse} object constructed from the response body.
     * @throws IOException If there is an error processing the JSON response.
     */
    private static GetMessageAttributeResponse extractGetMessageAttributeResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        JsonNode valueNode = root.get("value");
        Variant value = valueNode == null ? null : new Variant(textOrNull(valueNode, "type"), textOrNull(valueNode, "value"));
        return GetMessageAttributeResponse.builder().messageId(textOrNull(root, "messageId")).name(textOrNull(root, "name"))
                .value(value).build();
    }

    /**
     * Extracts and constructs a {@link GetMessageMetadataResponse} object from the
     * provided JSON response body.
     *
     * @param responseBody the JSON response body containing message metadata
     * @return a {@link GetMessageMetadataResponse} object populated with the metadata
     *         extracted from the response body
     * @throws IOException if an error occurs while reading or parsing the JSON response body
     */
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
     * Constructs a map of HTTP request headers based on the provided action and body parameters,
     * and signs the request using the appropriate authentication mechanism.
     *
     * @param action the action to be performed, typically used for specifying the API operation.
     * @param body the body of the request to be sent, used in signing the request if applicable.
     * @return a map of HTTP headers constructed for the request, including required authentication and metadata headers.
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
            signSignatureHeaders(signable, TARGET, headers);
        } else {
            headers.put("Authorization", "Bearer " + token.get());
        }
        return headers;
    }

    /**
     * Signs {@code signable} with the configured scheme and copies the headers it produced onto the
     * outgoing request.
     * <p>
     * Which headers those are is the scheme's business rather than this method's: SigV4 signs into
     * Authorization alongside two {@code x-amz-*} headers, RFC 9421 into Signature and
     * Signature-Input alongside Content-Digest. The scheme is read once into a local so that a
     * {@link #signingScheme(SigningScheme)} call arriving mid-request cannot sign with one scheme
     * and then copy the header names of the other.
     *
     * @param signable the request to sign, with every header the signature covers and the body
     *                 already set on it
     * @param service  the service to scope the signature to
     * @param headers  the outgoing headers, which the signature headers are added to in place
     */
    private void signSignatureHeaders(SignableRequest signable, String service, Map<String, String> headers) {
        signable.scheme(URI.create(baseUrl).getScheme());
        SigningScheme scheme = signingScheme;
        scheme.sign(signable, accessKeyId, secretAccessKey, region, service);
        for (String header : scheme.signatureHeaderNames()) {
            headers.put(header, signable.header(header));
        }
    }

    /**
     * Generates the value for the "Host" header based on the authority portion of the URI.
     * This ensures the "Host" header used for signing matches the value that will be sent by
     * the Java HTTP client to the server.
     *
     * @return The "Host" header value, including the port if specified in the URI.
     */
    private String hostHeader() {
        URI uri = URI.create(baseUrl);
        int port = uri.getPort();
        return port == -1 ? uri.getHost() : uri.getHost() + ":" + port;
    }

    /**
     * Extracts a {@link ReceiveMessagesResponse} object from the provided JSON response string.
     *
     * @param responseBody The JSON response body as a string.
     * @return A {@link ReceiveMessagesResponse} object containing the list of messages and the total count.
     * @throws IOException If an error occurs while parsing the JSON response.
     */
    private static ReceiveMessagesResponse extractReceiveMessagesResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        return ReceiveMessagesResponse.builder().messages(toMessageList(root.get("messages")))
                .total(root.path("total").asLong(0)).build();
    }

    /**
     * Converts a JSON array node containing message data into a list of {@link Message} objects.
     *
     * @param messagesNode the JSON node representing an array of message data.
     *                      Each element in the array is expected to contain fields required to
     *                      construct a {@link Message} object.
     * @return a list of {@link Message} objects constructed from the input JSON node. If the input
     *         node is null or not an array, an empty list is returned.
     */
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
                        textOrNull(messageNode, "md5Body"),
                        textOrNull(messageNode, "receiptHandle"),
                        messageNode.path("size").asLong(0),
                        textOrNull(messageNode, "contentType"),
                        toVariantMap(messageNode.get("attributes")),
                        textOrNull(messageNode, "md5Attributes"),
                        textOrNull(messageNode, "lastReceived"),
                        textOrNull(messageNode, "created"),
                        textOrNull(messageNode, "modified")));
            }
        }
        return messages;
    }

    /**
     * Converts a given JsonNode into a map where keys are strings and values are Variant objects.
     * Each entry in the resulting map corresponds to a field in the JSON node.
     *
     * @param node the JsonNode to be converted to a map; must represent a JSON object, otherwise an empty map is returned
     * @return a map containing the JSON node's fields as keys and their corresponding Variant objects as values
     */
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

    /**
     * Extracts a list of queues from the provided JSON response body.
     *
     * @param responseBody the JSON response body containing the queue information
     * @return a list of {@code Queue} objects parsed from the response body
     * @throws IOException if an error occurs while parsing the JSON response
     */
    private static ListQueueResponse extractListQueueResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        JsonNode queuesNode = root.get("queues");
        List<Queue> queues = new ArrayList<>();
        if (queuesNode != null && queuesNode.isArray()) {
            for (JsonNode queueNode : queuesNode) {
                queues.add(new Queue(
                        textOrNull(queueNode, "name"),
                        textOrNull(queueNode, "owner"),
                        textOrNull(queueNode, "ern"),
                        toStringMap(queueNode.get("tags")),
                        queueNode.path("size").asLong(0),
                        queueNode.path("delay").asLong(0),
                        queueNode.path("available").asLong(0),
                        queueNode.path("delayed").asLong(0),
                        queueNode.path("invisible").asLong(0),
                        queueNode.path("visibility").asLong(30),
                        queueNode.path("maxMessageLength").asLong(1024 * 1024),
                        queueNode.path("maxReceiveCount").asLong(3),
                        textOrNull(queueNode, "deadLetterQueueArn"),
                        textOrNull(queueNode, "priority"),
                        textOrNull(queueNode, "created"),
                        textOrNull(queueNode, "modified")));
            }
        }
        return ListQueueResponse.builder().queues(queues).total(root.path("total").asLong(0)).build();
    }

    /**
     * Converts a JsonNode object to a Map<String, String> by extracting all the fields
     * and their corresponding text values from the JsonNode.
     *
     * @param node the JsonNode to be converted, expected to be an object node. If the node
     *             is null or not an object, an empty map will be returned.
     * @return a map containing the field names as keys and their text values as values.
     *         Returns an empty map if the input node is null or not an object.
     */
    private static Map<String, String> toStringMap(JsonNode node) {
        Map<String, String> map = new LinkedHashMap<>();
        if (node != null && node.isObject()) {
            node.fields().forEachRemaining(entry -> map.put(entry.getKey(), entry.getValue().asText()));
        }
        return map;
    }

    /**
     * Retrieves the text value of a specified field from a given JSON node.
     * If the field is not present or its value is null, the method returns null.
     *
     * @param node the JSON node from which the field is to be read
     * @param field the name of the field to retrieve
     * @return the text value of the specified field, or null if the field is not present or its value is null
     */
    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
