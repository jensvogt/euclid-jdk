package de.jensvogt.euclid.dto.esm;

/**
 * Response containing the size of a bucket.
 *
 * @param ern  the ERN (Entity Resource Name) uniquely identifying the bucket
 * @param size the size of the bucket in bytes
 */
public record GetBucketSizeResponse(String ern, long size) {

    /**
     * Creates and returns a new instance of the Builder class,
     * which can be used to construct instances of the enclosing record.
     *
     * @return a new Builder instance for creating GetBucketSizeResponse objects
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetBucketSizeResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Represents an identifier for a resource in the system, referred to as "ERN" (Entity Resource Name).
         *
         * This field is used to uniquely specify and reference an entity or resource,
         * typically in operations that require precise identification of resources.
         */
        private String ern;

        /**
         * Specifies the size of the bucket or object in bytes.
         *
         * This field is used to store and retrieve the size information
         * associated with a specific bucket or object during operations
         * that deal with resource size calculations or validations.
         */
        private long size;

        /**
         * Sets the Entity Resource Name (ERN) for the builder.
         *
         * @param ern the unique identifier for a resource in the system
         * @return the updated builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the size of the bucket or object in bytes.
         *
         * @param size the size in bytes to be assigned to the bucket or object
         * @return the updated builder instance
         */
        public Builder size(long size) {
            this.size = size;
            return this;
        }

        /**
         * Builds and returns a new instance of the GetBucketSizeResponse record
         * with the specified Entity Resource Name (ERN) and size.
         *
         * @return a new GetBucketSizeResponse instance containing the ERN and size
         */
        public GetBucketSizeResponse build() {
            return new GetBucketSizeResponse(ern, size);
        }
    }
}
