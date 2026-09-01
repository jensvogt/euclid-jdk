package de.jensvogt.euclid.dto.eqs;

/**
 * Request to create a queue.
 *
 * @param name             the name of the queue
 * @param visibility       visibility timeout in seconds
 * @param maxRetries       maximum number of receive attempts before a message goes to the dead letter queue
 * @param maxMessageLength maximum allowed message length in bytes
 * @param dlqName          name of the dead letter queue, or empty for none
 * @param delay            seconds a new message stays delayed before becoming available
 * @param priority         default priority for the queue's messages, overridable per send-message
 */
public record CreateQueueRequest(String name, long visibility, long maxRetries, long maxMessageLength, String dlqName,
                                 long delay, String priority) {

    /**
     * Creates a new instance of the Builder for constructing a CreateQueueRequest object.
     *
     * @return a new Builder instance for constructing CreateQueueRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CreateQueueRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The name of the queue.
         */
        private String name;

        /**
         * Visibility timeout in seconds.
         */
        private long visibility = 30;

        /**
         * Maximum number of receive attempts before a message goes to the dead letter queue.
         */
        private long maxRetries = 3;

        /**
         * Maximum allowed message length in bytes.
         */
        private long maxMessageLength = 1024 * 1024;

        /**
         * Name of the dead letter queue, or empty for none.
         */
        private String dlqName = "";

        /**
         * Seconds a new message stays delayed before becoming available.
         */
        private long delay = 0;

        /**
         * Default priority for the queue's messages, overridable per send-message.
         */
        private String priority = "MIDDLE";

        /**
         * Sets the name of the queue.
         *
         * @param name the name of the queue
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets visibility timeout in seconds.
         *
         * @param visibility visibility timeout in seconds
         * @return the builder instance
         */
        public Builder visibility(long visibility) {
            this.visibility = visibility;
            return this;
        }

        /**
         * Sets maximum number of receive attempts before a message goes to the dead letter queue.
         *
         * @param maxRetries maximum number of receive attempts before a message goes to the dead letter queue
         * @return the builder instance
         */
        public Builder maxRetries(long maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        /**
         * Sets maximum allowed message length in bytes.
         *
         * @param maxMessageLength maximum allowed message length in bytes
         * @return the builder instance
         */
        public Builder maxMessageLength(long maxMessageLength) {
            this.maxMessageLength = maxMessageLength;
            return this;
        }

        /**
         * Sets name of the dead letter queue, or empty for none.
         *
         * @param dlqName name of the dead letter queue, or empty for none
         * @return the builder instance
         */
        public Builder dlqName(String dlqName) {
            this.dlqName = dlqName;
            return this;
        }

        /**
         * Sets seconds a new message stays delayed before becoming available.
         *
         * @param delay seconds a new message stays delayed before becoming available
         * @return the builder instance
         */
        public Builder delay(long delay) {
            this.delay = delay;
            return this;
        }

        /**
         * Sets default priority for the queue's messages, overridable per send-message.
         *
         * @param priority default priority for the queue's messages, overridable per send-message
         * @return the builder instance
         */
        public Builder priority(String priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Builds and returns a new instance of CreateQueueRequest using the properties set on the Builder.
         *
         * @return a new CreateQueueRequest instance.
         */
        public CreateQueueRequest build() {
            return new CreateQueueRequest(name, visibility, maxRetries, maxMessageLength, dlqName, delay, priority);
        }
    }
}
