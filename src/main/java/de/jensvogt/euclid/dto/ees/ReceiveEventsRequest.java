package de.jensvogt.euclid.dto.ees;

/**
 * Request to claim a subscriber's waiting events.
 *
 * @param name              the subscriber name
 * @param maxEvents         largest number of events to claim; the server floors this at 1
 * @param waitTime          seconds to long-poll for an event before answering empty, clamped to 20 by the server
 * @param visibilityTimeout seconds a claim holds before the events become claimable again
 */
public record ReceiveEventsRequest(String name, long maxEvents, long waitTime, long visibilityTimeout) {

    /**
     * Creates a new instance of the Builder for constructing a ReceiveEventsRequest object.
     *
     * @return a new Builder instance for constructing ReceiveEventsRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ReceiveEventsRequest} instances.
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
         * Largest number of events to claim; the server floors this at 1.
         */
        private long maxEvents = 10;

        /**
         * Seconds to long-poll for an event before answering empty, clamped to 20 by the server.
         */
        private long waitTime = 0;

        /**
         * Seconds a claim holds before the events become claimable again.
         */
        private long visibilityTimeout = 300;

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
         * Sets largest number of events to claim; the server floors this at 1.
         *
         * @param maxEvents largest number of events to claim; the server floors this at 1
         * @return the builder instance
         */
        public Builder maxEvents(long maxEvents) {
            this.maxEvents = maxEvents;
            return this;
        }

        /**
         * Sets seconds to long-poll for an event before answering empty, clamped to 20 by the server.
         *
         * @param waitTime seconds to long-poll for an event before answering empty, clamped to 20 by the server
         * @return the builder instance
         */
        public Builder waitTime(long waitTime) {
            this.waitTime = waitTime;
            return this;
        }

        /**
         * Sets seconds a claim holds before the events become claimable again.
         *
         * @param visibilityTimeout seconds a claim holds before the events become claimable again
         * @return the builder instance
         */
        public Builder visibilityTimeout(long visibilityTimeout) {
            this.visibilityTimeout = visibilityTimeout;
            return this;
        }

        /**
         * Builds and returns a new instance of ReceiveEventsRequest using the properties set on the Builder.
         *
         * @return a new ReceiveEventsRequest instance.
         */
        public ReceiveEventsRequest build() {
            return new ReceiveEventsRequest(name, maxEvents, waitTime, visibilityTimeout);
        }
    }
}
