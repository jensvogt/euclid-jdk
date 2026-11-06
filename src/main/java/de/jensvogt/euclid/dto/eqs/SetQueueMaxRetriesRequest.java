package de.jensvogt.euclid.dto.eqs;

/**
 * Request to change how often a queue hands out a message before moving it to its dead letter
 * queue - the queue's {@code maxReceiveCount}.
 *
 * <p>A message keeps the receive count it has; it is measured against the new limit on its next
 * receive. Lowering the limit below a message's count moves it to the dead letter queue then, not
 * now.
 *
 * @param ern        the ERN (Entity Resource Name) of the queue
 * @param maxRetries how many times a message is received before it goes to the dead letter queue,
 *                   between 1 and 1000 - the bounds AWS SQS holds {@code maxReceiveCount} to.
 *                   Anything outside is refused with HTTP 400
 */
public record SetQueueMaxRetriesRequest(String ern, long maxRetries) {

    /**
     * Creates a new instance of the Builder for constructing a SetQueueMaxRetriesRequest object.
     *
     * @return a new Builder instance for constructing SetQueueMaxRetriesRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SetQueueMaxRetriesRequest} instances.
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
        private String ern = "";

        /**
         * How many times a message is received before it goes to the dead letter queue.
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
         * Sets how many times a message is received before it goes to the dead letter queue.
         *
         * @param maxRetries the limit, between 1 and 1000
         * @return the builder instance
         */
        public Builder maxRetries(long maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        /**
         * Builds and returns a new instance of SetQueueMaxRetriesRequest using the properties set on the Builder.
         *
         * @return a new SetQueueMaxRetriesRequest instance.
         */
        public SetQueueMaxRetriesRequest build() {
            return new SetQueueMaxRetriesRequest(ern, maxRetries);
        }
    }
}
