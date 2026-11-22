package de.jensvogt.euclid.dto.eqs;

/**
 * Response returned after a queue has been purged, or a purge accepted.
 * <p>
 * Which of the two it is, {@code async} says. A purge done inline has emptied the queue by the
 * time this arrives, so there is nothing to count; one accepted for the background reports how
 * many messages were waiting when it was accepted, which is the only figure anybody can be given -
 * by the time it finishes the number is zero, and by the time this is read something may already
 * have sent more.
 *
 * @param ern      the ERN (Entity Resource Name) of the queue
 * @param async    whether the purge is still running in the background
 * @param messages the messages available on the queue when a background purge was accepted; zero
 *                 for a purge done inline
 */
public record PurgeQueueResponse(String ern, boolean async, long messages) {

    /**
     * Creates a new instance of the Builder for constructing a PurgeQueueResponse object.
     *
     * @return a new Builder instance for constructing PurgeQueueResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link PurgeQueueResponse} instances.
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
         * Whether the purge is still running in the background.
         */
        private boolean async;

        /**
         * The messages available when a background purge was accepted.
         */
        private long messages;

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
         * Sets whether the purge is still running in the background.
         *
         * @param async whether the purge is still running
         * @return the builder instance
         */
        public Builder async(boolean async) {
            this.async = async;
            return this;
        }

        /**
         * Sets the messages available when a background purge was accepted.
         *
         * @param messages the message count at acceptance
         * @return the builder instance
         */
        public Builder messages(long messages) {
            this.messages = messages;
            return this;
        }

        /**
         * Builds and returns a new instance of PurgeQueueResponse using the properties set on the Builder.
         *
         * @return a new PurgeQueueResponse instance.
         */
        public PurgeQueueResponse build() {
            return new PurgeQueueResponse(ern, async, messages);
        }
    }
}
