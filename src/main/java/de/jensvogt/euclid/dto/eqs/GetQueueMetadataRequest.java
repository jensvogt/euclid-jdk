package de.jensvogt.euclid.dto.eqs;

/**
 * Request to retrieve the metadata of a queue.
 *
 * @param ern the ERN (Entity Resource Name) of the queue
 */
public record GetQueueMetadataRequest(String ern) {

    /**
     * Creates a new instance of the Builder for constructing a GetQueueMetadataRequest object.
     *
     * @return a new Builder instance for constructing GetQueueMetadataRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetQueueMetadataRequest} instances.
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
         * Builds and returns a new instance of GetQueueMetadataRequest using the properties set on the Builder.
         *
         * @return a new GetQueueMetadataRequest instance populated with the ERN value.
         */
        public GetQueueMetadataRequest build() {
            return new GetQueueMetadataRequest(ern);
        }
    }
}
