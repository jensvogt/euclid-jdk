package de.jensvogt.euclid.dto.ens;

/**
 * Request to retrieve a topic's message counters.
 *
 * @param ern topic ERN
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
         * The topic ERN.
         */
        private String ern;

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
         * Builds and returns a new instance of GetMessageCountRequest using the properties set on the Builder.
         *
         * @return a new GetMessageCountRequest instance.
         */
        public GetMessageCountRequest build() {
            return new GetMessageCountRequest(ern);
        }
    }
}
