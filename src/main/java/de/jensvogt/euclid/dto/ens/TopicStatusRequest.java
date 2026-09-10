package de.jensvogt.euclid.dto.ens;

/**
 * Request body for the two actions that differ only in whether the topic delivers afterwards:
 * start-topic and stop-topic.
 *
 * @param ern the ERN of the topic to start or stop
 */
public record TopicStatusRequest(String ern) {

    /**
     * Creates a new instance of the Builder for constructing a TopicStatusRequest object.
     *
     * @return a new Builder instance for constructing TopicStatusRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link TopicStatusRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN of the topic to start or stop.
         */
        private String ern = "";

        /**
         * Sets the ERN of the topic the action applies to.
         *
         * @param ern the topic ERN
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Builds and returns a new instance of TopicStatusRequest using the properties set on the Builder.
         *
         * @return a new TopicStatusRequest instance.
         */
        public TopicStatusRequest build() {
            return new TopicStatusRequest(ern);
        }
    }
}
