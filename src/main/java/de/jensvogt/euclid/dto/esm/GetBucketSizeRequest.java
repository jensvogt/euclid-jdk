package de.jensvogt.euclid.dto.esm;

/**
 * Request to retrieve the size of a bucket.
 *
 * @param ern the ERN (Entity Resource Name) uniquely identifying the bucket
 */
public record GetBucketSizeRequest(String ern) {

    /**
     * Creates a new instance of the Builder for constructing a GetBucketSizeRequest object.
     *
     * @return a new Builder instance for constructing GetBucketSizeRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetBucketSizeRequest} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN (Entity Resource Name) uniquely identifying the bucket.
         */
        private String ern;

        /**
         * Sets the ERN of the bucket.
         *
         * @param ern the ERN uniquely identifying the bucket
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Builds and returns a new instance of GetBucketSizeRequest using the properties set on the Builder.
         *
         * @return a new GetBucketSizeRequest instance populated with the ERN value.
         */
        public GetBucketSizeRequest build() {
            return new GetBucketSizeRequest(ern);
        }
    }
}
