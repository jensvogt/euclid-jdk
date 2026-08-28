package de.jensvogt.euclid.dto.ens;

/**
 * Request to delete all messages from a topic.
 *
 * @param ern topic ERN
 */
public record PurgeTopicRequest(String ern) {

    /**
     * Creates a new instance of the Builder for constructing a PurgeTopicRequest object.
     *
     * @return a new Builder instance for constructing PurgeTopicRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link PurgeTopicRequest} instances.
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
         * Builds and returns a new instance of PurgeTopicRequest using the properties set on the Builder.
         *
         * @return a new PurgeTopicRequest instance.
         */
        public PurgeTopicRequest build() {
            return new PurgeTopicRequest(ern);
        }
    }
}
