package de.jensvogt.euclid.dto.eqs;

/**
 * Response returned after a message has been accepted by a queue.
 *
 * @param messageId the ID assigned to the new message
 */
public record SendMessageResponse(String messageId) {

    /**
     * Creates a new instance of the Builder for constructing a SendMessageResponse object.
     *
     * @return a new Builder instance for constructing SendMessageResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SendMessageResponse} instances.
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
         * Builds and returns a new instance of SendMessageResponse using the properties set on the Builder.
         *
         * @return a new SendMessageResponse instance.
         */
        public SendMessageResponse build() {
            return new SendMessageResponse(messageId);
        }
    }
}
