package de.jensvogt.euclid.dto.eqs;

public record GetMessageMetadataResponse(String messageId, String queueErn, String receiptHandle, String status,
                                          String priority, long size, long receivedCount, long visibilityTimeout,
                                          String contentType, String md5Body, String md5Attributes, String created,
                                          String modified) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String messageId;
        private String queueErn;
        private String receiptHandle;
        private String status;
        private String priority;
        private long size;
        private long receivedCount;
        private long visibilityTimeout;
        private String contentType;
        private String md5Body;
        private String md5Attributes;
        private String created;
        private String modified;

        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder queueErn(String queueErn) {
            this.queueErn = queueErn;
            return this;
        }

        public Builder receiptHandle(String receiptHandle) {
            this.receiptHandle = receiptHandle;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder priority(String priority) {
            this.priority = priority;
            return this;
        }

        public Builder size(long size) {
            this.size = size;
            return this;
        }

        public Builder receivedCount(long receivedCount) {
            this.receivedCount = receivedCount;
            return this;
        }

        public Builder visibilityTimeout(long visibilityTimeout) {
            this.visibilityTimeout = visibilityTimeout;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
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

        public Builder created(String created) {
            this.created = created;
            return this;
        }

        public Builder modified(String modified) {
            this.modified = modified;
            return this;
        }

        public GetMessageMetadataResponse build() {
            return new GetMessageMetadataResponse(messageId, queueErn, receiptHandle, status, priority, size,
                    receivedCount, visibilityTimeout, contentType, md5Body, md5Attributes, created, modified);
        }
    }
}
