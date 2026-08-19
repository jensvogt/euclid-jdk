package de.jensvogt.euclid.dto.sqs;

public record GetMessageAttributeRequest(String messageId, String name) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String messageId;
        private String name;

        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public GetMessageAttributeRequest build() {
            return new GetMessageAttributeRequest(messageId, name);
        }
    }
}
