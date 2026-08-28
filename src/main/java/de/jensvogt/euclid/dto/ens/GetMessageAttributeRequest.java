package de.jensvogt.euclid.dto.ens;

/**
 * Request to retrieve a single message attribute by key.
 *
 * @param messageId message ID
 * @param key       key of the attribute to look up
 */
public record GetMessageAttributeRequest(String messageId, String key) {

    /**
     * Creates a new instance of the Builder for constructing a GetMessageAttributeRequest object.
     *
     * @return a new Builder instance for constructing GetMessageAttributeRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetMessageAttributeRequest} instances.
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
         * The key of the attribute to look up.
         */
        private String key;

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
         * Sets the key of the attribute to look up.
         *
         * @param key the attribute key
         * @return the builder instance
         */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /**
         * Builds and returns a new instance of GetMessageAttributeRequest using the properties set on the Builder.
         *
         * @return a new GetMessageAttributeRequest instance.
         */
        public GetMessageAttributeRequest build() {
            return new GetMessageAttributeRequest(messageId, key);
        }
    }
}
