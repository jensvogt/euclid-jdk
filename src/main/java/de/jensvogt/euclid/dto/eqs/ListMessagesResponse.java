package de.jensvogt.euclid.dto.eqs;

import de.jensvogt.euclid.dto.eqs.model.Message;

import java.util.List;

/**
 * Response containing a page of messages.
 *
 * @param messages the list of messages returned
 * @param total    the total number of messages matching the request, across all pages
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
         * The list of messages returned.
         */
        private List<Message> messages;

        /**
         * The total number of messages matching the request, across all pages.
         */
        private long total;

        /**
         * Sets the list of messages.
         *
         * @param messages the list of messages returned
         * @return the builder instance
         */
        public Builder messages(List<Message> messages) {
            this.messages = messages;
            return this;
        }

        /**
         * Sets the total number of messages matching the request.
         *
         * @param total the total number of messages, across all pages
         * @return the builder instance
         */
        public Builder total(long total) {
            this.total = total;
            return this;
        }

        /**
         * Builds and returns a new instance of ListMessagesResponse using the properties set on the Builder.
         *
         * @return a new ListMessagesResponse instance populated with the messages and total values.
         */
        public ListMessagesResponse build() {
            return new ListMessagesResponse(messages, total);
        }
    }
}
