package de.jensvogt.euclid.dto.eqs;

public record GetMessageMetadataRequest(String messageId) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        private String messageId;

        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public GetMessageMetadataRequest build() {
            return new GetMessageMetadataRequest(messageId);
        }
    }
}
