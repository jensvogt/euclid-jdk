package de.jensvogt.euclid.dto.ens;

import de.jensvogt.euclid.dto.com.Variant;

/**
 * Response to a get-message-attribute request.
 *
 * @param messageId message ID
 * @param key       key of the attribute
 * @param value     the attribute's value
 */
public record GetMessageAttributeResponse(String messageId, String key, Variant value) {

    /**
     * Creates a new instance of the Builder for constructing a GetMessageAttributeResponse object.
     *
     * @return a new Builder instance for constructing GetMessageAttributeResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetMessageAttributeResponse} instances.
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
         * The key of the attribute.
         */
        private String key;

        /**
         * The attribute's value.
         */
        private Variant value;

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
         * Sets the key of the attribute.
         *
         * @param key the attribute key
         * @return the builder instance
         */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /**
         * Sets the attribute's value.
         *
         * @param value the attribute value
         * @return the builder instance
         */
        public Builder value(Variant value) {
            this.value = value;
            return this;
        }

        /**
         * Builds and returns a new instance of GetMessageAttributeResponse using the properties set on the Builder.
         *
         * @return a new GetMessageAttributeResponse instance.
         */
        public GetMessageAttributeResponse build() {
            return new GetMessageAttributeResponse(messageId, key, value);
        }
    }
}
