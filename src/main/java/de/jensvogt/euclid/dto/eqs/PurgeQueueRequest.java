package de.jensvogt.euclid.dto.eqs;

/**
 * Request to purge (delete all messages from) a queue.
 * <p>
 * {@code async} exists because a queue with a large backlog takes longer to empty than the gateway
 * will wait: done inline, the caller is handed a timeout while the purge carries on regardless.
 * Asked for in the background, the server answers HTTP 202 straight away with the number of
 * messages there were when it accepted the purge - the only count it can honestly give, since by
 * the time the purge finishes it is zero.
 *
 * @param ern   the ERN (Entity Resource Name) of the queue to purge
 * @param async whether the server answers before the purge has finished
 */
public record PurgeQueueRequest(String ern, boolean async) {

    /**
     * Creates a new instance of the Builder for constructing a PurgeQueueRequest object.
     *
     * @return a new Builder instance for constructing PurgeQueueRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link PurgeQueueRequest} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN (Entity Resource Name) of the queue to purge.
         */
        private String ern;

        /**
         * Whether the server answers before the purge has finished. Off by default, which is what
         * purge-queue has always done.
         */
        private boolean async = false;

        /**
         * Sets the ERN of the queue to purge.
         *
         * @param ern the ERN of the queue
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets whether the server answers before the purge has finished.
         *
         * @param async {@code true} to purge in the background
         * @return the builder instance
         */
        public Builder async(boolean async) {
            this.async = async;
            return this;
        }

        /**
         * Builds and returns a new instance of PurgeQueueRequest using the properties set on the Builder.
         *
         * @return a new PurgeQueueRequest instance populated with the ERN and async values.
         */
        public PurgeQueueRequest build() {
            return new PurgeQueueRequest(ern, async);
        }
    }
}
