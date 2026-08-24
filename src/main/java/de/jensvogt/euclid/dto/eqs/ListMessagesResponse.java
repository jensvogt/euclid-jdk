package de.jensvogt.euclid.dto.eqs;

import de.jensvogt.euclid.dto.eqs.model.Message;

import java.util.List;

public record ListMessagesResponse(List<Message> messages, long total) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        private List<Message> messages;
        private long total;

        public Builder messages(List<Message> messages) {
            this.messages = messages;
            return this;
        }

        public Builder total(long total) {
            this.total = total;
            return this;
        }

        public ListMessagesResponse build() {
            return new ListMessagesResponse(messages, total);
        }
    }
}
