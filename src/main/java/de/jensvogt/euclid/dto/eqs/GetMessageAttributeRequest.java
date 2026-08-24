package de.jensvogt.euclid.dto.eqs;

/**
 * Request to retrieve a single attribute of a message.
 *
 * @param messageId the id of the message to retrieve the attribute from
 * @param name      the name of the attribute to retrieve
 */
public record GetMessageAttributeRequest(String messageId, String name) {

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
         * The id of the message to retrieve the attribute from.
         */
        private String messageId;

        /**
         * The name of the attribute to retrieve.
         */
        private String name;

        /**
         * Sets the id of the message.
         *
         * @param messageId the id of the message to retrieve the attribute from
         * @return the builder instance
         */
        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        /**
         * Sets the name of the attribute.
         *
         * @param name the name of the attribute to retrieve
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Builds and returns a new instance of GetMessageAttributeRequest using the properties set on the Builder.
         *
         * @return a new GetMessageAttributeRequest instance populated with the message id and attribute name values.
         */
        public GetMessageAttributeRequest build() {
            return new GetMessageAttributeRequest(messageId, name);
        }
    }
}
