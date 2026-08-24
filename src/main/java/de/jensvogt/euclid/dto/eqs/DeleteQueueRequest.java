package de.jensvogt.euclid.dto.eqs;

/**
 * Request to delete a queue.
 *
 * @param ern the ERN (Entity Resource Name) of the queue to delete
 */
public record DeleteQueueRequest(String ern) {

    /**
     * Creates a new instance of the Builder for constructing a DeleteQueueRequest object.
     *
     * @return a new Builder instance for constructing DeleteQueueRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link DeleteQueueRequest} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN (Entity Resource Name) of the queue to delete.
         */
        private String ern;

        /**
         * Sets the ERN of the queue to delete.
         *
         * @param ern the ERN of the queue
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Builds and returns a new instance of DeleteQueueRequest using the properties set on the Builder.
         *
         * @return a new DeleteQueueRequest instance populated with the ERN value.
         */
        public DeleteQueueRequest build() {
            return new DeleteQueueRequest(ern);
        }
    }
}
