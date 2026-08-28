package de.jensvogt.euclid.dto.ens;

/**
 * Request to retrieve the metadata of a topic.
 *
 * @param ern topic ERN
 */
public record GetTopicMetadataRequest(String ern) {

    /**
     * Creates a new instance of the Builder for constructing a GetTopicMetadataRequest object.
     *
     * @return a new Builder instance for constructing GetTopicMetadataRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetTopicMetadataRequest} instances.
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
         * Builds and returns a new instance of GetTopicMetadataRequest using the properties set on the Builder.
         *
         * @return a new GetTopicMetadataRequest instance.
         */
        public GetTopicMetadataRequest build() {
            return new GetTopicMetadataRequest(ern);
        }
    }
}
