package de.jensvogt.euclid.dto.esm;

/**
 * Response returned after successfully purging a bucket.
 *
 * @param ern   the ERN (Entity Resource Name) of the purged bucket
 * @param count the number of objects deleted from the bucket
 */
public record PurgeBucketResponse(String ern, long count) {

    /**
     * Creates a new instance of the Builder for constructing a PurgeBucketResponse object.
     *
     * @return a new Builder instance for constructing PurgeBucketResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link PurgeBucketResponse} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN (Entity Resource Name) of the purged bucket.
         */
        private String ern;

        /**
         * The number of objects deleted from the bucket.
         */
        private long count;

        /**
         * Sets the ERN of the purged bucket.
         *
         * @param ern the ERN of the bucket
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the number of objects deleted from the bucket.
         *
         * @param count the number of deleted objects
         * @return the builder instance
         */
        public Builder count(long count) {
            this.count = count;
            return this;
        }

        /**
         * Builds and returns a new instance of PurgeBucketResponse using the properties set on the Builder.
         *
         * @return a new PurgeBucketResponse instance populated with the ERN and count values.
         */
        public PurgeBucketResponse build() {
            return new PurgeBucketResponse(ern, count);
        }
    }
}
