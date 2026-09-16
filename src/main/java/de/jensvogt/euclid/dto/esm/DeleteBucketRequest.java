package de.jensvogt.euclid.dto.esm;

/**
 * Request to delete a bucket.
 *
 * @param ern   the ERN (Entity Resource Name) of the bucket to delete
 * @param async whether the server answers as soon as it has taken the work on rather than when it
 *              has finished, which is what a bucket too large to empty within one request wants
 */
public record DeleteBucketRequest(String ern, boolean async) {

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
         * Whether the server answers as soon as it has taken the work on.
         */
        private boolean async;

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
         * Sets whether the server answers before the bucket and its objects are gone.
         *
         * @param async whether to delete in the background
         * @return the builder instance
         */
        public Builder async(boolean async) {
            this.async = async;
            return this;
        }

        /**
         * Builds and returns a new instance of DeleteBucketRequest using the properties set on the Builder.
         *
         * @return a new DeleteBucketRequest instance populated with the ERN value.
         */
        public DeleteBucketRequest build() {
            return new DeleteBucketRequest(ern, async);
        }
    }
}
