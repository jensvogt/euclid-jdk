package de.jensvogt.euclid.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.jensvogt.euclid.auth.CredentialsFileTokens;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.auth.SigningScheme;
import de.jensvogt.euclid.auth.SigningSchemeSelectable;
import de.jensvogt.euclid.auth.TokenRefreshable;
import de.jensvogt.euclid.dto.ees.model.DeliveryMode;
import de.jensvogt.euclid.http.EuclidHttpClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * One persistent WebSocket connection to the Euclid gateway's event stream: authenticates once
 * at handshake time (mirroring the HTTP path's Bearer/SigV4 authentication), lets callers
 * {@link #subscribe}/{@link #unsubscribe} to specific topic/filter combinations, and wait for the
 * next matching server-pushed "event" frame - the client-side counterpart of the server's
 * {@code Core::EventPusher}/{@code GatewayWsRegistry}/{@code WsFrame} (see {@code
 * EqsServer.cpp}'s {@code handleSendMessage()}, which pushes "eqs.message.sent").
 * <p>
 * Event delivery is opt-in on the gateway: a session receives nothing until it subscribes, and
 * only events matching a subscription's topic and filter after that - so, unlike a naive
 * broadcast-everything design, a client waiting on one queue's messages is never sent every other
 * queue's traffic too. An event only announces that something happened, never the payload itself,
 * so callers still make the corresponding HTTP call (e.g. receive-messages) to actually
 * fetch/claim whatever triggered it.
 * <p>
 * Connecting is lazy and best-effort: the first {@link #subscribe} or {@link #awaitEvent} call
 * attempts the handshake, and any failure (server has WebSocket support disabled, network error,
 * etc.) is surfaced as an {@link IOException} for the caller to fall back to polling.
 * <p>
 * A connection that drops is re-established in the background, and the subscriptions it carried
 * are registered again on the new one - delivery is per-connection on the gateway
 * ({@code GatewayWsRegistry}), so a reconnection that did not replay them would leave a connected
 * client attached to nothing and silently receiving no events. Listeners are told with
 * {@link EventStreamListener#onReconnected()} once that is done, because nothing could be pushed
 * while the connection was gone. A keepalive ping goes out every {@link #PING_INTERVAL} so an
 * otherwise idle connection is not closed by the gateway's idle timeout in the first place.
 */
public final class EuclidEventStream implements AutoCloseable, TokenRefreshable, SigningSchemeSelectable {

    private static final Logger LOG = Logger.getLogger(EuclidEventStream.class.getName());

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration ACK_TIMEOUT = Duration.ofSeconds(3);
    private static final String ACTION = "event-stream";

    /**
     * How often a ping goes out on an idle connection. Comfortably inside the gateway's
     * {@code euclid.gateway.websocket.idle-timeout-seconds} (300 by default), since the gateway
     * uses Beast's suggested server-role timeouts, which do not send keepalive pings themselves.
     */
    private static final Duration PING_INTERVAL = Duration.ofSeconds(60);

    private static final Duration RECONNECT_DELAY = Duration.ofSeconds(1);
    private static final Duration MAX_RECONNECT_DELAY = Duration.ofSeconds(30);

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
    private final String region;
    private final String accountId;
    private final String userId;
    private final String accessKeyId;
    private final String secretAccessKey;
    private final String target;

    /**
     * The scheme requests are signed with when an access key is configured.
     *
     * <p>Defaults to SigV4, which is what euclid has always accepted; a caller pointed at a server
     * that understands RFC 9421 switches it with {@link #signingScheme(SigningScheme)}. Volatile
     * because that call can come from a different thread than the requests it affects.
     */
    private volatile SigningScheme signingScheme = SigningScheme.SIGV4;
    private final HttpClient httpClient;

    private final List<Waiter> waiters = new CopyOnWriteArrayList<>();
    private final Map<String, CompletableFuture<Void>> pendingAcks = new ConcurrentHashMap<>();
    private final Set<Subscription> activeSubscriptions = ConcurrentHashMap.newKeySet();
    private final List<EventStreamListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<String, String> ackNames = new ConcurrentHashMap<>();
    private volatile String subscriberName;
    private final Object sendLock = new Object();
    private final Object connectLock = new Object();
    private volatile WebSocket webSocket;

    /**
     * Every delivery registration made on this connection, in the order it was made, so that a
     * replacement connection can be given the same ones. The gateway attaches a subscription to
     * the session that asked for it, so this is what makes a reconnection deliver anything at all.
     */
    private final Set<Registration> registrations = Collections.synchronizedSet(new LinkedHashSet<>());

    /** Runs the reconnect attempts and the keepalive; created once, on the first connection. */
    private volatile ScheduledExecutorService maintenance;

    private final AtomicBoolean reconnecting = new AtomicBoolean();
    private volatile boolean closed;

    /**
     * Creates a stream targeting the given server; the connection itself is opened lazily by the
     * first {@link #subscribe} or {@link #awaitEvent} call.
     *
     * @param baseUrl         the server's base URL, e.g. {@code https://euclid.example.com}
     * @param token           bearer token, used when no access key is configured
     * @param region          region to scope the connection to
     * @param accountId       account to scope the connection to
     * @param userId          the caller's user ID
     * @param accessKeyId     SigV4 access key ID; signs the handshake instead of using the bearer
     *                        token when both this and {@code secretAccessKey} are set
     * @param secretAccessKey SigV4 secret access key
     * @param caCertPath      path to an additional PEM CA certificate to trust, or {@code null}
     * @param target          the {@code x-euclid-target} to sign the handshake as, e.g. "eqs" -
     *                        purely for SigV4 signing purposes; the gateway does not route the
     *                        handshake by it
     */
    public EuclidEventStream(String baseUrl, String token, String region, String accountId, String userId,
                              String accessKeyId, String secretAccessKey, String caCertPath, String target) {
        this.baseUrl = baseUrl;
        this.token = CredentialsFileTokens.forClient(token, userId);
        this.region = region;
        this.accountId = accountId;
        this.userId = userId;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.target = target;
        this.httpClient = new EuclidHttpClient(caCertPath).httpClient();
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
     * Subscribes to events matching {@code topic}/{@code filter}, connecting first if not already
     * connected. A no-op, without a round trip, if already subscribed to this exact topic/filter.
     *
     * @param topic  the event topic to subscribe to, e.g. "eqs.message.sent"
     * @param filter exact-match key/value pairs the event's body must satisfy; empty matches
     *               every event of this topic
     * @throws IOException          if the websocket couldn't be connected, or the subscribe
     *                               request wasn't acknowledged before {@link #ACK_TIMEOUT}
     * @throws InterruptedException if interrupted while waiting for the connection or the ack
     */
    public void subscribe(String topic, Map<String, String> filter) throws IOException, InterruptedException {
        Subscription subscription = new Subscription(topic, filter);
        if (!activeSubscriptions.add(subscription)) {
            return;
        }
        try {
            sendControlFrame("subscribe", topic, filter, null, null);
        } catch (IOException | InterruptedException e) {
            activeSubscriptions.remove(subscription);
            throw e;
        }
    }

    /**
     * Unsubscribes from events matching {@code topic}/{@code filter}. A no-op if not currently
     * subscribed to this exact topic/filter.
     *
     * @param topic  the event topic to unsubscribe from
     * @param filter the filter originally passed to {@link #subscribe}
     * @throws IOException          if the websocket couldn't be connected, or the unsubscribe
     *                               request wasn't acknowledged before {@link #ACK_TIMEOUT}
     * @throws InterruptedException if interrupted while waiting for the connection or the ack
     */
    public void unsubscribe(String topic, Map<String, String> filter) throws IOException, InterruptedException {
        if (!activeSubscriptions.remove(new Subscription(topic, filter))) {
            return;
        }
        sendControlFrame("unsubscribe", topic, filter, null, null);
    }

    /**
     * Registers an EES subscription under {@code name} and attaches this connection to it, so the
     * events it matches arrive here.
     * <p>
     * A named subscription outlives the connection: it is the same subscription the {@code ees}
     * module takes over HTTP, and defaults to {@link DeliveryMode#DURABLE}, which keeps events
     * until they are acknowledged. Subscribing again with the same name and topic replaces its
     * filter and mode rather than adding a second subscription.
     * <p>
     * Naming nothing - see {@link #subscribe(String, Map)} - gets a subscription of its own that
     * is live and is removed when the connection ends, which is what a view wants.
     *
     * @param name   the subscriber name to register and attach to
     * @param topic  the event type to receive, e.g. "esm.object.created"
     * @param filter exact-match key/value pairs the event payload must satisfy; empty matches
     *               every event of this type
     * @param mode   whether the events are kept until acknowledged or only delivered live
     * @throws IOException          if the websocket couldn't be connected, or the subscribe
     *                               request wasn't acknowledged before {@link #ACK_TIMEOUT}
     * @throws InterruptedException if interrupted while waiting for the connection or the ack
     */
    public void subscribe(String name, String topic, Map<String, String> filter, DeliveryMode mode)
            throws IOException, InterruptedException {
        sendControlFrame("subscribe", topic, filter, name, mode);
    }

    /**
     * Attaches this connection to a subscription that already exists, without redefining it.
     * <p>
     * This is how a client listens to a subscription somebody else set up - the CLI, an SDK call,
     * another instance of the same application - and the only form that cannot accidentally
     * overwrite its filter.
     *
     * @param name the subscriber name to attach to
     * @throws IOException          if the websocket couldn't be connected, or the request wasn't
     *                               acknowledged before {@link #ACK_TIMEOUT}
     * @throws InterruptedException if interrupted while waiting for the connection or the ack
     */
    public void attach(String name) throws IOException, InterruptedException {
        sendControlFrame("subscribe", null, Map.of(), name, null);
    }

    /**
     * Stops receiving events on this connection without touching the subscription itself, so a
     * durable subscriber keeps collecting while nothing is listening.
     *
     * @param name the subscriber name to detach from
     * @throws IOException          if the websocket couldn't be connected, or the request wasn't
     *                               acknowledged before {@link #ACK_TIMEOUT}
     * @throws InterruptedException if interrupted while waiting for the connection or the ack
     */
    public void detach(String name) throws IOException, InterruptedException {
        sendControlFrame("unsubscribe", null, Map.of(), name, null);
    }

    /**
     * The subscriber name this connection's events arrive under.
     * <p>
     * Worth asking for after an unnamed {@link #subscribe(String, Map)}: the gateway names that
     * subscription itself, and this is where the name it chose is reported.
     *
     * @return the name, or {@code null} if nothing has been subscribed yet
     */
    public String subscriberName() {
        return subscriberName;
    }

    /**
     * Registers a listener for the frames that arrive on this connection.
     *
     * @param listener the listener to add
     */
    public void addListener(EventStreamListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a previously registered listener.
     *
     * @param listener the listener to remove
     */
    public void removeListener(EventStreamListener listener) {
        listeners.remove(listener);
    }

    /**
     * Subscribes to {@code topic}/{@code filter} (if not already subscribed) and waits for the
     * next matching event.
     *
     * @param topic         the event topic to match, e.g. "eqs.message.sent"
     * @param filter        exact-match key/value pairs the event's body must satisfy; empty
     *                      matches every event of this topic
     * @param timeoutMillis how long to wait, in milliseconds, before giving up
     * @return {@code true} if a matching event arrived before the timeout, {@code false} if it timed out
     * @throws IOException          if the websocket could not be connected or subscribed
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitEvent(String topic, Map<String, String> filter, long timeoutMillis)
            throws IOException, InterruptedException {
        // Registered before subscribe() rather than after: otherwise an event sent the instant
        // the subscription is acknowledged could arrive and be dispatched before this waiter
        // existed to catch it, silently dropping it until the timeout.
        CompletableFuture<Void> future = new CompletableFuture<>();
        Waiter waiter = new Waiter(topic, filter, future);
        waiters.add(waiter);
        try {
            subscribe(topic, filter);
            future.get(Math.max(0, timeoutMillis), TimeUnit.MILLISECONDS);
            return true;
        } catch (TimeoutException | ExecutionException e) {
            return false;
        } finally {
            waiters.remove(waiter);
        }
    }

    /**
     * Closes the underlying websocket connection, if open, and stops reconnecting. Safe to call
     * even if never connected.
     */
    @Override
    public void close() {
        closed = true;
        ScheduledExecutorService executor = maintenance;
        if (executor != null) {
            executor.shutdownNow();
        }
        WebSocket ws = webSocket;
        if (ws != null) {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "");
        }
    }

    private void sendControlFrame(String type, String topic, Map<String, String> filter, String name, DeliveryMode mode)
            throws IOException, InterruptedException {
        connect();
        sendControlFrameOnCurrentConnection(type, topic, filter, name, mode);

        // Remembered only once it was acknowledged, so a replay never re-sends something the
        // gateway rejected, and dropped on unsubscribe so it is not resurrected by a reconnect.
        Registration registration = new Registration(topic, Map.copyOf(filter), name, mode);
        if ("subscribe".equals(type)) {
            registrations.add(registration);
        } else {
            registrations.remove(registration);
        }
    }

    private void sendControlFrameOnCurrentConnection(String type, String topic, Map<String, String> filter,
                                                     String name, DeliveryMode mode)
            throws IOException, InterruptedException {
        WebSocket ws = webSocket;
        if (ws == null) {
            throw new IOException("event stream is not connected");
        }

        String id = UUID.randomUUID().toString();
        CompletableFuture<Void> ackFuture = new CompletableFuture<>();
        pendingAcks.put(id, ackFuture);
        try {
            String frame = buildControlFrame(type, id, topic, filter, name, mode);
            synchronized (sendLock) {
                ws.sendText(frame, true).get(ACK_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            }
            ackFuture.get(ACK_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            // The ack is where an unnamed subscription learns the name the gateway gave it, and
            // where a named one is confirmed - either way it is what this connection's events
            // will arrive under.
            String acknowledged = ackNames.remove(id);
            if (acknowledged != null && !acknowledged.isEmpty()) {
                subscriberName = "unsubscribe".equals(type) ? null : acknowledged;
            }
        } catch (ExecutionException | TimeoutException e) {
            throw new IOException("failed to " + type + " to " + (topic == null ? "subscriber " + name : "topic " + topic), e);
        } finally {
            pendingAcks.remove(id);
            ackNames.remove(id);
        }
    }

    private static String buildControlFrame(String type, String id, String topic, Map<String, String> filter,
                                            String name, DeliveryMode mode) throws IOException {
        ObjectNode node = OBJECT_MAPPER.createObjectNode();
        node.put("type", type);
        node.put("id", id);
        // Omitted rather than sent empty when attaching: a frame that names a subscription and no
        // topic says "send me what this one gets", while one carrying a topic redefines it.
        if (topic != null) {
            node.put("topic", topic);
            ObjectNode filterNode = node.putObject("filter");
            filter.forEach(filterNode::put);
        }
        if (name != null && !name.isEmpty()) {
            node.put("name", name);
        }
        if (mode != null) {
            node.put("mode", mode.wireValue());
        }
        return OBJECT_MAPPER.writeValueAsString(node);
    }

    private void connect() throws IOException, InterruptedException {
        if (webSocket != null) {
            return;
        }
        synchronized (connectLock) {
            if (webSocket != null) {
                return;
            }
            if (closed) {
                throw new IOException("event stream is closed");
            }
            URI uri = toWebSocketUri(baseUrl);
            WebSocket.Builder builder = httpClient.newWebSocketBuilder().connectTimeout(CONNECT_TIMEOUT);
            requestHeaders().forEach(builder::header);
            try {
                webSocket = builder.buildAsync(uri, new EventListener()).get(CONNECT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                throw new IOException("failed to connect event stream to " + uri, e.getCause());
            } catch (TimeoutException e) {
                throw new IOException("timed out connecting event stream to " + uri);
            }
            startMaintenance();
        }
    }

    /**
     * Starts the keepalive, once, on the first successful connection. A ping every
     * {@link #PING_INTERVAL} is what keeps a connection that has nothing to say from being closed
     * as idle - the gateway does not ping, and a client that only ever reads would look idle to it
     * no matter how much it is subscribed to.
     */
    private void startMaintenance() {
        if (maintenance != null) {
            return;
        }
        maintenance = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "euclid-event-stream");
            thread.setDaemon(true);
            return thread;
        });
        maintenance.scheduleWithFixedDelay(this::ping, PING_INTERVAL.toMillis(), PING_INTERVAL.toMillis(),
                TimeUnit.MILLISECONDS);
    }

    private void ping() {
        WebSocket ws = webSocket;
        if (ws == null || closed) {
            return;
        }
        try {
            synchronized (sendLock) {
                ws.sendPing(ByteBuffer.allocate(0)).get(ACK_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // The connection is gone or going; onError/onClose is what reconnects, and a failed
            // ping is how this one finds out a moment earlier.
            LOG.log(Level.FINE, "event stream keepalive failed", e);
            connectionLost();
        }
    }

    /**
     * Called when the connection has gone away, from whichever of onClose, onError or a failed
     * ping notices first. Reconnecting happens on the maintenance thread rather than here, because
     * two of those three callers are the websocket's own reading thread, which the handshake and
     * the acknowledged control frames below would deadlock.
     */
    private void connectionLost() {
        webSocket = null;
        if (closed || !reconnecting.compareAndSet(false, true)) {
            return;
        }
        ScheduledExecutorService executor = maintenance;
        if (executor == null || executor.isShutdown()) {
            reconnecting.set(false);
            return;
        }
        executor.schedule(this::reconnect, RECONNECT_DELAY.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Re-establishes the connection and puts its subscriptions back, retrying with a backoff until
     * it succeeds or the stream is closed. Delivery is per-connection on the gateway, so a
     * reconnection without the replay would be a connected client attached to nothing.
     */
    private void reconnect() {
        long delay = RECONNECT_DELAY.toMillis();
        try {
            while (!closed && !Thread.currentThread().isInterrupted()) {
                try {
                    connect();
                    replayRegistrations();
                    LOG.log(Level.INFO, () -> "event stream reconnected, " + registrations.size() + " subscription(s) restored");
                    for (EventStreamListener listener : listeners) {
                        listener.onReconnected();
                    }
                    return;
                } catch (IOException e) {
                    LOG.log(Level.FINE, "could not reconnect the event stream, retrying", e);
                    webSocket = null;
                    Thread.sleep(delay);
                    delay = Math.min(delay * 2, MAX_RECONNECT_DELAY.toMillis());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            reconnecting.set(false);
        }
    }

    private void replayRegistrations() throws IOException, InterruptedException {
        List<Registration> replay;
        synchronized (registrations) {
            replay = List.copyOf(registrations);
        }
        for (Registration registration : replay) {
            sendControlFrameOnCurrentConnection("subscribe", registration.topic(), registration.filter(),
                    registration.name(), registration.mode());
        }
    }

    private static URI toWebSocketUri(String baseUrl) {
        String wsUrl;
        if (baseUrl.startsWith("https://")) {
            wsUrl = "wss://" + baseUrl.substring("https://".length());
        } else if (baseUrl.startsWith("http://")) {
            wsUrl = "ws://" + baseUrl.substring("http://".length());
        } else {
            wsUrl = baseUrl;
        }
        return URI.create(wsUrl + "/");
    }

    /**
     * Builds the handshake headers: routing headers plus authentication.
     * <p>
     * Signs with the configured {@link SigningScheme} - SigV4 unless
     * {@link #signingScheme(SigningScheme)} says otherwise - when accessKeyId and secretAccessKey
     * are both set, mirroring how euclid-cli authenticates service calls; falls back to the bearer
     * token otherwise. The handshake is a GET with no body, unlike every action call's POST - so it
     * is signed as one, distinct from the POST-based signing the rest of the SDK's
     * {@code requestHeaders()} methods do.
     */
    private Map<String, String> requestHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
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
            SignableRequest signable = new SignableRequest("GET", "/");
            headers.forEach(signable::header);
            signable.header("host", hostHeader());
            signable.header("x-euclid-target", target);
            signable.header("x-euclid-action", ACTION);
            signable.body("");
            signSignatureHeaders(signable, target, headers);
        } else {
            headers.put("Authorization", "Bearer " + token.get());
        }
        headers.put("x-euclid-target", target);
        headers.put("x-euclid-action", ACTION);
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

    // The literal "Host" header java.net.http will put on the wire, derived the same way it
    // derives it (from the URI's authority) so the value we sign here matches what the server
    // actually receives.
    private String hostHeader() {
        URI uri = URI.create(baseUrl);
        int port = uri.getPort();
        return port == -1 ? uri.getHost() : uri.getHost() + ":" + port;
    }

    private void handleFrame(String frame) {
        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(frame);
        } catch (IOException e) {
            return;
        }
        String type = textOrNull(root, "type");
        if (type == null) {
            return;
        }

        if ("subscribed".equals(type) || "unsubscribed".equals(type)) {
            String id = textOrNull(root, "id");
            String name = textOrNull(root, "name");
            if (id != null && name != null) {
                ackNames.put(id, name);
            }
            CompletableFuture<Void> ack = pendingAcks.get(id);
            if (ack != null) {
                ack.complete(null);
            }
            return;
        }

        if ("lag".equals(type)) {
            JsonNode dropped = root.get("dropped");
            for (EventStreamListener listener : listeners) {
                listener.onLag(dropped == null ? 0 : dropped.asLong());
            }
            return;
        }

        if (!"event".equals(type)) {
            return;
        }
        String topic = textOrNull(root, "topic");
        JsonNode body = root.get("body");
        if (topic == null || body == null) {
            return;
        }

        // A durable subscription is told that something is waiting, not what it is - see
        // EventStreamListener - so the two are dispatched differently rather than a listener
        // having to know which kind of subscription it is attached to.
        if ("notify".equals(textOrNull(body, "delivery"))) {
            for (EventStreamListener listener : listeners) {
                listener.onNotify(topic);
            }
            return;
        }

        for (EventStreamListener listener : listeners) {
            listener.onEvent(topic, body);
        }
        for (Waiter waiter : waiters) {
            if (waiter.topic().equals(topic) && matchesFilter(body, waiter.filter())) {
                waiter.future().complete(null);
            }
        }
    }

    private static boolean matchesFilter(JsonNode body, Map<String, String> filter) {
        for (Map.Entry<String, String> entry : filter.entrySet()) {
            JsonNode value = body.get(entry.getKey());
            if (value == null || !entry.getValue().equals(value.asText())) {
                return false;
            }
        }
        return true;
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private record Subscription(String topic, Map<String, String> filter) {
    }

    /**
     * One delivery registration, in the form needed to make it again on a new connection: the
     * arguments of the control frame that established it.
     */
    private record Registration(String topic, Map<String, String> filter, String name, DeliveryMode mode) {
    }

    private record Waiter(String topic, Map<String, String> filter, CompletableFuture<Void> future) {
    }

    private final class EventListener implements WebSocket.Listener {

        // Per connection rather than per stream: a reconnection starts a new listener, and a
        // buffer shared with the dead one would prepend its half-read frame to the first new one.
        private final StringBuilder frameBuffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            frameBuffer.append(data);
            webSocket.request(1);
            if (last) {
                String frame = frameBuffer.toString();
                frameBuffer.setLength(0);
                handleFrame(frame);
            }
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            LOG.log(Level.FINE, () -> "event stream closed by the gateway: " + statusCode + " " + reason);
            connectionLost();
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            LOG.log(Level.FINE, "event stream failed", error);
            connectionLost();
        }
    }
}
