package de.jensvogt.euclid.dto.eqs;

/**
 * Response containing the counts of messages in a queue, broken down by state.
 *
 * @param ern       the ERN (Entity Resource Name) of the queue
 * @param available number of messages currently available for receipt
 * @param delayed   number of messages currently delayed
 * @param invisible number of messages currently invisible (being processed)
 * @param total     total number of messages, the sum of the three counts above
 */
public record GetMessageCountResponse(String ern, long available, long delayed, long invisible, long total) {

    /**
     * Creates a new instance of the Builder for constructing a GetMessageCountResponse object.
     *
     * @return a new Builder instance for constructing GetMessageCountResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetMessageCountResponse} instances.
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
         * Number of messages currently available for receipt.
         */
        private long available = 0;

        /**
         * Number of messages currently delayed.
         */
        private long delayed = 0;

        /**
         * Number of messages currently invisible (being processed).
         */
        private long invisible = 0;

        /**
         * Total number of messages, the sum of the three counts above.
         */
        private long total = 0;

        /**
         * Sets the ERN (Entity Resource Name) of the queue.
         *
         * @param ern the ERN (Entity Resource Name) of the queue
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets number of messages currently available for receipt.
         *
         * @param available number of messages currently available for receipt
         * @return the builder instance
         */
        public Builder available(long available) {
            this.available = available;
            return this;
        }

        /**
         * Sets number of messages currently delayed.
         *
         * @param delayed number of messages currently delayed
         * @return the builder instance
         */
        public Builder delayed(long delayed) {
            this.delayed = delayed;
            return this;
        }

        /**
         * Sets number of messages currently invisible (being processed).
         *
         * @param invisible number of messages currently invisible (being processed)
         * @return the builder instance
         */
        public Builder invisible(long invisible) {
            this.invisible = invisible;
            return this;
        }

        /**
         * Sets total number of messages, the sum of the three counts above.
         *
         * @param total total number of messages, the sum of the three counts above
         * @return the builder instance
         */
        public Builder total(long total) {
            this.total = total;
            return this;
        }

        /**
         * Builds and returns a new instance of GetMessageCountResponse using the properties set on the Builder.
         *
         * @return a new GetMessageCountResponse instance.
         */
        public GetMessageCountResponse build() {
            return new GetMessageCountResponse(ern, available, delayed, invisible, total);
        }
    }
}
