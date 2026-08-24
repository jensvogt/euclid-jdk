package de.jensvogt.euclid.dto.esm;

/**
 * Request to delete a bucket.
 *
 * @param ern the ERN (Entity Resource Name) of the bucket to delete
 */
public record DeleteBucketRequest(String ern) {

    /**
     * Creates a new instance of the Builder for constructing a DeleteBucketRequest object.
     *
     * @return a new Builder instance for constructing DeleteBucketRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link DeleteBucketRequest} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN (Entity Resource Name) of the bucket to delete.
         */
        private String ern;

        /**
         * Sets the ERN of the bucket to delete.
         *
         * @param ern the ERN of the bucket
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Builds and returns a new instance of DeleteBucketRequest using the properties set on the Builder.
         *
         * @return a new DeleteBucketRequest instance populated with the ERN value.
         */
        public DeleteBucketRequest build() {
            return new DeleteBucketRequest(ern);
        }
    }
}
