package de.jensvogt.euclid.dto.ens;

/**
 * Request to create a new topic.
 *
 * @param name             topic name
 * @param maxMessageLength maximum allowed size, in bytes, of a single message
 */
public record CreateTopicRequest(String name, long maxMessageLength) {

    /**
     * Creates a new instance of the Builder for constructing a CreateTopicRequest object.
     *
     * @return a new Builder instance for constructing CreateTopicRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CreateTopicRequest} instances.
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
         * The maximum allowed message size, in bytes.
         */
        private long maxMessageLength = 1024 * 1024;

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
         * Sets the maximum allowed message size.
         *
         * @param maxMessageLength maximum size, in bytes, of a single message
         * @return the builder instance
         */
        public Builder maxMessageLength(long maxMessageLength) {
            this.maxMessageLength = maxMessageLength;
            return this;
        }

        /**
         * Builds and returns a new instance of CreateTopicRequest using the properties set on the Builder.
         *
         * @return a new CreateTopicRequest instance.
         */
        public CreateTopicRequest build() {
            return new CreateTopicRequest(name, maxMessageLength);
        }
    }
}
