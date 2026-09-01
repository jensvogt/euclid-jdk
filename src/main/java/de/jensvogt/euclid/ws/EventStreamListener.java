package de.jensvogt.euclid.ws;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Called as frames arrive on an {@link EuclidEventStream}.
 * <p>
 * The three methods are the three things the gateway can say to a connected client, and they mean
 * different things on purpose:
 * <ul>
 *   <li>{@link #onEvent} - here is an event. Only a live subscription receives these, because only
 *       a live subscription has nothing else that could carry it.</li>
 *   <li>{@link #onNotify} - something is waiting for you. A durable subscription is told this
 *       rather than being sent the event, since two instances share a subscriber name and the
 *       claim in {@code receive-events} is what decides which of them processes it.</li>
 *   <li>{@link #onLag} - the connection fell behind and events were dropped on the way out. For a
 *       durable subscription that is an instruction rather than a loss: the events are still in
 *       the store, so claim them.</li>
 * </ul>
 * Every method is called on the websocket's reading thread, which must not be blocked - anything
 * that talks to the server belongs on a thread of the listener's own. {@link EuclidEventListener}
 * does exactly that, and is what most callers want instead of implementing this directly.
 */
public interface EventStreamListener {

    /**
     * A live event arrived.
     *
     * @param topic the event type, e.g. "esm.object.created"
     * @param body  the event payload
     */
    default void onEvent(String topic, JsonNode body) {
    }

    /**
     * A durable subscription has events waiting.
     *
     * @param topic the event type that produced them
     */
    default void onNotify(String topic) {
    }

    /**
     * The gateway dropped events because this connection was not reading them fast enough.
     *
     * @param dropped how many were dropped
     */
    default void onLag(long dropped) {
    }
}
