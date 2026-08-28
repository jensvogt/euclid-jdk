package de.jensvogt.euclid.dto.ens;

/**
 * Request to set the value of an existing topic tag. The tag must already exist.
 *
 * @param ern   topic ERN
 * @param key   tag key
 * @param value tag value
 */
public record SetTopicTagRequest(String ern, String key, String value) {

    /**
     * Creates a new instance of the Builder for constructing a SetTopicTagRequest object.
     *
     * @return a new Builder instance for constructing SetTopicTagRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SetTopicTagRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The topic ERN.
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
         * Sets the topic ERN.
         *
         * @param ern the topic ERN
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
         * Builds and returns a new instance of SetTopicTagRequest using the properties set on the Builder.
         *
         * @return a new SetTopicTagRequest instance.
         */
        public SetTopicTagRequest build() {
            return new SetTopicTagRequest(ern, key, value);
        }
    }
}
