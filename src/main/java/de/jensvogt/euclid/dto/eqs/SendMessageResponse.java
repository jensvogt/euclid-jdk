package de.jensvogt.euclid.dto.eqs;

/**
 * Response returned after successfully sending a message.
 *
 * @param messageId     the id of the sent message
 * @param md5Body       MD5 checksum of the message body
 * @param md5Attributes MD5 checksum of the message attributes
 */
public record SendMessageResponse(String messageId, String md5Body, String md5Attributes) {

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
         * The id of the sent message.
         */
        private String messageId;

        /**
         * MD5 checksum of the message body.
         */
        private String md5Body;

        /**
         * MD5 checksum of the message attributes.
         */
        private String md5Attributes;

        /**
         * Sets the id of the sent message.
         *
         * @param messageId the message id
         * @return the builder instance
         */
        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        /**
         * Sets the MD5 checksum of the message body.
         *
         * @param md5Body the MD5 checksum of the body
         * @return the builder instance
         */
        public Builder md5Body(String md5Body) {
            this.md5Body = md5Body;
            return this;
        }

        /**
         * Sets the MD5 checksum of the message attributes.
         *
         * @param md5Attributes the MD5 checksum of the attributes
         * @return the builder instance
         */
        public Builder md5Attributes(String md5Attributes) {
            this.md5Attributes = md5Attributes;
            return this;
        }

        /**
         * Builds and returns a new instance of SendMessageResponse using the properties set on the Builder.
         *
         * @return a new SendMessageResponse instance populated with the message id and MD5 checksum values.
         */
        public SendMessageResponse build() {
            return new SendMessageResponse(messageId, md5Body, md5Attributes);
        }
    }
}
