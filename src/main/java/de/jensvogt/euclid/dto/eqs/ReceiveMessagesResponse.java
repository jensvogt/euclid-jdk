package de.jensvogt.euclid.dto.eqs;

import de.jensvogt.euclid.dto.eqs.model.Message;

import java.util.List;

public record ReceiveMessagesResponse(List<Message> messages, long total) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
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

        public ReceiveMessagesResponse build() {
            return new ReceiveMessagesResponse(messages, total);
        }
    }
}
