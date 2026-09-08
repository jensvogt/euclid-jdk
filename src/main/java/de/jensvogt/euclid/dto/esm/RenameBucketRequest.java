package de.jensvogt.euclid.dto.esm;

/**
 * Request to rename a bucket, which also repoints every object and subscription that named it.
 *
 * @param ern     the ERN (Entity Resource Name) of the bucket to rename
 * @param newName the name the bucket is to have, which must not already be taken
 */
public record RenameBucketRequest(String ern, String newName) {

    /**
     * Creates a new instance of the Builder for constructing a RenameBucketRequest object.
     *
     * @return a new Builder instance for constructing RenameBucketRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link RenameBucketRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN (Entity Resource Name) of the bucket to rename.
         */
        private String ern;

        /**
         * The name the bucket is to have.
         */
        private String newName;

        /**
         * Sets the ERN of the bucket to rename.
         *
         * @param ern the ERN of the bucket
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the name the bucket is to have.
         *
         * @param newName the new bucket name
         * @return the builder instance
         */
        public Builder newName(String newName) {
            this.newName = newName;
            return this;
        }

        /**
         * Builds and returns a new instance of RenameBucketRequest using the properties set on the Builder.
         *
         * @return a new RenameBucketRequest instance populated with the ERN and new name values.
         */
        public RenameBucketRequest build() {
            return new RenameBucketRequest(ern, newName);
        }
    }
}
