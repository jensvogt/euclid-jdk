package de.jensvogt.euclid.dto.esm;

/**
 * Request to purge (delete all objects from) a bucket.
 *
 * @param ern the ERN (Entity Resource Name) of the bucket to purge
 */
public record PurgeBucketRequest(String ern) {

    /**
     * Creates a new instance of the Builder for constructing a PurgeBucketRequest object.
     *
     * @return a new Builder instance for constructing PurgeBucketRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link PurgeBucketRequest} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN (Entity Resource Name) of the bucket to purge.
         */
        private String ern;

        /**
         * Sets the ERN of the bucket to purge.
         *
         * @param ern the ERN of the bucket
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Builds and returns a new instance of PurgeBucketRequest using the properties set on the Builder.
         *
         * @return a new PurgeBucketRequest instance populated with the ERN value.
         */
        public PurgeBucketRequest build() {
            return new PurgeBucketRequest(ern);
        }
    }
}
