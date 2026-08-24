package de.jensvogt.euclid.dto.eqs;

public record SendMessageResponse(String messageId, String md5Body, String md5Attributes) {

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
        private String md5Body;
        private String md5Attributes;

        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder md5Body(String md5Body) {
            this.md5Body = md5Body;
            return this;
        }

        public Builder md5Attributes(String md5Attributes) {
            this.md5Attributes = md5Attributes;
            return this;
        }

        public SendMessageResponse build() {
            return new SendMessageResponse(messageId, md5Body, md5Attributes);
        }
    }
}
