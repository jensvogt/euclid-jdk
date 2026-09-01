package de.jensvogt.euclid.dto.eqs;

import de.jensvogt.euclid.dto.com.Variant;

/**
 * Request to set the value of a single message attribute, creating it if it doesn't exist yet.
 *
 * @param messageId message ID
 * @param key       key of the attribute to set
 * @param value     the attribute's new value
 */
public record SetMessageAttributeRequest(String messageId, String key, Variant value) {

    /**
     * Creates a new instance of the Builder for constructing a SetMessageAttributeRequest object.
     *
     * @return a new Builder instance for constructing SetMessageAttributeRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SetMessageAttributeRequest} instances.
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
         * The key of the attribute to set.
         */
        private String key;

        /**
         * The attribute's new value.
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
         * Sets the key of the attribute to set.
         *
         * @param key the attribute key
         * @return the builder instance
         */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /**
         * Sets the attribute's new value.
         *
         * @param value the attribute value
         * @return the builder instance
         */
        public Builder value(Variant value) {
            this.value = value;
            return this;
        }

        /**
         * Builds and returns a new instance of SetMessageAttributeRequest using the properties set on the Builder.
         *
         * @return a new SetMessageAttributeRequest instance.
         */
        public SetMessageAttributeRequest build() {
            return new SetMessageAttributeRequest(messageId, key, value);
        }
    }
}
