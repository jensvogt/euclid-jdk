package de.jensvogt.euclid.dto.eqs;

/**
 * Response returned after a queue's default visibility timeout has been changed.
 *
 * @param ern        the ERN (Entity Resource Name) of the queue
 * @param visibility the default visibility timeout the queue now has, in seconds
 */
public record SetQueueVisibilityResponse(String ern, long visibility) {

    /**
     * Creates a new instance of the Builder for constructing a SetQueueVisibilityResponse object.
     *
     * @return a new Builder instance for constructing SetQueueVisibilityResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SetQueueVisibilityResponse} instances.
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
         * The default visibility timeout the queue now has, in seconds.
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
         * Sets the default visibility timeout the queue now has, in seconds.
         *
         * @param visibility the visibility timeout in seconds
         * @return the builder instance
         */
        public Builder visibility(long visibility) {
            this.visibility = visibility;
            return this;
        }

        /**
         * Builds and returns a new instance of SetQueueVisibilityResponse using the properties set on the Builder.
         *
         * @return a new SetQueueVisibilityResponse instance populated with the ERN and visibility values.
         */
        public SetQueueVisibilityResponse build() {
            return new SetQueueVisibilityResponse(ern, visibility);
        }
    }
}
