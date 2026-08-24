package de.jensvogt.euclid.dto.esm;

/**
 * Response containing the ERN (Entity Resource Name) of a bucket.
 *
 * @param ern the ERN of the bucket
 */
public record GetBucketErnResponse(String ern) {

    /**
     * Creates a new instance of the Builder for constructing a GetBucketErnResponse object.
     *
     * @return a new Builder instance for constructing GetBucketErnResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetBucketErnResponse} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN of the bucket.
         */
        private String ern;

        /**
         * Sets the ERN of the bucket.
         *
         * @param ern the ERN of the bucket
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Builds and returns a new instance of GetBucketErnResponse using the properties set on the Builder.
         *
         * @return a new GetBucketErnResponse instance populated with the ERN value.
         */
        public GetBucketErnResponse build() {
            return new GetBucketErnResponse(ern);
        }
    }
}
