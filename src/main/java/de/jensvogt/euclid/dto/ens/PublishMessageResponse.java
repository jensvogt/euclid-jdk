package de.jensvogt.euclid.dto.ens;

/**
 * Response returned after a message has been accepted by a topic.
 *
 * @param messageId the ID assigned to the new message
 */
public record PublishMessageResponse(String messageId) {

    /**
     * Creates a new instance of the Builder for constructing a PublishMessageResponse object.
     *
     * @return a new Builder instance for constructing PublishMessageResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link PublishMessageResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ID assigned to the new message.
         */
        private String messageId;

        /**
         * Sets the ID assigned to the new message.
         *
         * @param messageId the ID assigned to the new message
         * @return the builder instance
         */
        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        /**
         * Builds and returns a new instance of PublishMessageResponse using the properties set on the Builder.
         *
         * @return a new PublishMessageResponse instance.
         */
        public PublishMessageResponse build() {
            return new PublishMessageResponse(messageId);
        }
    }
}
