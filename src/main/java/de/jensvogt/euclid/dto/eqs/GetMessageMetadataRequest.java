package de.jensvogt.euclid.dto.eqs;

/**
 * Request to retrieve the metadata of a message.
 *
 * @param messageId the id of the message to retrieve the metadata for
 */
public record GetMessageMetadataRequest(String messageId) {

    /**
     * Creates a new instance of the Builder for constructing a GetMessageMetadataRequest object.
     *
     * @return a new Builder instance for constructing GetMessageMetadataRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetMessageMetadataRequest} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The id of the message to retrieve the metadata for.
         */
        private String messageId;

        /**
         * Sets the id of the message.
         *
         * @param messageId the id of the message to retrieve the metadata for
         * @return the builder instance
         */
        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        /**
         * Builds and returns a new instance of GetMessageMetadataRequest using the properties set on the Builder.
         *
         * @return a new GetMessageMetadataRequest instance populated with the message id value.
         */
        public GetMessageMetadataRequest build() {
            return new GetMessageMetadataRequest(messageId);
        }
    }
}
