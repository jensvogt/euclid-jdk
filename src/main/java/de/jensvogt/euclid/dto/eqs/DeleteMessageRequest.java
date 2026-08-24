package de.jensvogt.euclid.dto.eqs;

/**
 * Request to delete a message from a queue.
 *
 * @param receiptHandle the receipt handle identifying the message to delete
 */
public record DeleteMessageRequest(String receiptHandle) {

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
         * The receipt handle identifying the message to delete.
         */
        private String receiptHandle;

        /**
         * Sets the receipt handle of the message to delete.
         *
         * @param receiptHandle the receipt handle
         * @return the builder instance
         */
        public Builder receiptHandle(String receiptHandle) {
            this.receiptHandle = receiptHandle;
            return this;
        }

        /**
         * Builds and returns a new instance of DeleteMessageRequest using the properties set on the Builder.
         *
         * @return a new DeleteMessageRequest instance populated with the receipt handle value.
         */
        public DeleteMessageRequest build() {
            return new DeleteMessageRequest(receiptHandle);
        }
    }
}
