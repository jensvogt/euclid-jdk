package de.jensvogt.euclid.dto.eqs;

/**
 * Request to delete a tag from a queue.
 *
 * @param ern queue ERN
 * @param key tag key to delete
 */
public record DeleteQueueTagRequest(String ern, String key) {

    /**
     * Creates a new instance of the Builder for constructing a DeleteQueueTagRequest object.
     *
     * @return a new Builder instance for constructing DeleteQueueTagRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link DeleteQueueTagRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The queue ERN.
         */
        private String ern;

        /**
         * The tag key to delete.
         */
        private String key;

        /**
         * Sets the queue ERN.
         *
         * @param ern the queue ERN
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the tag key to delete.
         *
         * @param key the tag key
         * @return the builder instance
         */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /**
         * Builds and returns a new instance of DeleteQueueTagRequest using the properties set on the Builder.
         *
         * @return a new DeleteQueueTagRequest instance.
         */
        public DeleteQueueTagRequest build() {
            return new DeleteQueueTagRequest(ern, key);
        }
    }
}
