package de.jensvogt.euclid.dto.eqs;

/**
 * Request to receive one or more messages from a queue.
 *
 * @param ern      the ERN (Entity Resource Name) of the queue to receive messages from
 * @param maxCount the maximum number of messages to receive
 * @param waitTime the maximum time, in seconds, to wait for messages to become available
 */
public record ReceiveMessagesRequest(String ern, long maxCount, long waitTime) {

    /**
     * Creates a new instance of the Builder for constructing a ReceiveMessagesRequest object.
     *
     * @return a new Builder instance for constructing ReceiveMessagesRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ReceiveMessagesRequest} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN (Entity Resource Name) of the queue to receive messages from.
         */
        private String ern;

        /**
         * The maximum number of messages to receive.
         */
        private long maxCount;

        /**
         * The maximum time, in seconds, to wait for messages to become available.
         */
        private long waitTime;

        /**
         * Sets the ERN of the queue to receive messages from.
         *
         * @param ern the queue ERN
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the maximum number of messages to receive.
         *
         * @param maxCount the maximum number of messages
         * @return the builder instance
         */
        public Builder maxCount(long maxCount) {
            this.maxCount = maxCount;
            return this;
        }

        /**
         * Sets the maximum time, in seconds, to wait for messages to become available.
         *
         * @param waitTime the wait time in seconds
         * @return the builder instance
         */
        public Builder waitTime(long waitTime) {
            this.waitTime = waitTime;
            return this;
        }

        /**
         * Builds and returns a new instance of ReceiveMessagesRequest using the properties set on the Builder.
         *
         * @return a new ReceiveMessagesRequest instance populated with the ERN, max count and wait time values.
         */
        public ReceiveMessagesRequest build() {
            return new ReceiveMessagesRequest(ern, maxCount, waitTime);
        }
    }
}
