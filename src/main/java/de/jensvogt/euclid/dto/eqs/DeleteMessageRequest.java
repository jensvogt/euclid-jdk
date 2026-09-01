package de.jensvogt.euclid.dto.eqs;

/**
 * Request to delete a single message, addressed either by receipt handle or by message ID.
 * <p>
 * The receipt handle is the SQS-compatible way and only works for a message that was received,
 * failing once its visibility timeout has expired. The message ID deletes the message directly,
 * including one that has never been received - a Euclid extension with no AWS SQS equivalent.
 * Exactly one of the two is set; the server rejects a request carrying neither.
 *
 * @param receiptHandle receipt handle of a received message, or empty to delete by ID
 * @param messageId     ID of the message to delete, or empty to delete by receipt handle
 */
public record DeleteMessageRequest(String receiptHandle, String messageId) {

    /**
     * Creates a new instance of the Builder for constructing a DeleteMessageRequest object.
     *
     * @return a new Builder instance for constructing DeleteMessageRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link DeleteMessageRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Receipt handle of a received message, or empty to delete by ID.
         */
        private String receiptHandle = "";

        /**
         * ID of the message to delete, or empty to delete by receipt handle.
         */
        private String messageId = "";

        /**
         * Sets receipt handle of a received message, or empty to delete by ID.
         *
         * @param receiptHandle receipt handle of a received message, or empty to delete by ID
         * @return the builder instance
         */
        public Builder receiptHandle(String receiptHandle) {
            this.receiptHandle = receiptHandle;
            return this;
        }

        /**
         * Sets ID of the message to delete, or empty to delete by receipt handle.
         *
         * @param messageId ID of the message to delete, or empty to delete by receipt handle
         * @return the builder instance
         */
        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        /**
         * Builds and returns a new instance of DeleteMessageRequest using the properties set on the Builder.
         *
         * @return a new DeleteMessageRequest instance.
         */
        public DeleteMessageRequest build() {
            return new DeleteMessageRequest(receiptHandle, messageId);
        }
    }
}
