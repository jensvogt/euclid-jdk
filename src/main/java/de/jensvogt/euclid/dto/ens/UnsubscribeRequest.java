package de.jensvogt.euclid.dto.ens;

/**
 * Request to delete a subscription.
 *
 * @param ern ERN of the subscription to delete
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
         * The ERN of the subscription to delete.
         */
        private String ern;

        /**
         * Sets the ERN of the subscription to delete.
         *
         * @param ern the subscription ERN
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
