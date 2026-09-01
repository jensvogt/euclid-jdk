package de.jensvogt.euclid.dto.eqs;

/**
 * Response returned after a message has been accepted by a queue.
 *
 * @param messageId     the ID assigned to the new message
 * @param md5Body       MD5 checksum of the message body, for verifying it arrived intact
 * @param md5Attributes MD5 checksum of the message's attributes
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
         * The ID assigned to the new message.
         */
        private String messageId;

        /**
         * MD5 checksum of the message body, for verifying it arrived intact.
         */
        private String md5Body;

        /**
         * MD5 checksum of the message's attributes.
         */
        private String md5Attributes;

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
         * Sets MD5 checksum of the message body, for verifying it arrived intact.
         *
         * @param md5Body MD5 checksum of the message body, for verifying it arrived intact
         * @return the builder instance
         */
        public Builder md5Body(String md5Body) {
            this.md5Body = md5Body;
            return this;
        }

        /**
         * Sets MD5 checksum of the message's attributes.
         *
         * @param md5Attributes MD5 checksum of the message's attributes
         * @return the builder instance
         */
        public Builder md5Attributes(String md5Attributes) {
            this.md5Attributes = md5Attributes;
            return this;
        }

        /**
         * Builds and returns a new instance of SendMessageResponse using the properties set on the Builder.
         *
         * @return a new SendMessageResponse instance.
         */
        public SendMessageResponse build() {
            return new SendMessageResponse(messageId, md5Body, md5Attributes);
        }
    }
}
