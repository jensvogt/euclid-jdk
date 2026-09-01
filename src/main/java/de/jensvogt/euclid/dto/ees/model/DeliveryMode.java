package de.jensvogt.euclid.dto.ees.model;

/**
 * How a subscriber's events reach it.
 * <p>
 * The choice is between keeping events and only seeing them: a subscription that must not miss
 * anything while its application restarts is {@link #DURABLE}, and one that only wants to know
 * what is happening while somebody is watching is {@link #LIVE}.
 */
public enum DeliveryMode {

    /**
     * Every matching event is stored for the subscriber and kept until it is acknowledged, so
     * nothing is lost while the subscriber is away. A connected client is told that something is
     * waiting rather than being sent the event itself - two instances share a subscriber name, and
     * the claim in {@code receive-events} is what makes exactly one of them process each event.
     */
    DURABLE,

    /**
     * Nothing is stored. The event is pushed to whatever websocket sessions are attached to the
     * name and is then gone - what a view wants, and what costs no database write per event.
     */
    LIVE;

    /**
     * The wire value, as the {@code ees} module reads and reports it.
     *
     * @return "durable" or "live"
     */
    public String wireValue() {
        return name().toLowerCase();
    }

    /**
     * Reads a wire value back, defaulting to {@link #DURABLE} for anything unrecognized - losing
     * events because a mode was misspelled would be the worse failure.
     *
     * @param value the wire value, e.g. "live"
     * @return the mode it names
     */
    public static DeliveryMode fromWireValue(String value) {
        return "live".equalsIgnoreCase(value) ? LIVE : DURABLE;
    }
}
