package de.jensvogt.euclid.ws;

import com.fasterxml.jackson.databind.JsonNode;
import de.jensvogt.euclid.dto.ees.ReceiveEventsResponse;
import de.jensvogt.euclid.dto.ees.model.DeliveryMode;
import de.jensvogt.euclid.dto.ees.model.Event;
import de.jensvogt.euclid.module.ees.EuclidEes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Calls a handler for every event a subscriber receives, without the application writing a poll
 * loop.
 * <p>
 * This is the durable half of the event stream put together: the subscription is registered
 * through {@link EuclidEes}, the websocket is attached to its name, and when the gateway says
 * something is waiting, the events are claimed, handed to the handler, and acknowledged. Nothing
 * is acknowledged before the handler returns, so an application that dies mid-event gets it again
 * rather than losing it - which is the whole reason for a durable subscription.
 * <p>
 * A {@link DeliveryMode#LIVE} listener is the same shape without the store behind it: events
 * arrive on the connection and are handed straight to the handler, and there is nothing to
 * acknowledge or to catch up on.
 * <p>
 * Claiming runs on a thread of the listener's own, never on the websocket's reading thread, so a
 * slow handler delays this subscriber and nothing else. {@link Builder#concurrency(int)} runs more
 * than one such thread, each independently claiming and dispatching batches - safe because the
 * claim in {@code receive-events} is what decides who handles an event, the same mechanism that
 * lets two applications poll one subscriber without processing anything twice.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * try (EuclidEventListener listener = EuclidEventListener.builder()
 *         .ees(session.ees())
 *         .stream(stream)
 *         .name("invoice-import")
 *         .eventTypes(List.of("esm.object.created"))
 *         .filter(Map.of("bucketName", "inbox"))
 *         .handler(event -> importInvoice(event.payload()))
 *         .build()) {
 *     listener.start();
 *     // ... events arrive on the handler until the listener is closed
 * }
 * }</pre>
 */
public final class EuclidEventListener implements AutoCloseable, EventStreamListener {

    private static final Logger LOG = Logger.getLogger(EuclidEventListener.class.getName());

    /**
     * How many events one claim asks for. A claim is a round trip, so this is the batch size; the
     * listener keeps claiming until a claim comes back empty.
     */
    private static final long CLAIM_BATCH = 32;

    private final EuclidEes ees;
    private final EuclidEventStream stream;
    private final String name;
    private final List<String> eventTypes;
    private final Map<String, Object> filter;
    private final DeliveryMode mode;
    private final Consumer<Event> handler;
    private final int concurrency;
    private final ExecutorService claims;
    private final AtomicBoolean started = new AtomicBoolean();

    private EuclidEventListener(Builder builder) {
        this.ees = builder.ees;
        this.stream = builder.stream;
        this.name = builder.name;
        this.eventTypes = List.copyOf(builder.eventTypes);
        this.filter = builder.filter == null ? Map.of() : Map.copyOf(builder.filter);
        this.mode = builder.mode == null ? DeliveryMode.DURABLE : builder.mode;
        this.handler = builder.handler;
        this.concurrency = Math.max(1, builder.concurrency);
        this.claims = Executors.newFixedThreadPool(this.concurrency, runnable -> {
            Thread thread = new Thread(runnable, "euclid-events");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Creates a builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Registers the subscription, attaches this connection to it, and starts delivering.
     * <p>
     * A durable listener drains whatever is already waiting before it returns, so events that
     * arrived while the application was down are handled at startup rather than on the next one.
     *
     * @throws IOException          if the subscription could not be registered or the websocket
     *                               could not be attached
     * @throws InterruptedException if interrupted while doing either
     */
    public void start() throws IOException, InterruptedException {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        ees.subscribeEvents(name, eventTypes, filter, mode);
        stream.addListener(this);
        stream.attach(name);
        if (mode == DeliveryMode.DURABLE) {
            for (int i = 0; i < concurrency; i++) {
                claims.execute(this::drain);
            }
        }
    }

    /**
     * Detaches the connection and stops delivering. The subscription itself is left alone, so a
     * durable one keeps collecting events for the next time this listener starts - use
     * {@link EuclidEes#unsubscribeEvents(String)} to give that up deliberately.
     */
    @Override
    public void close() {
        stream.removeListener(this);
        claims.shutdown();
        try {
            if (!claims.awaitTermination(5, TimeUnit.SECONDS)) {
                claims.shutdownNow();
            }
        } catch (InterruptedException e) {
            claims.shutdownNow();
            Thread.currentThread().interrupt();
        }
        if (started.get()) {
            try {
                stream.detach(name);
            } catch (IOException | InterruptedException e) {
                // The connection is being given up anyway, and the gateway drops the attachment
                // when it closes - so failing to say so politely changes nothing.
                LOG.log(Level.FINE, "could not detach from subscriber " + name, e);
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Override
    public void onNotify(String topic) {
        claims.execute(this::drain);
    }

    @Override
    public void onLag(long dropped) {
        // Exactly what a lag frame is for on a durable subscription: the events the gateway could
        // not push are still in the store, so go and get them.
        LOG.log(Level.FINE, () -> "gateway dropped " + dropped + " pushed events for " + name + ", claiming instead");
        if (mode == DeliveryMode.DURABLE) {
            claims.execute(this::drain);
        }
    }

    @Override
    public void onEvent(String topic, JsonNode body) {
        if (mode != DeliveryMode.LIVE) {
            return;
        }
        // Nothing to claim and nothing to acknowledge: for a live subscription this frame is the
        // event, and the only copy of it there will ever be.
        claims.execute(() -> dispatch(new Event(null, topic, null, toMap(body), 0, null)));
    }

    /**
     * Claims and handles everything waiting, in batches, until a claim comes back empty.
     */
    private void drain() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                ReceiveEventsResponse claimed = ees.receiveEvents(name, CLAIM_BATCH, 0, 0);
                List<Event> events = claimed.events();
                if (events == null || events.isEmpty()) {
                    return;
                }

                List<String> handled = new ArrayList<>(events.size());
                for (Event event : events) {
                    if (dispatch(event)) {
                        handled.add(event.eventId());
                    }
                }
                // Only what the handler actually got through is acknowledged; anything it threw on
                // stays claimed and comes back when its visibility timeout runs out.
                if (!handled.isEmpty()) {
                    ees.ackEvents(name, handled);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            // Left for the next notify or the next drain: the events are still in the store, so a
            // failed claim costs latency rather than data.
            LOG.log(Level.WARNING, "could not claim events for " + name, e);
        }
    }

    private boolean dispatch(Event event) {
        try {
            handler.accept(event);
            return true;
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "handler failed for an event of " + name + ", leaving it unacknowledged", e);
            return false;
        }
    }

    private static Map<String, Object> toMap(JsonNode body) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        body.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            if (value.isNumber()) {
                payload.put(entry.getKey(), value.numberValue());
            } else if (value.isBoolean()) {
                payload.put(entry.getKey(), value.booleanValue());
            } else if (value.isNull()) {
                payload.put(entry.getKey(), null);
            } else {
                payload.put(entry.getKey(), value.asText());
            }
        });
        return payload;
    }

    /**
     * Builds an {@link EuclidEventListener}.
     */
    public static final class Builder {

        private EuclidEes ees;
        private EuclidEventStream stream;
        private String name;
        private List<String> eventTypes = List.of();
        private Map<String, Object> filter;
        private DeliveryMode mode;
        private Consumer<Event> handler;
        private int concurrency = 1;

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Sets the ees client used to register the subscription and claim its events.
         *
         * @param ees the client
         * @return this builder
         */
        public Builder ees(EuclidEes ees) {
            this.ees = ees;
            return this;
        }

        /**
         * Sets the connection this listener attaches to. One stream can carry several listeners.
         *
         * @param stream the event stream
         * @return this builder
         */
        public Builder stream(EuclidEventStream stream) {
            this.stream = stream;
            return this;
        }

        /**
         * Sets the subscriber name. Two instances of one application share it deliberately: each
         * event is then handled by exactly one of them.
         *
         * @param name the subscriber name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the event types to receive.
         *
         * @param eventTypes the event types, e.g. {@code List.of("esm.object.created")}
         * @return this builder
         */
        public Builder eventTypes(List<String> eventTypes) {
            this.eventTypes = eventTypes;
            return this;
        }

        /**
         * Sets the payload filter, evaluated where the event is published.
         *
         * @param filter exact-match key/value pairs, or empty for every event of these types
         * @return this builder
         */
        public Builder filter(Map<String, Object> filter) {
            this.filter = filter;
            return this;
        }

        /**
         * Sets the delivery mode. Defaults to {@link DeliveryMode#DURABLE}.
         *
         * @param mode the mode
         * @return this builder
         */
        public Builder mode(DeliveryMode mode) {
            this.mode = mode;
            return this;
        }

        /**
         * Sets the handler called for each event.
         *
         * @param handler the handler; throwing from it leaves the event unacknowledged, so it is
         *                delivered again
         * @return this builder
         */
        public Builder handler(Consumer<Event> handler) {
            this.handler = handler;
            return this;
        }

        /**
         * Sets how many threads independently claim and dispatch batches at once. Defaults to
         * one; raise it when publishing outruns what a single thread can drain. Only applies to
         * {@link DeliveryMode#DURABLE}, since a {@link DeliveryMode#LIVE} event is handled exactly
         * once, as it arrives, with nothing to claim concurrently.
         *
         * @param concurrency the number of claiming threads, floored at one
         * @return this builder
         */
        public Builder concurrency(int concurrency) {
            this.concurrency = concurrency;
            return this;
        }

        /**
         * Builds the listener.
         *
         * @return the listener, not yet started
         * @throws IllegalStateException if the client, stream, name or handler is missing
         */
        public EuclidEventListener build() {
            if (ees == null || stream == null || name == null || name.isEmpty() || handler == null) {
                throw new IllegalStateException("ees, stream, name and handler are required");
            }
            return new EuclidEventListener(this);
        }
    }
}
