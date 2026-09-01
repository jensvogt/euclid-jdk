package de.jensvogt.euclid.dto.esm;

/**
 * Request to list every subscription on a bucket.
 *
 * @param bucketErn the ERN of the bucket whose subscriptions are listed
 */
public record ListSubscriptionsRequest(String bucketErn) {

    /**
     * Creates a new instance of the Builder for constructing a ListSubscriptionsRequest object.
     *
     * @return a new Builder instance for constructing ListSubscriptionsRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListSubscriptionsRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN of the bucket whose subscriptions are listed.
         */
        private String bucketErn;

        /**
         * Sets the ERN of the bucket whose subscriptions are listed.
         *
         * @param bucketErn the ERN of the bucket whose subscriptions are listed
         * @return the builder instance
         */
        public Builder bucketErn(String bucketErn) {
            this.bucketErn = bucketErn;
            return this;
        }

        /**
         * Builds and returns a new instance of ListSubscriptionsRequest using the properties set on the Builder.
         *
         * @return a new ListSubscriptionsRequest instance.
         */
        public ListSubscriptionsRequest build() {
            return new ListSubscriptionsRequest(bucketErn);
        }
    }
}
