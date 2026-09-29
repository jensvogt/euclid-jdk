package de.jensvogt.euclid.dto.ens;

/**
 * Request to replace the body of a message already published to a topic.
 *
 * <p>The whole body, not a patch: a message body is opaque to euclid, so there is nothing that could
 * merge two of them.
 *
 * <p>What this reaches is the copy ENS still holds - what {@code listMessages} and {@code getMessage}
 * answer with, and what a resend would send. A topic fans a message out to its subscribers when it is
 * published, so the copies that already left are past changing.
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
