package de.jensvogt.euclid.dto.eqs;

/**
 * Response returned after a queue's {@code maxReceiveCount} has been changed.
 *
 * @param ern        the ERN (Entity Resource Name) of the queue
 * @param maxRetries how many times a message is now received before it goes to the dead letter
 *                   queue
 */
public record SetQueueMaxRetriesResponse(String ern, long maxRetries) {

    /**
     * Creates a new instance of the Builder for constructing a SetQueueMaxRetriesResponse object.
     *
     * @return a new Builder instance for constructing SetQueueMaxRetriesResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SetQueueMaxRetriesResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN of the queue.
         */
        private String ern;

        /**
         * How many times a message is now received before it goes to the dead letter queue.
         */
        private long maxRetries;

        /**
         * Sets the ERN of the queue.
         *
         * @param ern the queue ERN
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets how many times a message is now received before it goes to the dead letter queue.
         *
         * @param maxRetries the limit
         * @return the builder instance
         */
        public Builder maxRetries(long maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        /**
         * Builds and returns a new instance of SetQueueMaxRetriesResponse using the properties set on the Builder.
         *
         * @return a new SetQueueMaxRetriesResponse instance.
         */
        public SetQueueMaxRetriesResponse build() {
            return new SetQueueMaxRetriesResponse(ern, maxRetries);
        }
    }
}
