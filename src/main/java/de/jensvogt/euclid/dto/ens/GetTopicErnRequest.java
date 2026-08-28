package de.jensvogt.euclid.dto.ens;

/**
 * Request to resolve a topic's ERN by name.
 *
 * @param name topic name
 */
public record GetTopicErnRequest(String name) {

    /**
     * Creates a new instance of the Builder for constructing a GetTopicErnRequest object.
     *
     * @return a new Builder instance for constructing GetTopicErnRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetTopicErnRequest} instances.
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
         * Builds and returns a new instance of GetTopicErnRequest using the properties set on the Builder.
         *
         * @return a new GetTopicErnRequest instance.
         */
        public GetTopicErnRequest build() {
            return new GetTopicErnRequest(name);
        }
    }
}
