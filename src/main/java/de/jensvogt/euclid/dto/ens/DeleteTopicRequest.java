package de.jensvogt.euclid.dto.ens;

/**
 * Request to delete an existing topic.
 *
 * @param ern ERN of the topic to delete
 */
public record DeleteTopicRequest(String ern) {

    /**
     * Creates a new instance of the Builder for constructing a DeleteTopicRequest object.
     *
     * @return a new Builder instance for constructing DeleteTopicRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link DeleteTopicRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN of the topic to delete.
         */
        private String ern;

        /**
         * Sets the ERN of the topic to delete.
         *
         * @param ern the topic ERN
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Builds and returns a new instance of DeleteTopicRequest using the properties set on the Builder.
         *
         * @return a new DeleteTopicRequest instance.
         */
        public DeleteTopicRequest build() {
            return new DeleteTopicRequest(ern);
        }
    }
}
