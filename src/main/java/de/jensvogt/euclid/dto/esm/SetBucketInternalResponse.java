package de.jensvogt.euclid.dto.esm;

/**
 * Response returned after a bucket's internal flag has been set or cleared.
 *
 * @param ern      the ERN (Entity Resource Name) of the bucket
 * @param name     the name of the bucket
 * @param internal the flag the bucket now carries
 */
public record SetBucketInternalResponse(String ern, String name, boolean internal) {

    /**
     * Creates a new instance of the Builder for constructing a SetBucketInternalResponse object.
     *
     * @return a new Builder instance for constructing SetBucketInternalResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SetBucketInternalResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN (Entity Resource Name) of the bucket.
         */
        private String ern;

        /**
         * The name of the bucket.
         */
        private String name;

        /**
         * The flag the bucket now carries.
         */
        private boolean internal;

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
         * Sets the name of the bucket.
         *
         * @param name the bucket name
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the flag the bucket now carries.
         *
         * @param internal whether the bucket is euclid's own
         * @return the builder instance
         */
        public Builder internal(boolean internal) {
            this.internal = internal;
            return this;
        }

        /**
         * Builds and returns a new instance of SetBucketInternalResponse using the properties set on the Builder.
         *
         * @return a new SetBucketInternalResponse instance.
         */
        public SetBucketInternalResponse build() {
            return new SetBucketInternalResponse(ern, name, internal);
        }
    }
}
