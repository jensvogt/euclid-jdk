package de.jensvogt.euclid.dto.esm;

/**
 * Request to remove a subscription, identified by the ERN a subscribe action returned.
 *
 * @param ern the ERN of the subscription to remove
 */
public record UnsubscribeRequest(String ern) {

    /**
     * Creates a new instance of the Builder for constructing an UnsubscribeRequest object.
     *
     * @return a new Builder instance for constructing UnsubscribeRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link UnsubscribeRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN of the subscription to remove.
         */
        private String ern;

        /**
         * Sets the ERN of the subscription to remove.
         *
         * @param ern the ERN of the subscription to remove
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Builds and returns a new instance of UnsubscribeRequest using the properties set on the Builder.
         *
         * @return a new UnsubscribeRequest instance.
         */
        public UnsubscribeRequest build() {
            return new UnsubscribeRequest(ern);
        }
    }
}
