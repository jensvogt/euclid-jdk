package de.jensvogt.euclid.dto.ees;

/**
 * The outcome of an ack-events action.
 *
 * @param subscriber   the subscriber name
 * @param acknowledged number of events actually deleted
 * @param waiting      number of events still waiting for the subscriber
 */
public record AckEventsResponse(String subscriber, long acknowledged, long waiting) {

    /**
     * Creates a new instance of the Builder for constructing an AckEventsResponse object.
     *
     * @return a new Builder instance for constructing AckEventsResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link AckEventsResponse} instances.
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
        private String subscriber;

        /**
         * Number of events actually deleted.
         */
        private long acknowledged = 0;

        /**
         * Number of events still waiting for the subscriber.
         */
        private long waiting = 0;

        /**
         * Sets the subscriber name.
         *
         * @param subscriber the subscriber name
         * @return the builder instance
         */
        public Builder subscriber(String subscriber) {
            this.subscriber = subscriber;
            return this;
        }

        /**
         * Sets number of events actually deleted.
         *
         * @param acknowledged number of events actually deleted
         * @return the builder instance
         */
        public Builder acknowledged(long acknowledged) {
            this.acknowledged = acknowledged;
            return this;
        }

        /**
         * Sets number of events still waiting for the subscriber.
         *
         * @param waiting number of events still waiting for the subscriber
         * @return the builder instance
         */
        public Builder waiting(long waiting) {
            this.waiting = waiting;
            return this;
        }

        /**
         * Builds and returns a new instance of AckEventsResponse using the properties set on the Builder.
         *
         * @return a new AckEventsResponse instance.
         */
        public AckEventsResponse build() {
            return new AckEventsResponse(subscriber, acknowledged, waiting);
        }
    }
}
