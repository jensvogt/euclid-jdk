package de.jensvogt.euclid.dto.eqs;

/**
 * Request to change how long a message sent to a queue is held back before it can be received.
 *
 * <p>Applies to what is sent from here on. A message already waiting keeps the moment it was given
 * when it arrived - its delay was turned into a timestamp then, and changing the queue's figure does
 * not go back and move it.
 *
 * @param ern   the ERN (Entity Resource Name) of the queue
 * @param delay the new delay, in seconds, between 0 and 900. Zero is no delay at all
 */
public record SetQueueDelayRequest(String ern, long delay) {

    /**
     * Creates a new instance of the Builder for constructing a SetQueueDelayRequest object.
     *
     * @return a new Builder instance for constructing SetQueueDelayRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SetQueueDelayRequest} instances.
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
         * The new delay, in seconds.
         */
        private long delay;

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
         * Sets the new delay.
         *
         * @param delay the delay in seconds, between 0 and 900
         * @return the builder instance
         */
        public Builder delay(long delay) {
            this.delay = delay;
            return this;
        }

        /**
         * Builds and returns a new instance of SetQueueDelayRequest using the properties set on the Builder.
         *
         * @return a new SetQueueDelayRequest instance.
         */
        public SetQueueDelayRequest build() {
            return new SetQueueDelayRequest(ern, delay);
        }
    }
}
