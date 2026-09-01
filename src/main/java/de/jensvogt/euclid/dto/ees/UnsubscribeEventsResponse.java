package de.jensvogt.euclid.dto.ees;

/**
 * The outcome of an unsubscribe-events action.
 *
 * @param subscriber the subscriber name
 * @param removed    number of subscriptions removed
 */
public record UnsubscribeEventsResponse(String subscriber, long removed) {

    /**
     * Creates a new instance of the Builder for constructing an UnsubscribeEventsResponse object.
     *
     * @return a new Builder instance for constructing UnsubscribeEventsResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link UnsubscribeEventsResponse} instances.
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
         * Number of subscriptions removed.
         */
        private long removed = 0;

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
         * Sets number of subscriptions removed.
         *
         * @param removed number of subscriptions removed
         * @return the builder instance
         */
        public Builder removed(long removed) {
            this.removed = removed;
            return this;
        }

        /**
         * Builds and returns a new instance of UnsubscribeEventsResponse using the properties set on the Builder.
         *
         * @return a new UnsubscribeEventsResponse instance.
         */
        public UnsubscribeEventsResponse build() {
            return new UnsubscribeEventsResponse(subscriber, removed);
        }
    }
}
