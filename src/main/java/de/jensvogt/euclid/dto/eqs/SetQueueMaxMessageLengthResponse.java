package de.jensvogt.euclid.dto.eqs;

/**
 * Response returned after a queue's message size limit has been changed.
 *
 * <p>Two numbers rather than one, because zero means something: {@link #maxMessageLength()} is what
 * the queue now carries, and {@link #effectiveMaxMessageLength()} is what a send is actually
 * measured against. They differ only when the queue carries no limit of its own, in which case the
 * second is the 1 MiB default - which is the answer a caller wants and cannot work out from the
 * first alone.
 *
 * @param ern                       the ERN (Entity Resource Name) of the queue
 * @param maxMessageLength          the limit the queue now carries, in bytes; zero means it carries
 *                                  none of its own
 * @param effectiveMaxMessageLength the limit a send is measured against, in bytes - the queue's own
 *                                  where it has one, the default otherwise
 */
public record SetQueueMaxMessageLengthResponse(String ern, long maxMessageLength, long effectiveMaxMessageLength) {

    /**
     * Creates a new instance of the Builder for constructing a SetQueueMaxMessageLengthResponse object.
     *
     * @return a new Builder instance for constructing SetQueueMaxMessageLengthResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SetQueueMaxMessageLengthResponse} instances.
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
        private String ern;

        /**
         * The limit the queue now carries, in bytes.
         */
        private long maxMessageLength;

        /**
         * The limit a send is measured against, in bytes.
         */
        private long effectiveMaxMessageLength;

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
         * Sets the limit the queue now carries.
         *
         * @param maxMessageLength the limit in bytes
         * @return the builder instance
         */
        public Builder maxMessageLength(long maxMessageLength) {
            this.maxMessageLength = maxMessageLength;
            return this;
        }

        /**
         * Sets the limit a send is measured against.
         *
         * @param effectiveMaxMessageLength the effective limit in bytes
         * @return the builder instance
         */
        public Builder effectiveMaxMessageLength(long effectiveMaxMessageLength) {
            this.effectiveMaxMessageLength = effectiveMaxMessageLength;
            return this;
        }

        /**
         * Builds and returns a new instance of SetQueueMaxMessageLengthResponse using the properties set on the Builder.
         *
         * @return a new SetQueueMaxMessageLengthResponse instance.
         */
        public SetQueueMaxMessageLengthResponse build() {
            return new SetQueueMaxMessageLengthResponse(ern, maxMessageLength, effectiveMaxMessageLength);
        }
    }
}
