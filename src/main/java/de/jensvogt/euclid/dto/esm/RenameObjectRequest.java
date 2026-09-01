package de.jensvogt.euclid.dto.esm;

/**
 * Request to rename an object within its bucket - a move that cannot leave the bucket, which is
 * the whole difference between the two.
 *
 * @param bucketErn ERN of the bucket holding the object
 * @param key       the object's current key
 * @param newKey    the key the object is renamed to
 */
public record RenameObjectRequest(String bucketErn, String key, String newKey) {

    /**
     * Creates a new instance of the Builder for constructing a RenameObjectRequest object.
     *
     * @return a new Builder instance for constructing RenameObjectRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link RenameObjectRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * ERN of the bucket holding the object.
         */
        private String bucketErn;

        /**
         * The object's current key.
         */
        private String key;

        /**
         * The key the object is renamed to.
         */
        private String newKey;

        /**
         * Sets ERN of the bucket holding the object.
         *
         * @param bucketErn ERN of the bucket holding the object
         * @return the builder instance
         */
        public Builder bucketErn(String bucketErn) {
            this.bucketErn = bucketErn;
            return this;
        }

        /**
         * Sets the object's current key.
         *
         * @param key the object's current key
         * @return the builder instance
         */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /**
         * Sets the key the object is renamed to.
         *
         * @param newKey the key the object is renamed to
         * @return the builder instance
         */
        public Builder newKey(String newKey) {
            this.newKey = newKey;
            return this;
        }

        /**
         * Builds and returns a new instance of RenameObjectRequest using the properties set on the Builder.
         *
         * @return a new RenameObjectRequest instance.
         */
        public RenameObjectRequest build() {
            return new RenameObjectRequest(bucketErn, key, newKey);
        }
    }
}
