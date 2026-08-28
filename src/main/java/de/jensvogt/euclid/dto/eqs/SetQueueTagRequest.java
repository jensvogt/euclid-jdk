package de.jensvogt.euclid.dto.eqs;

/**
 * Request to set the value of an existing queue tag. The tag must already exist.
 *
 * @param ern   queue ERN
 * @param key   tag key
 * @param value tag value
 */
public record SetQueueTagRequest(String ern, String key, String value) {

    /**
     * Creates a new instance of the Builder for constructing a SetQueueTagRequest object.
     *
     * @return a new Builder instance for constructing SetQueueTagRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SetQueueTagRequest} instances.
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
         * Builds and returns a new instance of SetQueueTagRequest using the properties set on the Builder.
         *
         * @return a new SetQueueTagRequest instance.
         */
        public SetQueueTagRequest build() {
            return new SetQueueTagRequest(ern, key, value);
        }
    }
}
