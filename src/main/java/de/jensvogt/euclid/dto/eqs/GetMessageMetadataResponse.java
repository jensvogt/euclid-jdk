package de.jensvogt.euclid.dto.eqs;

/**
 * Response carrying a message's metadata, everything about it except the body itself.
 *
 * @param messageId         the message ID
 * @param queueErn          ERN of the queue the message belongs to
 * @param receiptHandle     receipt handle from the last receive, empty if never received
 * @param status            current message status
 * @param priority          the message priority
 * @param size              size of the message body in bytes
 * @param receivedCount     number of times the message has been received
 * @param visibilityTimeout seconds the message stays invisible after a receive
 * @param contentType       the message body's content type
 * @param md5Body           MD5 checksum of the message body
 * @param md5Attributes     MD5 checksum of the message's attributes
 * @param created           creation timestamp
 * @param modified          last-modified timestamp
 */
public record GetMessageMetadataResponse(String messageId, String queueErn, String receiptHandle, String status,
                                         String priority, long size, long receivedCount, long visibilityTimeout,
                                         String contentType, String md5Body, String md5Attributes, String created,
                                         String modified) {

    /**
     * Creates a new instance of the Builder for constructing a GetMessageMetadataResponse object.
     *
     * @return a new Builder instance for constructing GetMessageMetadataResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetMessageMetadataResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The message ID.
         */
        private String messageId;

        /**
         * ERN of the queue the message belongs to.
         */
        private String queueErn;

        /**
         * Receipt handle from the last receive, empty if never received.
         */
        private String receiptHandle;

        /**
         * Current message status.
         */
        private String status;

        /**
         * The message priority.
         */
        private String priority;

        /**
         * Size of the message body in bytes.
         */
        private long size = 0;

        /**
         * Number of times the message has been received.
         */
        private long receivedCount = 0;

        /**
         * Seconds the message stays invisible after a receive.
         */
        private long visibilityTimeout = 0;

        /**
         * The message body's content type.
         */
        private String contentType;

        /**
         * MD5 checksum of the message body.
         */
        private String md5Body;

        /**
         * MD5 checksum of the message's attributes.
         */
        private String md5Attributes;

        /**
         * Creation timestamp.
         */
        private String created;

        /**
         * Last-modified timestamp.
         */
        private String modified;

        /**
         * Sets the message ID.
         *
         * @param messageId the message ID
         * @return the builder instance
         */
        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        /**
         * Sets ERN of the queue the message belongs to.
         *
         * @param queueErn ERN of the queue the message belongs to
         * @return the builder instance
         */
        public Builder queueErn(String queueErn) {
            this.queueErn = queueErn;
            return this;
        }

        /**
         * Sets receipt handle from the last receive, empty if never received.
         *
         * @param receiptHandle receipt handle from the last receive, empty if never received
         * @return the builder instance
         */
        public Builder receiptHandle(String receiptHandle) {
            this.receiptHandle = receiptHandle;
            return this;
        }

        /**
         * Sets current message status.
         *
         * @param status current message status
         * @return the builder instance
         */
        public Builder status(String status) {
            this.status = status;
            return this;
        }

        /**
         * Sets the message priority.
         *
         * @param priority the message priority
         * @return the builder instance
         */
        public Builder priority(String priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Sets size of the message body in bytes.
         *
         * @param size size of the message body in bytes
         * @return the builder instance
         */
        public Builder size(long size) {
            this.size = size;
            return this;
        }

        /**
         * Sets number of times the message has been received.
         *
         * @param receivedCount number of times the message has been received
         * @return the builder instance
         */
        public Builder receivedCount(long receivedCount) {
            this.receivedCount = receivedCount;
            return this;
        }

        /**
         * Sets seconds the message stays invisible after a receive.
         *
         * @param visibilityTimeout seconds the message stays invisible after a receive
         * @return the builder instance
         */
        public Builder visibilityTimeout(long visibilityTimeout) {
            this.visibilityTimeout = visibilityTimeout;
            return this;
        }

        /**
         * Sets the message body's content type.
         *
         * @param contentType the message body's content type
         * @return the builder instance
         */
        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        /**
         * Sets MD5 checksum of the message body.
         *
         * @param md5Body MD5 checksum of the message body
         * @return the builder instance
         */
        public Builder md5Body(String md5Body) {
            this.md5Body = md5Body;
            return this;
        }

        /**
         * Sets MD5 checksum of the message's attributes.
         *
         * @param md5Attributes MD5 checksum of the message's attributes
         * @return the builder instance
         */
        public Builder md5Attributes(String md5Attributes) {
            this.md5Attributes = md5Attributes;
            return this;
        }

        /**
         * Sets creation timestamp.
         *
         * @param created creation timestamp
         * @return the builder instance
         */
        public Builder created(String created) {
            this.created = created;
            return this;
        }

        /**
         * Sets last-modified timestamp.
         *
         * @param modified last-modified timestamp
         * @return the builder instance
         */
        public Builder modified(String modified) {
            this.modified = modified;
            return this;
        }

        /**
         * Builds and returns a new instance of GetMessageMetadataResponse using the properties set on the Builder.
         *
         * @return a new GetMessageMetadataResponse instance.
         */
        public GetMessageMetadataResponse build() {
            return new GetMessageMetadataResponse(messageId, queueErn, receiptHandle, status, priority, size, receivedCount, visibilityTimeout, contentType, md5Body, md5Attributes, created, modified);
        }
    }
}
