package de.jensvogt.euclid.dto.ens;

/**
 * Response to a get-topic-ern request.
 *
 * @param name topic name
 * @param ern  topic ERN
 */
public record GetTopicErnResponse(String name, String ern) {

    /**
     * Creates a new instance of the Builder for constructing a GetTopicErnResponse object.
     *
     * @return a new Builder instance for constructing GetTopicErnResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetTopicErnResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The topic name.
         */
        private String name;

        /**
         * The topic ERN.
         */
        private String ern;

        /**
         * Sets the topic name.
         *
         * @param name the topic name
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

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
         * Builds and returns a new instance of GetTopicErnResponse using the properties set on the Builder.
         *
         * @return a new GetTopicErnResponse instance.
         */
        public GetTopicErnResponse build() {
            return new GetTopicErnResponse(name, ern);
        }
    }
}
