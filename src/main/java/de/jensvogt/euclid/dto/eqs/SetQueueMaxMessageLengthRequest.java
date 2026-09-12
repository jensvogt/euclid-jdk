package de.jensvogt.euclid.dto.eqs;

/**
 * Request to change the largest message a queue accepts.
 *
 * <p>Applies to what is sent from here on. A message already in the queue was measured against the
 * limit in force when it arrived and is not measured again, so lowering this does not remove
 * anything.
 *
 * @param ern              the ERN (Entity Resource Name) of the queue
 * @param maxMessageLength the largest message the queue accepts, in bytes. Zero is not "accept
 *                         nothing" but "carry no limit of your own": a send is then measured
 *                         against the 1 MiB default, which is what a queue created before the limit
 *                         meant anything is measured against too. A negative value is refused with
 *                         HTTP 400
 */
public record SetQueueMaxMessageLengthRequest(String ern, long maxMessageLength) {

    /**
     * Creates a new instance of the Builder for constructing a SetQueueMaxMessageLengthRequest object.
     *
     * @return a new Builder instance for constructing SetQueueMaxMessageLengthRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SetQueueMaxMessageLengthRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN of the queue.
         */
        private String ern = "";

        /**
         * The largest message the queue accepts, in bytes; zero follows the default.
         */
        private long maxMessageLength;

        /**
         * Sets the ERN of the queue.
         *
         * @param ern the queue ERN
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the largest message the queue accepts.
         *
         * @param maxMessageLength the limit in bytes, or zero to follow the default
         * @return the builder instance
         */
        public Builder maxMessageLength(long maxMessageLength) {
            this.maxMessageLength = maxMessageLength;
            return this;
        }

        /**
         * Builds and returns a new instance of SetQueueMaxMessageLengthRequest using the properties set on the Builder.
         *
         * @return a new SetQueueMaxMessageLengthRequest instance.
         */
        public SetQueueMaxMessageLengthRequest build() {
            return new SetQueueMaxMessageLengthRequest(ern, maxMessageLength);
        }
    }
}
