package de.jensvogt.euclid.dto.eqs;

/**
 * Request to retrieve the count of messages in a queue.
 *
 * @param ern the ERN (Entity Resource Name) of the queue
 */
public record GetMessageCountRequest(String ern) {

    /**
     * Creates a new instance of the Builder for constructing a GetMessageCountRequest object.
     *
     * @return a new Builder instance for constructing GetMessageCountRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetMessageCountRequest} instances.
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
         * Builds and returns a new instance of GetMessageCountRequest using the properties set on the Builder.
         *
         * @return a new GetMessageCountRequest instance populated with the ERN value.
         */
        public GetMessageCountRequest build() {
            return new GetMessageCountRequest(ern);
        }
    }
}
