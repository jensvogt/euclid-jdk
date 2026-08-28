package de.jensvogt.euclid.dto.ens;

import de.jensvogt.euclid.dto.ens.model.Message;

import java.util.List;

/**
 * Response returned from list-messages.
 *
 * @param messages the messages
 * @param total    total number of messages in the topic
 */
public record ListMessagesResponse(List<Message> messages, long total) {

    /**
     * Creates a new instance of the Builder for constructing a ListMessagesResponse object.
     *
     * @return a new Builder instance for constructing ListMessagesResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListMessagesResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The messages.
         */
        private List<Message> messages;

        /**
         * The total number of messages in the topic.
         */
        private long total;

        /**
         * Sets the messages.
         *
         * @param messages the messages
         * @return the builder instance
         */
        public Builder messages(List<Message> messages) {
            this.messages = messages;
            return this;
        }

        /**
         * Sets the total number of messages in the topic.
         *
         * @param total the total number of messages
         * @return the builder instance
         */
        public Builder total(long total) {
            this.total = total;
            return this;
        }

        /**
         * Builds and returns a new instance of ListMessagesResponse using the properties set on the Builder.
         *
         * @return a new ListMessagesResponse instance.
         */
        public ListMessagesResponse build() {
            return new ListMessagesResponse(messages, total);
        }
    }
}
