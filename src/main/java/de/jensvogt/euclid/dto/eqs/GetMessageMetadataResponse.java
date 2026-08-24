package de.jensvogt.euclid.dto.eqs;

/**
 * Response containing the metadata of a message, without its body.
 *
 * @param messageId         the id of the message
 * @param queueErn          the ERN (Entity Resource Name) of the queue the message belongs to
 * @param receiptHandle     handle to use for deleting or changing the visibility of this message
 * @param status            current status of the message
 * @param priority          the priority of the message
 * @param size              size in bytes of the message body
 * @param receivedCount     number of times this message has been received
 * @param visibilityTimeout the visibility timeout, in seconds, applied to this message
 * @param contentType       the content type of the message body
 * @param md5Body           MD5 checksum of the message body
 * @param md5Attributes     MD5 checksum of the message attributes
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
         * The id of the message.
         */
        private String messageId;

        /**
         * The ERN (Entity Resource Name) of the queue the message belongs to.
         */
        private String queueErn;

        /**
         * Handle to use for deleting or changing the visibility of this message.
         */
        private String receiptHandle;

        /**
         * Current status of the message.
         */
        private String status;

        /**
         * The priority of the message.
         */
        private String priority;

        /**
         * Size in bytes of the message body.
         */
        private long size;

        /**
         * Number of times this message has been received.
         */
        private long receivedCount;

        /**
         * The visibility timeout, in seconds, applied to this message.
         */
        private long visibilityTimeout;

        /**
         * The content type of the message body.
         */
        private String contentType;

        /**
         * MD5 checksum of the message body.
         */
        private String md5Body;

        /**
         * MD5 checksum of the message attributes.
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
         * Sets the id of the message.
         *
         * @param messageId the message id
         * @return the builder instance
         */
        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        /**
         * Sets the ERN of the queue the message belongs to.
         *
         * @param queueErn the queue ERN
         * @return the builder instance
         */
        public Builder queueErn(String queueErn) {
            this.queueErn = queueErn;
            return this;
        }

        /**
         * Sets the receipt handle of the message.
         *
         * @param receiptHandle handle to use for deleting or changing the visibility of this message
         * @return the builder instance
         */
        public Builder receiptHandle(String receiptHandle) {
            this.receiptHandle = receiptHandle;
            return this;
        }

        /**
         * Sets the status of the message.
         *
         * @param status the message status
         * @return the builder instance
         */
        public Builder status(String status) {
            this.status = status;
            return this;
        }

        /**
         * Sets the priority of the message.
         *
         * @param priority the message priority
         * @return the builder instance
         */
        public Builder priority(String priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Sets the size in bytes of the message body.
         *
         * @param size the size in bytes
         * @return the builder instance
         */
        public Builder size(long size) {
            this.size = size;
            return this;
        }

        /**
         * Sets the number of times this message has been received.
         *
         * @param receivedCount the received count
         * @return the builder instance
         */
        public Builder receivedCount(long receivedCount) {
            this.receivedCount = receivedCount;
            return this;
        }

        /**
         * Sets the visibility timeout, in seconds, applied to this message.
         *
         * @param visibilityTimeout the visibility timeout in seconds
         * @return the builder instance
         */
        public Builder visibilityTimeout(long visibilityTimeout) {
            this.visibilityTimeout = visibilityTimeout;
            return this;
        }

        /**
         * Sets the content type of the message body.
         *
         * @param contentType the content type
         * @return the builder instance
         */
        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        /**
         * Sets the MD5 checksum of the message body.
         *
         * @param md5Body the MD5 checksum of the body
         * @return the builder instance
         */
        public Builder md5Body(String md5Body) {
            this.md5Body = md5Body;
            return this;
        }

        /**
         * Sets the MD5 checksum of the message attributes.
         *
         * @param md5Attributes the MD5 checksum of the attributes
         * @return the builder instance
         */
        public Builder md5Attributes(String md5Attributes) {
            this.md5Attributes = md5Attributes;
            return this;
        }

        /**
         * Sets the creation timestamp.
         *
         * @param created the creation timestamp
         * @return the builder instance
         */
        public Builder created(String created) {
            this.created = created;
            return this;
        }

        /**
         * Sets the last-modified timestamp.
         *
         * @param modified the last-modified timestamp
         * @return the builder instance
         */
        public Builder modified(String modified) {
            this.modified = modified;
            return this;
        }

        /**
         * Builds and returns a new instance of GetMessageMetadataResponse using the properties set on the Builder.
         *
         * @return a new GetMessageMetadataResponse instance populated with the message metadata values.
         */
        public GetMessageMetadataResponse build() {
            return new GetMessageMetadataResponse(messageId, queueErn, receiptHandle, status, priority, size,
                    receivedCount, visibilityTimeout, contentType, md5Body, md5Attributes, created, modified);
        }
    }
}
