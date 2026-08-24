package de.jensvogt.euclid.dto.eqs;

/**
 * Represents a request to create a new queue with specific attributes.
 *
 * The queue configuration includes the name of the queue, visibility timeout,
 * maximum number of retries, maximum message length, associated dead-letter queue name,
 * and a message delivery delay.
 *
 * The {@code CreateQueueRequest} class is implemented as a record, providing an immutable
 * data structure and a builder for constructing instances.
 *
 * Fields:
 * - {@code name}: The name of the queue.
 * - {@code visibility}: The visibility timeout for messages in the queue, defined in seconds.
 * - {@code maxRetries}: The maximum number of retries for processing a message.
 * - {@code maxMessageLength}: The maximum allowed size of a message in bytes.
 * - {@code dlqName}: The name of the dead-letter queue to which messages are sent after exceeding retries.
 * - {@code delay}: The delay in seconds for delivering messages to the queue.
 *
 * Methods:
 * - {@code builder()}: Returns a builder instance for creating {@code CreateQueueRequest} objects.
 *
 * Use the provided builder for creating customized instances of {@code CreateQueueRequest}.
 *
 * @param name The name of the queue.
 * @param visibility The visibility timeout for messages in the queue, defined in seconds.
 * @param maxRetries The maximum number of retries for processing a message.
 * @param maxMessageLength The maximum allowed size of a message in bytes.
 * @param dlqName The name of the dead-letter queue to which messages are sent after exceeding retries.
 * @param delay The delay in seconds for delivering messages to the queue.
 */
public record CreateQueueRequest(String name, long visibility, long maxRetries, long maxMessageLength,
                                  String dlqName, long delay) {

    /**
     * Creates a new instance of the {@code Builder} for constructing a {@code CreateQueueRequest}.
     *
     * @return a new {@code Builder} instance for building a {@code CreateQueueRequest}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder class for constructing instances of {@code CreateQueueRequest}.
     * Provides a convenient and flexible way to configure the parameters of the
     * {@code CreateQueueRequest} object before creating it.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The name of the queue to be created.
         * Used to uniquely identify the queue within the system.
         */
        private String name;

        /**
         * Defines the default visibility timeout for a queue in seconds.
         * This determines the time duration for which a message remains hidden
         * from other consumers after being retrieved by a consumer.
         */
        private long visibility = 30;

        /**
         * Specifies the maximum number of retry attempts that will be made
         * if an operation fails. This value is intended to control retry
         * logic and define a limit on how many times an operation can be
         * retried before concluding it as a failure.
         */
        private long maxRetries = 3;

        /**
         * Specifies the maximum allowable size, in bytes, for a message in the queue.
         * This value is used to enforce a limit on the size of messages that can be
         * sent or processed within the system.
         */
        private long maxMessageLength = 1024 * 1024;

        /**
         * Specifies the name of the dead-letter queue (DLQ) associated with the main queue.
         * A dead-letter queue is used to store messages that cannot be successfully processed
         * or delivered after a certain number of retry attempts or due to other processing issues.
         */
        private String dlqName = "";

        /**
         * Specifies the delay duration, in milliseconds, to be applied before processing a message.
         * This delay determines the amount of time a message remains in a pending state
         * before it becomes available for delivery or consumption.
         *
         * A value of {@code 0} indicates that no delay will be applied, and the message
         * will be immediately eligible for processing.
         */
        private long delay = 0;

        /**
         * Sets the name for the builder.
         *
         * @param name the name to set
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the visibility parameter for the builder.
         *
         * @param visibility the visibility value to set
         * @return the builder instance
         */
        public Builder visibility(long visibility) {
            this.visibility = visibility;
            return this;
        }

        /**
         * Sets the maximum number of retries for the builder.
         *
         * @param maxRetries the maximum number of retries to set
         * @return the builder instance
         */
        public Builder maxRetries(long maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        /**
         * Sets the maximum allowable message length for the builder.
         *
         * @param maxMessageLength the maximum length of a message in bytes
         * @return the builder instance
         */
        public Builder maxMessageLength(long maxMessageLength) {
            this.maxMessageLength = maxMessageLength;
            return this;
        }

        /**
         * Sets the dead-letter queue (DLQ) name for the builder.
         *
         * @param dlqName the name of the dead-letter queue to set
         * @return the builder instance
         */
        public Builder dlqName(String dlqName) {
            this.dlqName = dlqName;
            return this;
        }

        /**
         * Sets the delay parameter for the builder.
         *
         * @param delay the delay value to set, in milliseconds
         * @return the builder instance
         */
        public Builder delay(long delay) {
            this.delay = delay;
            return this;
        }

        /**
         * Builds and returns a {@code CreateQueueRequest} using the parameters
         * that have been set on the builder instance.
         *
         * @return a new {@code CreateQueueRequest} instance initialized with the
         *         configured properties such as name, visibility, maxRetries,
         *         maxMessageLength, dlqName, and delay.
         */
        public CreateQueueRequest build() {
            return new CreateQueueRequest(name, visibility, maxRetries, maxMessageLength, dlqName, delay);
        }
    }
}
