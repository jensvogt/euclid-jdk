package de.jensvogt.euclid.dto.eqs;

import de.jensvogt.euclid.dto.com.Variant;

/**
 * Response containing a single attribute of a message.
 *
 * @param messageId the id of the message the attribute belongs to
 * @param name      the name of the attribute
 * @param value     the typed value of the attribute
 */
public record GetMessageAttributeResponse(String messageId, String name, Variant value) {

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
         * The id of the message the attribute belongs to.
         */
        private String messageId;

        /**
         * The name of the attribute.
         */
        private String name;

        /**
         * The typed value of the attribute.
         */
        private Variant value;

        /**
         * Sets the id of the message.
         *
         * @param messageId the id of the message the attribute belongs to
         * @return the builder instance
         */
        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        /**
         * Sets the name of the attribute.
         *
         * @param name the attribute name
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the typed value of the attribute.
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
         * @return a new GetMessageAttributeResponse instance populated with the message id, attribute name and value.
         */
        public GetMessageAttributeResponse build() {
            return new GetMessageAttributeResponse(messageId, name, value);
        }
    }
}
