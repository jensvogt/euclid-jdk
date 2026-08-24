package de.jensvogt.euclid.dto.eqs;

import de.jensvogt.euclid.dto.eqs.model.Message;

import java.util.List;

/**
 * Response containing the messages received from a queue.
 *
 * @param messages the list of received messages
 * @param total    the total number of messages returned
 */
public record ReceiveMessagesResponse(List<Message> messages, long total) {

    /**
     * Creates a new instance of the Builder for constructing a ReceiveMessagesResponse object.
     *
     * @return a new Builder instance for constructing ReceiveMessagesResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ReceiveMessagesResponse} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The list of received messages.
         */
        private List<Message> messages;

        /**
         * The total number of messages returned.
         */
        private long total;

        /**
         * Sets the list of received messages.
         *
         * @param messages the list of messages
         * @return the builder instance
         */
        public Builder messages(List<Message> messages) {
            this.messages = messages;
            return this;
        }

        /**
         * Sets the total number of messages returned.
         *
         * @param total the total number of messages
         * @return the builder instance
         */
        public Builder total(long total) {
            this.total = total;
            return this;
        }

        /**
         * Builds and returns a new instance of ReceiveMessagesResponse using the properties set on the Builder.
         *
         * @return a new ReceiveMessagesResponse instance populated with the messages and total values.
         */
        public ReceiveMessagesResponse build() {
            return new ReceiveMessagesResponse(messages, total);
        }
    }
}
