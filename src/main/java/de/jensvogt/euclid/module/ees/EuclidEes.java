package de.jensvogt.euclid.module.ees;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jensvogt.euclid.auth.SigV4;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.dto.ees.AckEventsRequest;
import de.jensvogt.euclid.dto.ees.AckEventsResponse;
import de.jensvogt.euclid.dto.ees.ListSubscriptionsRequest;
import de.jensvogt.euclid.dto.ees.ListSubscriptionsResponse;
import de.jensvogt.euclid.dto.ees.ReceiveEventsRequest;
import de.jensvogt.euclid.dto.ees.ReceiveEventsResponse;
import de.jensvogt.euclid.dto.ees.SubscribeEventsRequest;
import de.jensvogt.euclid.dto.ees.SubscribeEventsResponse;
import de.jensvogt.euclid.dto.ees.UnsubscribeEventsRequest;
import de.jensvogt.euclid.dto.ees.UnsubscribeEventsResponse;
import de.jensvogt.euclid.dto.ees.model.DeliveryMode;
import de.jensvogt.euclid.dto.ees.model.Event;
import de.jensvogt.euclid.dto.ees.model.EventSubscription;
import de.jensvogt.euclid.exception.EuclidServiceException;
import de.jensvogt.euclid.http.EuclidHttpClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * EES (event service) operations for an authenticated
 * {@link de.jensvogt.euclid.module.eam.EuclidSession}: the event bus, for consumers that are not
 * euclid modules.
 * <p>
 * A subscriber registers a durable <em>name</em> and pulls. Nothing is pushed and no queue has to be
 * created: {@link #receiveEvents} claims waiting events and {@link #ackEvents} deletes them, and an
 * event that is claimed but never acknowledged becomes claimable again when its visibility timeout
 * runs out - so a consumer that crashes mid-work loses nothing.
 * <p>
 * The name decides fan-out. Two <em>instances</em> of one application share a name, so whichever
 * claims an event first processes it; two <em>different</em> applications use different names and
 * each receive their own copy of the same event.
 * <p>
 * A subscription carries a filter that is evaluated at publish time, so a subscriber only ever
 * accumulates the events it asked for rather than everything of that type in the installation.
 * <p>
 * <strong>ESM object events</strong> - the ones a storage consumer typically wants. All three carry
 * the same flat payload, whose values are strings, numbers and booleans precisely so a filter can
 * match them by equality:
 * <table border="1">
 * <caption>ESM object event types</caption>
 * <tr><th>Event type</th><th>Published when</th></tr>
 * <tr><td>{@code esm.object.created}</td><td>an object is first written</td></tr>
 * <tr><td>{@code esm.object.updated}</td><td>an existing object is replaced or otherwise changed</td></tr>
 * <tr><td>{@code esm.object.deleted}</td><td>an object is deleted, including the source of a move</td></tr>
 * </table>
 * Payload: {@code ern}, {@code bucketErn}, {@code bucketName}, {@code key}, {@code prefix},
 * {@code directory}, {@code size}, {@code contentType}, {@code md5Sum}, {@code owner},
 * {@code userId}, {@code accountId}, {@code region}, {@code namespace}, {@code eventTime}.
 * <p>
 * Those fields are what make the useful subscriptions expressible without a consumer receiving
 * everything and discarding most of it: {@code {"bucketErn": "..."}} for one bucket,
 * {@code {"prefix": "invoices/2026/"}} for one "directory" - a key is a path by convention only, and
 * {@code prefix} is that convention spelled out by the server rather than by each subscriber - and
 * {@code {"directory": false}} to skip directory markers. {@code owner} is who uploaded the object
 * and {@code userId} who made this change; a move performed by an operator differs in the two.
 * <p>
 * Note that ESM also publishes {@code esm.subscription.delivery} and
 * {@code esm.subscription.publication}. Those are not domain events: each is addressed to the one
 * module that can act on it, carrying a queue or topic to put a notification into, and they are the
 * plumbing behind {@code EuclidEsm}'s bucket subscribe/unsubscribe. A consumer wanting that path
 * reads the delivered message and parses it with {@code EuclidEsm.parseBucketEvent} instead.
 */
public final class EuclidEes {

    /**
     * A singleton instance of {@code ObjectMapper} from the Jackson library used for
     * serializing Java objects to JSON and deserializing JSON to Java objects.
     * <p>
     * This instance is thread-safe and can be reused throughout the application
     * to avoid the overhead of creating multiple instances.
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Reads an event payload or a subscription filter, whose shape is decided by the event type
     * rather than by this client, into a plain map.
     */
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    /**
     * The Euclid service every request from this class is addressed to, sent as the
     * {@code x-euclid-target} header.
     */
    private static final String TARGET = "ees";

    /**
     * The base URL of the Euclid server this instance talks to.
     */
    private final String baseUrl;

    /**
     * The bearer token issued at login, used when no SigV4 access key is configured.
     */
    private final String token;

    /**
     * The region requests are made in.
     */
    private final String region;

    /**
     * The account requests are made on behalf of. An event whose payload names a different account
     * is never stored for this subscriber.
     */
    private final String accountId;

    /**
     * The user requests are made on behalf of.
     */
    private final String userId;

    /**
     * Public identifier of the SigV4 access key, or {@code null} to authenticate with the token.
     */
    private final String accessKeyId;

    /**
     * Secret paired with {@link #accessKeyId}, or {@code null} to authenticate with the token.
     */
    private final String secretAccessKey;

    /**
     * The namespace requests are scoped to, sent as the {@code x-euclid-namespace} header.
     */
    private final String nameSpace;

    /**
     * The HTTP client used for every request except {@link #receiveEvents}, pre-configured with
     * this session's TLS trust.
     */
    private final EuclidHttpClient httpClient;

    /**
     * The HTTP client used for {@link #receiveEvents}, whose request holds a gateway worker thread
     * open for up to 20 seconds (the server's clamp on {@code waitTime}) - long enough that the
     * default 10-second request timeout used by {@link #httpClient} would abort the request before
     * the server ever gets to answer it.
     */
    private final EuclidHttpClient longPollHttpClient;

    /**
     * Constructs an EES client. Normally obtained from
     * {@link de.jensvogt.euclid.module.eam.EuclidSession#ees()} rather than built directly.
     *
     * @param baseUrl         the base URL of the Euclid server
     * @param token           the bearer token issued at login
     * @param region          the region requests are made in
     * @param accountId       the account requests are made on behalf of
     * @param userId          the user requests are made on behalf of
     * @param accessKeyId     public identifier of the SigV4 access key, or {@code null} for token auth
     * @param secretAccessKey secret paired with {@code accessKeyId}, or {@code null} for token auth
     * @param caCertPath      path to an additional PEM CA certificate to trust, or {@code null}
     * @param nameSpace       the namespace requests are scoped to, or {@code null} if unscoped
     */
    public EuclidEes(String baseUrl, String token, String region, String accountId, String userId,
                     String accessKeyId, String secretAccessKey, String caCertPath, String nameSpace) {
        this.baseUrl = baseUrl;
        this.token = token;
        this.region = region;
        this.accountId = accountId;
        this.userId = userId;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.nameSpace = nameSpace;
        this.httpClient = new EuclidHttpClient(caCertPath);
        this.longPollHttpClient = new EuclidHttpClient(Duration.ofSeconds(30), caCertPath);
    }

    /**
     * Subscribes to every event of the given types, unfiltered.
     *
     * @param name the subscriber name events are claimed under
     * @param eventTypes the event types to receive
     * @return the subscriptions the subscriber now holds
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public SubscribeEventsResponse subscribeEvents(String name, List<String> eventTypes)
            throws IOException, InterruptedException {
        return subscribeEvents(name, eventTypes, Map.of());
    }

    /**
     * Registers a durable subscription, or updates the filter of one that already exists.
     * <p>
     * The filter is matched against the event payload at publish time, so it decides what the
     * subscriber accumulates rather than what it sees on receive - narrowing it later does not
     * remove events already stored. Watching one bucket means filtering
     * {@code esm.object.modified} on that bucket's {@code bucketErn}.
     *
     * @param name the subscriber name events are claimed under
     * @param eventTypes the event types to receive; one subscription is registered per type
     * @param filter exact-match key/value pairs an event payload must satisfy, or empty for all
     * @return the subscriptions the subscriber now holds
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public SubscribeEventsResponse subscribeEvents(String name, List<String> eventTypes, Map<String, Object> filter)
            throws IOException, InterruptedException {
        return subscribeEvents(name, eventTypes, filter, DeliveryMode.DURABLE);
    }

    /**
     * Registers a subscription with an explicit delivery mode.
     * <p>
     * {@link DeliveryMode#DURABLE} keeps every matching event until it is acknowledged, so an
     * application that was restarting still finds what happened meanwhile.
     * {@link DeliveryMode#LIVE} stores nothing and only reaches websocket sessions attached to the
     * name - what a view wants, since a screen has no use for the hour of events it missed while
     * nobody was looking at it.
     * <p>
     * Subscribing again with the same name and event type replaces the subscription rather than
     * adding a second one, so this is also how a mode or a filter is changed.
     *
     * @param name the subscriber name events are claimed under
     * @param eventTypes the event types to receive; one subscription is registered per type
     * @param filter exact-match key/value pairs an event payload must satisfy, or empty for all
     * @param mode how the events should reach this subscriber
     * @return the subscriptions the subscriber now holds
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public SubscribeEventsResponse subscribeEvents(String name, List<String> eventTypes, Map<String, Object> filter,
                                                    DeliveryMode mode) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(SubscribeEventsRequest.builder()
                .name(name).eventTypes(eventTypes).filter(filter)
                .mode(mode == null ? null : mode.wireValue()).build());
        JsonNode root = post("subscribe-events", body);
        return SubscribeEventsResponse.builder().subscriptions(toSubscriptions(root.get("subscriptions"))).build();
    }

    /**
     * Removes the subscriber entirely, along with every event still waiting for it - which is what
     * an application being decommissioned wants.
     *
     * @param name the subscriber name
     * @return how many subscriptions were removed
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public UnsubscribeEventsResponse unsubscribeEvents(String name) throws IOException, InterruptedException {
        return unsubscribeEvents(name, "");
    }

    /**
     * Stops a subscriber receiving one event type, leaving its other subscriptions in place.
     *
     * @param name the subscriber name
     * @param eventType the event type to stop receiving; empty removes the subscriber entirely
     * @return how many subscriptions were removed
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public UnsubscribeEventsResponse unsubscribeEvents(String name, String eventType)
            throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                UnsubscribeEventsRequest.builder().name(name).eventType(eventType).build());
        JsonNode root = post("unsubscribe-events", body);
        return UnsubscribeEventsResponse.builder().subscriber(textOrNull(root, "subscriber"))
                .removed(root.path("removed").asLong(0)).build();
    }

    /**
     * Lists a subscriber's subscriptions, and how many events are waiting for it.
     *
     * @param name the subscriber name
     * @return the subscriptions and the waiting count
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ListSubscriptionsResponse listSubscriptions(String name) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(ListSubscriptionsRequest.builder().name(name).build());
        JsonNode root = post("list-subscriptions", body);
        return ListSubscriptionsResponse.builder().subscriptions(toSubscriptions(root.get("subscriptions")))
                .waiting(root.path("waiting").asLong(0)).build();
    }

    /**
     * Claims up to ten waiting events, answering immediately if there are none.
     *
     * @param name the subscriber name
     * @return the claimed events
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ReceiveEventsResponse receiveEvents(String name) throws IOException, InterruptedException {
        return receiveEvents(name, 10, 0, 300);
    }

    /**
     * Claims a subscriber's waiting events, optionally long-polling for them.
     * <p>
     * Claimed events are invisible to anything else sharing this subscriber name until
     * {@code visibilityTimeout} elapses, and only {@link #ackEvents} removes them - so an event
     * whose processing fails is redelivered rather than lost.
     *
     * @param name the subscriber name
     * @param maxEvents largest number of events to claim; the server floors this at 1
     * @param waitTime seconds to wait for an event before answering empty. The server clamps this
     *                 to 20, because a gateway worker thread is held for the duration
     * @param visibilityTimeout seconds the claim holds before the events become claimable again
     * @return the claimed events, oldest first
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ReceiveEventsResponse receiveEvents(String name, long maxEvents, long waitTime, long visibilityTimeout)
            throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(ReceiveEventsRequest.builder().name(name)
                .maxEvents(maxEvents).waitTime(waitTime).visibilityTimeout(visibilityTimeout).build());
        JsonNode root = post(longPollHttpClient, "receive-events", body);

        List<Event> events = new ArrayList<>();
        JsonNode eventsNode = root.get("events");
        if (eventsNode != null && eventsNode.isArray()) {
            for (JsonNode eventNode : eventsNode) {
                events.add(new Event(
                        textOrNull(eventNode, "eventId"),
                        textOrNull(eventNode, "eventType"),
                        textOrNull(eventNode, "sourceModule"),
                        toObjectMap(eventNode.get("payload")),
                        eventNode.path("attempts").asLong(0),
                        textOrNull(eventNode, "created")));
            }
        }
        return ReceiveEventsResponse.builder().events(events).total(root.path("total").asLong(0)).build();
    }

    /**
     * Acknowledges a single event, deleting it.
     *
     * @param name the subscriber name the event was claimed under
     * @param eventId the ID from the claimed envelope
     * @return how many events were deleted, and how many are still waiting
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public AckEventsResponse ackEvent(String name, String eventId) throws IOException, InterruptedException {
        return ackEvents(name, List.of(eventId));
    }

    /**
     * Acknowledges claimed events, deleting them - which is what "processed" means here.
     * <p>
     * An event that is no longer there counts as acknowledged rather than failing: a redelivery
     * acked twice and one whose retention ran out mean the same thing to a caller. So
     * {@code acknowledged} can be lower than the number of IDs passed.
     *
     * @param name the subscriber name the events were claimed under
     * @param eventIds the IDs from the claimed envelopes
     * @return how many events were deleted, and how many are still waiting
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public AckEventsResponse ackEvents(String name, List<String> eventIds) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                AckEventsRequest.builder().name(name).eventIds(eventIds).build());
        JsonNode root = post("ack-events", body);
        return AckEventsResponse.builder().subscriber(textOrNull(root, "subscriber"))
                .acknowledged(root.path("acknowledged").asLong(0)).waiting(root.path("waiting").asLong(0)).build();
    }

    /**
     * Posts one of EES's actions and parses the response body, since every one of them takes a JSON
     * request and answers with JSON.
     *
     * @param action the EES action to post
     * @param body the JSON request body
     * @return the parsed response body
     * @throws IOException if an I/O error occurs during the request
     * @throws InterruptedException if the operation is interrupted while waiting for the response
     */
    private JsonNode post(String action, String body) throws IOException, InterruptedException {
        return post(httpClient, action, body);
    }

    /**
     * Same as {@link #post(String, String)}, but issued through a specific client - so
     * {@link #receiveEvents} can use {@link #longPollHttpClient}'s longer request timeout instead
     * of {@link #httpClient}'s.
     */
    private JsonNode post(EuclidHttpClient client, String action, String body) throws IOException, InterruptedException {
        HttpResponse<String> response = client.post(baseUrl + "/", body, TARGET, action,
                requestHeaders(action, body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException(TARGET, action, response.statusCode(), response.body());
        }

        return OBJECT_MAPPER.readTree(response.body());
    }

    /**
     * Converts a JsonNode holding an array of subscriptions into a list.
     *
     * @param node the JsonNode representing the array of subscriptions
     * @return the subscriptions, or an empty list if the node is null or not an array
     */
    private static List<EventSubscription> toSubscriptions(JsonNode node) {
        List<EventSubscription> subscriptions = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode subscriptionNode : node) {
                subscriptions.add(new EventSubscription(
                        textOrNull(subscriptionNode, "subscriber"),
                        textOrNull(subscriptionNode, "eventType"),
                        toObjectMap(subscriptionNode.get("filter")),
                        textOrNull(subscriptionNode, "accountId"),
                        DeliveryMode.fromWireValue(textOrNull(subscriptionNode, "mode")),
                        textOrNull(subscriptionNode, "created"),
                        textOrNull(subscriptionNode, "lastSeen")));
            }
        }
        return subscriptions;
    }

    /**
     * Converts a JSON object whose shape this client does not fix - an event payload or a
     * subscription filter - into a map, leaving each value as whatever JSON type it arrived as.
     *
     * @param node the JsonNode to convert
     * @return the node's fields, or an empty map if it is null or not an object
     */
    private static Map<String, Object> toObjectMap(JsonNode node) {
        if (node == null || !node.isObject()) {
            return Map.of();
        }
        return OBJECT_MAPPER.convertValue(node, OBJECT_MAP);
    }

    /**
     * Generates a map of HTTP request headers for a specified action and request body.
     * The headers include content type, region, account ID, user ID, and
     * authentication information. If AWS credentials are available, the headers
     * are signed using the SigV4 signing process; otherwise, a Bearer token is used.
     *
     * @param action the action being performed by the request.
     * @param body the body of the request to be included for signing.
     * @return a map of HTTP headers constructed for the request.
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

    /**
     * Builds the {@code host} header value the SigV4 signature is computed over, including the port
     * when the base URL names one.
     *
     * @return the host header value
     */
    private String hostHeader() {
        URI uri = URI.create(baseUrl);
        int port = uri.getPort();
        return port == -1 ? uri.getHost() : uri.getHost() + ":" + port;
    }

    /**
     * Reads a text field from a JSON node, tolerating both an absent field and an explicit null.
     *
     * @param node the node to read from
     * @param field the field name
     * @return the field's text value, or {@code null} if it is absent or null
     */
    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
