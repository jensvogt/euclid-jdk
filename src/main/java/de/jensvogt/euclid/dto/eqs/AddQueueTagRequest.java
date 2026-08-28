package de.jensvogt.euclid.dto.eqs;

/**
 * Request to add a tag to a queue.
 *
 * @param ern   queue ERN
 * @param key   tag key
 * @param value tag value
 */
public record AddQueueTagRequest(String ern, String key, String value) {

    /**
     * Creates a new instance of the Builder for constructing an AddQueueTagRequest object.
     *
     * @return a new Builder instance for constructing AddQueueTagRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link AddQueueTagRequest} instances.
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
         * The tag key.
         */
        private String key;

        /**
         * The tag value.
         */
        private String value;

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
         * Sets the tag key.
         *
         * @param key the tag key
         * @return the builder instance
         */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /**
         * Sets the tag value.
         *
         * @param value the tag value
         * @return the builder instance
         */
        public Builder value(String value) {
            this.value = value;
            return this;
        }

        /**
         * Builds and returns a new instance of AddQueueTagRequest using the properties set on the Builder.
         *
         * @return a new AddQueueTagRequest instance.
         */
        public AddQueueTagRequest build() {
            return new AddQueueTagRequest(ern, key, value);
        }
    }
}
