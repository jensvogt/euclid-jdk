package de.jensvogt.euclid.dto.eqs;

import de.jensvogt.euclid.dto.eqs.model.Message;

/**
 * One message, as a listing describes each of its own.
 * <p>
 * The same {@link Message} a listing answer carries, rather than a shape of its own, so that what a
 * listing shows and what this shows cannot drift apart.
 *
 * @param message the message
 */
public record GetMessageResponse(Message message) {

    /**
     * Creates a new instance of the Builder for constructing a GetMessageResponse object.
     *
     * @return a new Builder instance for constructing GetMessageResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetMessageResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The message.
         */
        private Message message;

        /**
         * Sets the message.
         *
         * @param message the message
         * @return the builder instance
         */
        public Builder message(Message message) {
            this.message = message;
            return this;
        }

        /**
         * Builds and returns a new instance of GetMessageResponse using the properties set on the Builder.
         *
         * @return a new GetMessageResponse instance.
         */
        public GetMessageResponse build() {
            return new GetMessageResponse(message);
        }
    }
}
