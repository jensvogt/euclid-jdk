package de.jensvogt.euclid.dto.eqs;

/**
 * Response containing the counts of messages in a queue, broken down by state.
 *
 * @param ern       the ERN (Entity Resource Name) of the queue
 * @param available number of messages currently available for receipt
 * @param delayed   number of messages currently delayed
 * @param invisible number of messages currently invisible (being processed)
 */
public record GetMessageCountResponse(String ern, long available, long delayed, long invisible) {

    /**
     * Calculates the total count of messages by summing up available, delayed, and invisible messages.
     *
     * @return the total number of messages
     */
    public long total() {
        return available + delayed + invisible;
    }

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
        private long available;

        /**
         * Number of messages currently delayed.
         */
        private long delayed;

        /**
         * Number of messages currently invisible (being processed).
         */
        private long invisible;

        /**
         * Sets the ERN of the queue.
         *
         * @param ern the ERN of the queue
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the number of messages currently available for receipt.
         *
         * @param available the number of available messages
         * @return the builder instance
         */
        public Builder available(long available) {
            this.available = available;
            return this;
        }

        /**
         * Sets the number of messages currently delayed.
         *
         * @param delayed the number of delayed messages
         * @return the builder instance
         */
        public Builder delayed(long delayed) {
            this.delayed = delayed;
            return this;
        }

        /**
         * Sets the number of messages currently invisible (being processed).
         *
         * @param invisible the number of invisible messages
         * @return the builder instance
         */
        public Builder invisible(long invisible) {
            this.invisible = invisible;
            return this;
        }

        /**
         * Builds and returns a new instance of GetMessageCountResponse using the properties set on the Builder.
         *
         * @return a new GetMessageCountResponse instance populated with the ERN and message count values.
         */
        public GetMessageCountResponse build() {
            return new GetMessageCountResponse(ern, available, delayed, invisible);
        }
    }
}
