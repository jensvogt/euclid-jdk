package de.jensvogt.euclid.dto.esm;

import de.jensvogt.euclid.dto.esm.model.Bucket;

/**
 * One bucket, as a listing describes each of its own.
 * <p>
 * The same {@link Bucket} a {@code listBuckets} answer carries, rather than a shape of its own, so
 * that what a listing shows and what this shows cannot drift apart.
 *
 * @param bucket the bucket
 */
public record GetBucketResponse(Bucket bucket) {

    /**
     * Creates a new instance of the Builder for constructing a GetBucketResponse object.
     *
     * @return a new Builder instance for constructing GetBucketResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetBucketResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The bucket.
         */
        private Bucket bucket;

        /**
         * Sets the bucket.
         *
         * @param bucket the bucket
         * @return the builder instance
         */
        public Builder bucket(Bucket bucket) {
            this.bucket = bucket;
            return this;
        }

        /**
         * Builds and returns a new instance of GetBucketResponse using the properties set on the Builder.
         *
         * @return a new GetBucketResponse instance.
         */
        public GetBucketResponse build() {
            return new GetBucketResponse(bucket);
        }
    }
}
