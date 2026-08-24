package de.jensvogt.euclid.dto.eqs;

/**
 * Request to change the visibility timeout of a message.
 *
 * @param messageId  the id of the message to change the visibility of
 * @param visibility the new visibility timeout, in seconds
 */
public record SetMessageVisibilityRequest(String messageId, long visibility) {

    /**
     * Creates a new instance of the Builder for constructing a SetMessageVisibilityRequest object.
     *
     * @return a new Builder instance for constructing SetMessageVisibilityRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SetMessageVisibilityRequest} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The id of the message to change the visibility of.
         */
        private String messageId;

        /**
         * The new visibility timeout, in seconds.
         */
        private long visibility;

        /**
         * Sets the id of the message.
         *
         * @param messageId the id of the message to change the visibility of
         * @return the builder instance
         */
        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        /**
         * Sets the new visibility timeout, in seconds.
         *
         * @param visibility the visibility timeout in seconds
         * @return the builder instance
         */
        public Builder visibility(long visibility) {
            this.visibility = visibility;
            return this;
        }

        /**
         * Builds and returns a new instance of SetMessageVisibilityRequest using the properties set on the Builder.
         *
         * @return a new SetMessageVisibilityRequest instance populated with the message id and visibility values.
         */
        public SetMessageVisibilityRequest build() {
            return new SetMessageVisibilityRequest(messageId, visibility);
        }
    }
}
