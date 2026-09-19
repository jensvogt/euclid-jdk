package de.jensvogt.euclid.dto.ens;

/**
 * Request for one topic, by ERN or by name.
 * <p>
 * Exactly one of the two is sent. A name is resolved in the caller's own account and namespace, so
 * it means "my topic of that name"; an ERN names one topic in the installation.
 *
 * @param ern  the ERN uniquely identifying the topic, or null when it is named instead
 * @param name the topic's name, used when no ERN is given
 */
public record GetTopicRequest(String ern, String name) {

    /**
     * Creates a new instance of the Builder for constructing a GetTopicRequest object.
     *
     * @return a new Builder instance for constructing GetTopicRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetTopicRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN uniquely identifying the topic.
         */
        private String ern;

        /**
         * The topic's name.
         */
        private String name;

        /**
         * Sets the ERN of the topic.
         *
         * @param ern the ERN uniquely identifying the topic
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the name of the topic.
         *
         * @param name the topic's name, resolved in the caller's own account and namespace
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Builds and returns a new instance of GetTopicRequest using the properties set on the Builder.
         *
         * @return a new GetTopicRequest instance.
         */
        public GetTopicRequest build() {
            return new GetTopicRequest(ern, name);
        }
    }
}
