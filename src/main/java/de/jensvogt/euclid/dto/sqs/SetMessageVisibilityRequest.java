package de.jensvogt.euclid.dto.sqs;

public record SetMessageVisibilityRequest(String messageId, long visibility) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String messageId;
        private long visibility;

        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder visibility(long visibility) {
            this.visibility = visibility;
            return this;
        }

        public SetMessageVisibilityRequest build() {
            return new SetMessageVisibilityRequest(messageId, visibility);
        }
    }
}
