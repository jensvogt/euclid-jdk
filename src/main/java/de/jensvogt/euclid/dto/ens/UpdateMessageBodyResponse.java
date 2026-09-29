package de.jensvogt.euclid.dto.ens;

/**
 * Response returned after a published message's body has been replaced.
 *
 * @param messageId    the message ID, unchanged - rewriting a body does not make a new message
 * @param topicErn     the ERN of the topic the message was published to
 * @param size         the new body's length in bytes
 * @param previousSize the length of the body that was replaced, which is the one thing a caller
 *                     cannot go back and check: it is gone by the time this answer arrives
 * @param contentType  the content type, derived afresh from the new body
 */
public record UpdateMessageBodyResponse(String messageId, String topicErn, long size, long previousSize,
                                        String contentType) {

    /**
     * Creates a new instance of the Builder for constructing an UpdateMessageBodyResponse object.
     *
     * @return a new Builder instance for constructing UpdateMessageBodyResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link UpdateMessageBodyResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The message ID.
         */
        private String messageId;

        /**
         * The ERN of the topic the message was published to.
         */
        private String topicErn;

        /**
         * The new body's length in bytes.
         */
        private long size;

        /**
         * The length of the body that was replaced.
         */
        private long previousSize;

        /**
         * The content type, derived afresh from the new body.
         */
        private String contentType;

        /**
         * Sets the message ID.
         *
         * @param messageId the message ID
         * @return the builder instance
         */
        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        /**
         * Sets the ERN of the topic the message was published to.
         *
         * @param topicErn the topic ERN
         * @return the builder instance
         */
        public Builder topicErn(String topicErn) {
            this.topicErn = topicErn;
            return this;
        }

        /**
         * Sets the new body's length in bytes.
         *
         * @param size the new size
         * @return the builder instance
         */
        public Builder size(long size) {
            this.size = size;
            return this;
        }

        /**
         * Sets the length of the body that was replaced.
         *
         * @param previousSize the previous size
         * @return the builder instance
         */
        public Builder previousSize(long previousSize) {
            this.previousSize = previousSize;
            return this;
        }

        /**
         * Sets the content type.
         *
         * @param contentType the content type derived from the new body
         * @return the builder instance
         */
        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        /**
         * Builds and returns a new instance of UpdateMessageBodyResponse using the properties set on the Builder.
         *
         * @return a new UpdateMessageBodyResponse instance.
         */
        public UpdateMessageBodyResponse build() {
            return new UpdateMessageBodyResponse(messageId, topicErn, size, previousSize, contentType);
        }
    }
}
