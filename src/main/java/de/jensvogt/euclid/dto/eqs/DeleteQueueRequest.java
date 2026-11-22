package de.jensvogt.euclid.dto.eqs;

/**
 * Request to delete a queue.
 * <p>
 * {@code ifEmpty} is what an automated caller usually means by "delete": remove the queue only if
 * nothing is using it. The server answers that by counting the messages rather than trusting the
 * queue's own counters, and treats events still being delivered into the queue as use too - they
 * are about to become messages, and the delete would discard them. Either refuses the delete with
 * HTTP 409, as does a server that could not establish whether deliveries are pending.
 *
 * @param ern     the ERN (Entity Resource Name) of the queue to delete
 * @param ifEmpty whether to refuse the delete while the queue still holds messages or has deliveries
 *                on their way into it; {@code false} deletes unconditionally
 */
public record DeleteQueueRequest(String ern, boolean ifEmpty) {

    /**
     * Creates a new instance of the Builder for constructing a DeleteQueueRequest object.
     *
     * @return a new Builder instance for constructing DeleteQueueRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link DeleteQueueRequest} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN (Entity Resource Name) of the queue to delete.
         */
        private String ern;

        /**
         * Whether to refuse the delete while the queue is in use. Off by default, which is what
         * delete-queue has always done.
         */
        private boolean ifEmpty = false;

        /**
         * Sets the ERN of the queue to delete.
         *
         * @param ern the ERN of the queue
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets whether the delete is refused while the queue still holds messages or has deliveries
         * on their way into it.
         *
         * @param ifEmpty {@code true} to delete only an unused queue
         * @return the builder instance
         */
        public Builder ifEmpty(boolean ifEmpty) {
            this.ifEmpty = ifEmpty;
            return this;
        }

        /**
         * Builds and returns a new instance of DeleteQueueRequest using the properties set on the Builder.
         *
         * @return a new DeleteQueueRequest instance populated with the ERN and ifEmpty values.
         */
        public DeleteQueueRequest build() {
            return new DeleteQueueRequest(ern, ifEmpty);
        }
    }
}
