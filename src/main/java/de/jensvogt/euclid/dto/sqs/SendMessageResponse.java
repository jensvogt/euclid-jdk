package de.jensvogt.euclid.dto.sqs;

public record SendMessageResponse(String messageId) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String messageId;

        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public SendMessageResponse build() {
            return new SendMessageResponse(messageId);
        }
    }
}
