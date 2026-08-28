package de.jensvogt.euclid.dto.ens;

/**
 * Response returned after successfully creating a topic.
 *
 * @param name the name of the created topic
 * @param ern  the ERN uniquely identifying the created topic
 */
public record CreateTopicResponse(String name, String ern) {

    /**
     * Creates a new instance of the Builder for constructing a CreateTopicResponse object.
     *
     * @return a new Builder instance for constructing CreateTopicResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CreateTopicResponse} instances.
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
         * Builds and returns a new instance of CreateTopicResponse using the properties set on the Builder.
         *
         * @return a new CreateTopicResponse instance.
         */
        public CreateTopicResponse build() {
            return new CreateTopicResponse(name, ern);
        }
    }
}
