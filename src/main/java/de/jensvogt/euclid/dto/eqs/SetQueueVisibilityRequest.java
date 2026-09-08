package de.jensvogt.euclid.dto.eqs;

/**
 * Request to change a queue's default visibility timeout - the window a message is held invisible
 * for after it has been received, unless the receiver overrides it.
 *
 * <p>Only the default changes. Messages already in flight keep the window they were given when
 * they were received, so shortening it here cannot expire a lease a consumer is still working on.
 *
 * @param ern        the ERN (Entity Resource Name) of the queue
 * @param visibility the new default visibility timeout, in seconds, between 0 and 43200
 */
public record SetQueueVisibilityRequest(String ern, long visibility) {

    /**
     * Creates a new instance of the Builder for constructing a SetQueueVisibilityRequest object.
     *
     * @return a new Builder instance for constructing SetQueueVisibilityRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SetQueueVisibilityRequest} instances.
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
         * The new default visibility timeout, in seconds.
         */
        private long visibility;

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
         * Sets the new default visibility timeout, in seconds.
         *
         * @param visibility the visibility timeout in seconds
         * @return the builder instance
         */
        public Builder visibility(long visibility) {
            this.visibility = visibility;
            return this;
        }

        /**
         * Builds and returns a new instance of SetQueueVisibilityRequest using the properties set on the Builder.
         *
         * @return a new SetQueueVisibilityRequest instance populated with the ERN and visibility values.
         */
        public SetQueueVisibilityRequest build() {
            return new SetQueueVisibilityRequest(ern, visibility);
        }
    }
}
