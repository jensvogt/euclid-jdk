package de.jensvogt.euclid.dto.esm;

/**
 * Request for one bucket, by ERN or by name.
 * <p>
 * Exactly one of the two is sent. A name is resolved in the caller's own account and namespace -
 * the pair {@code createBucket} built the ERN from - so it means "my bucket of that name" and
 * cannot reach another account's bucket called the same thing. An ERN names one bucket in the
 * installation.
 *
 * @param ern  the ERN uniquely identifying the bucket, or null when it is named instead
 * @param name the bucket's name, used when no ERN is given
 */
public record GetBucketRequest(String ern, String name) {

    /**
     * Creates a new instance of the Builder for constructing a GetBucketRequest object.
     *
     * @return a new Builder instance for constructing GetBucketRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetBucketRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN uniquely identifying the bucket.
         */
        private String ern;

        /**
         * The bucket's name.
         */
        private String name;

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
         * Sets the name of the bucket.
         *
         * @param name the bucket's name, resolved in the caller's own account and namespace
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Builds and returns a new instance of GetBucketRequest using the properties set on the Builder.
         *
         * @return a new GetBucketRequest instance.
         */
        public GetBucketRequest build() {
            return new GetBucketRequest(ern, name);
        }
    }
}
