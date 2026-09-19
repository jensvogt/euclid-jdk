package de.jensvogt.euclid.dto.eqs;

/**
 * Request for one message, by its id.
 * <p>
 * The message id, not the receipt handle: a receipt handle belongs to one delivery and is void once
 * that delivery's claim has expired, while the id names the message for as long as it exists.
 *
 * @param messageId the message's id
 */
public record GetMessageRequest(String messageId) {

    /**
     * Creates a new instance of the Builder for constructing a GetMessageRequest object.
     *
     * @return a new Builder instance for constructing GetMessageRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetMessageRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The message's id.
         */
        private String messageId;

        /**
         * Sets the id of the message.
         *
         * @param messageId the message's id
         * @return the builder instance
         */
        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        /**
         * Builds and returns a new instance of GetMessageRequest using the properties set on the Builder.
         *
         * @return a new GetMessageRequest instance.
         */
        public GetMessageRequest build() {
            return new GetMessageRequest(messageId);
        }
    }
}
