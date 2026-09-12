package de.jensvogt.euclid.dto.eqs;

/**
 * Response returned after a queue's delay has been changed.
 *
 * @param ern   the ERN (Entity Resource Name) of the queue
 * @param delay the delay the queue now has, in seconds
 */
public record SetQueueDelayResponse(String ern, long delay) {

    /**
     * Creates a new instance of the Builder for constructing a SetQueueDelayResponse object.
     *
     * @return a new Builder instance for constructing SetQueueDelayResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SetQueueDelayResponse} instances.
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
         * The delay the queue now has, in seconds.
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
         * Sets the delay the queue now has.
         *
         * @param delay the delay in seconds
         * @return the builder instance
         */
        public Builder delay(long delay) {
            this.delay = delay;
            return this;
        }

        /**
         * Builds and returns a new instance of SetQueueDelayResponse using the properties set on the Builder.
         *
         * @return a new SetQueueDelayResponse instance.
         */
        public SetQueueDelayResponse build() {
            return new SetQueueDelayResponse(ern, delay);
        }
    }
}
