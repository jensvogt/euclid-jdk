package de.jensvogt.euclid.dto.eqs;

/**
 * Request to replace the body of a message already on a queue.
 *
 * <p>The whole body, not a patch: a message body is opaque to euclid, so there is nothing that could
 * merge two of them.
 *
 * @param messageId message ID of the message to rewrite
 * @param body      the new body, which may be empty
 */
public record UpdateMessageBodyRequest(String messageId, String body) {

    /**
     * Creates a new instance of the Builder for constructing an UpdateMessageBodyRequest object.
     *
     * @return a new Builder instance for constructing UpdateMessageBodyRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link UpdateMessageBodyRequest} instances.
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
         * The new body.
         */
        private String body;

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
         * Sets the new body.
         *
         * @param body the new message body
         * @return the builder instance
         */
        public Builder body(String body) {
            this.body = body;
            return this;
        }

        /**
         * Builds and returns a new instance of UpdateMessageBodyRequest using the properties set on the Builder.
         *
         * @return a new UpdateMessageBodyRequest instance.
         */
        public UpdateMessageBodyRequest build() {
            return new UpdateMessageBodyRequest(messageId, body);
        }
    }
}
