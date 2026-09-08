package de.jensvogt.euclid.dto.eqs;

/**
 * Response returned after a queue has been stopped or started.
 *
 * @param ern       the ERN (Entity Resource Name) of the queue
 * @param status    the queue's status afterwards, {@code "STOPPED"} or {@code "AVAILABLE"}
 * @param available the number of messages waiting on the queue, which stopping does not change -
 *                  messages already in flight keep their lease and may still be deleted
 */
public record QueueStatusResponse(String ern, String status, long available) {

    /**
     * Creates a new instance of the Builder for constructing a QueueStatusResponse object.
     *
     * @return a new Builder instance for constructing QueueStatusResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link QueueStatusResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN (Entity Resource Name) of the queue.
         */
        private String ern;

        /**
         * The queue's status afterwards, {@code "STOPPED"} or {@code "AVAILABLE"}.
         */
        private String status;

        /**
         * The number of messages waiting on the queue.
         */
        private long available;

        /**
         * Sets the ERN (Entity Resource Name) of the queue.
         *
         * @param ern the ERN of the queue
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the queue's status afterwards.
         *
         * @param status {@code "STOPPED"} or {@code "AVAILABLE"}
         * @return the builder instance
         */
        public Builder status(String status) {
            this.status = status;
            return this;
        }

        /**
         * Sets the number of messages waiting on the queue.
         *
         * @param available the number of messages waiting
         * @return the builder instance
         */
        public Builder available(long available) {
            this.available = available;
            return this;
        }

        /**
         * Builds and returns a new instance of QueueStatusResponse using the properties set on the Builder.
         *
         * @return a new QueueStatusResponse instance populated with the ERN, status and available values.
         */
        public QueueStatusResponse build() {
            return new QueueStatusResponse(ern, status, available);
        }
    }
}
