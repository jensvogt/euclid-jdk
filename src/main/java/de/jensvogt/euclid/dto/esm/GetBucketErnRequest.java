package de.jensvogt.euclid.dto.esm;

/**
 * Request to retrieve the ERN (Entity Resource Name) of a bucket.
 *
 * @param name the name of the bucket
 */
public record GetBucketErnRequest(String name) {

    /**
     * Creates a new instance of the Builder for constructing a GetBucketErnRequest object.
     *
     * @return a new Builder instance for constructing GetBucketErnRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetBucketErnRequest} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The name of the bucket.
         */
        private String name;

        /**
         * Sets the name of the bucket.
         *
         * @param name the name of the bucket
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Builds and returns a new instance of GetBucketErnRequest using the properties set on the Builder.
         *
         * @return a new GetBucketErnRequest instance populated with the name value.
         */
        public GetBucketErnRequest build() {
            return new GetBucketErnRequest(name);
        }
    }
}
