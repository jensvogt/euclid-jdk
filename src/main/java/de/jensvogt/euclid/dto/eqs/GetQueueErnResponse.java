package de.jensvogt.euclid.dto.eqs;

/**
 * Response containing the ERN (Entity Resource Name) of a queue.
 *
 * @param ern the ERN of the queue
 */
public record GetQueueErnResponse(String ern) {

    /**
     * Creates a new instance of the Builder for constructing a GetQueueErnResponse object.
     *
     * @return a new Builder instance for constructing GetQueueErnResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetQueueErnResponse} instances.
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
         * Builds and returns a new instance of GetQueueErnResponse using the properties set on the Builder.
         *
         * @return a new GetQueueErnResponse instance populated with the ERN value.
         */
        public GetQueueErnResponse build() {
            return new GetQueueErnResponse(ern);
        }
    }
}
