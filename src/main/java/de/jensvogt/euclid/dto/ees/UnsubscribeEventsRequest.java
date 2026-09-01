package de.jensvogt.euclid.dto.ees;

/**
 * Request to remove an external subscription, and every event still waiting for it.
 *
 * @param name      the subscriber name
 * @param eventType the event type to stop receiving; empty removes all of this subscriber's subscriptions
 */
public record UnsubscribeEventsRequest(String name, String eventType) {

    /**
     * Creates a new instance of the Builder for constructing an UnsubscribeEventsRequest object.
     *
     * @return a new Builder instance for constructing UnsubscribeEventsRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link UnsubscribeEventsRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The subscriber name.
         */
        private String name;

        /**
         * The event type to stop receiving; empty removes all of this subscriber's subscriptions.
         */
        private String eventType = "";

        /**
         * Sets the subscriber name.
         *
         * @param name the subscriber name
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the event type to stop receiving; empty removes all of this subscriber's subscriptions.
         *
         * @param eventType the event type to stop receiving; empty removes all of this subscriber's subscriptions
         * @return the builder instance
         */
        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        /**
         * Builds and returns a new instance of UnsubscribeEventsRequest using the properties set on the Builder.
         *
         * @return a new UnsubscribeEventsRequest instance.
         */
        public UnsubscribeEventsRequest build() {
            return new UnsubscribeEventsRequest(name, eventType);
        }
    }
}
