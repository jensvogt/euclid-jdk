package de.jensvogt.euclid.dto.eqs;

/**
 * Request to purge (delete all messages from) a queue.
 *
 * @param ern the ERN (Entity Resource Name) of the queue to purge
 */
public record PurgeQueueRequest(String ern) {

    /**
     * Creates a new instance of the Builder for constructing a PurgeQueueRequest object.
     *
     * @return a new Builder instance for constructing PurgeQueueRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link PurgeQueueRequest} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN (Entity Resource Name) of the queue to purge.
         */
        private String ern;

        /**
         * Sets the ERN of the queue to purge.
         *
         * @param ern the ERN of the queue
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Builds and returns a new instance of PurgeQueueRequest using the properties set on the Builder.
         *
         * @return a new PurgeQueueRequest instance populated with the ERN value.
         */
        public PurgeQueueRequest build() {
            return new PurgeQueueRequest(ern);
        }
    }
}
