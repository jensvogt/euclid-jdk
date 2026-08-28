package de.jensvogt.euclid.dto.ens;

/**
 * Response containing a topic's message counters.
 *
 * @param ern       topic ERN
 * @param available total number of messages available
 * @param send      total number of messages sent
 * @param resend    total number of messages resent
 */
public record GetMessageCountResponse(String ern, long available, long send, long resend) {

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
         * The topic ERN.
         */
        private String ern;

        /**
         * Total number of messages available.
         */
        private long available;

        /**
         * Total number of messages sent.
         */
        private long send;

        /**
         * Total number of messages resent.
         */
        private long resend;

        /**
         * Sets the topic ERN.
         *
         * @param ern the topic ERN
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the total number of messages available.
         *
         * @param available the number available
         * @return the builder instance
         */
        public Builder available(long available) {
            this.available = available;
            return this;
        }

        /**
         * Sets the total number of messages sent.
         *
         * @param send the number sent
         * @return the builder instance
         */
        public Builder send(long send) {
            this.send = send;
            return this;
        }

        /**
         * Sets the total number of messages resent.
         *
         * @param resend the number resent
         * @return the builder instance
         */
        public Builder resend(long resend) {
            this.resend = resend;
            return this;
        }

        /**
         * Builds and returns a new instance of GetMessageCountResponse using the properties set on the Builder.
         *
         * @return a new GetMessageCountResponse instance.
         */
        public GetMessageCountResponse build() {
            return new GetMessageCountResponse(ern, available, send, resend);
        }
    }
}
