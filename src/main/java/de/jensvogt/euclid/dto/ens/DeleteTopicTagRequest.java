package de.jensvogt.euclid.dto.ens;

/**
 * Request to delete a tag from a topic.
 *
 * @param ern topic ERN
 * @param key tag key to delete
 */
public record DeleteTopicTagRequest(String ern, String key) {

    /**
     * Creates a new instance of the Builder for constructing a DeleteTopicTagRequest object.
     *
     * @return a new Builder instance for constructing DeleteTopicTagRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link DeleteTopicTagRequest} instances.
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
         * The tag key to delete.
         */
        private String key;

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
         * Builds and returns a new instance of DeleteTopicTagRequest using the properties set on the Builder.
         *
         * @return a new DeleteTopicTagRequest instance.
         */
        public DeleteTopicTagRequest build() {
            return new DeleteTopicTagRequest(ern, key);
        }
    }
}
