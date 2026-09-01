package de.jensvogt.euclid.dto.esm;

/**
 * Request to copy or move an object between buckets, or within one.
 * <p>
 * Shared by copy-object and move-object: the two differ only in whether the source survives,
 * which the action name decides rather than a field here.
 *
 * @param sourceBucketErn ERN of the bucket the object is read from
 * @param sourceKey       key of the object to copy or move
 * @param targetBucketErn ERN of the bucket the object is written to
 * @param targetKey       key the object is written under
 */
public record CopyObjectRequest(String sourceBucketErn, String sourceKey, String targetBucketErn, String targetKey) {

    /**
     * Creates a new instance of the Builder for constructing a CopyObjectRequest object.
     *
     * @return a new Builder instance for constructing CopyObjectRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CopyObjectRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * ERN of the bucket the object is read from.
         */
        private String sourceBucketErn;

        /**
         * Key of the object to copy or move.
         */
        private String sourceKey;

        /**
         * ERN of the bucket the object is written to.
         */
        private String targetBucketErn;

        /**
         * Key the object is written under.
         */
        private String targetKey;

        /**
         * Sets ERN of the bucket the object is read from.
         *
         * @param sourceBucketErn ERN of the bucket the object is read from
         * @return the builder instance
         */
        public Builder sourceBucketErn(String sourceBucketErn) {
            this.sourceBucketErn = sourceBucketErn;
            return this;
        }

        /**
         * Sets key of the object to copy or move.
         *
         * @param sourceKey key of the object to copy or move
         * @return the builder instance
         */
        public Builder sourceKey(String sourceKey) {
            this.sourceKey = sourceKey;
            return this;
        }

        /**
         * Sets ERN of the bucket the object is written to.
         *
         * @param targetBucketErn ERN of the bucket the object is written to
         * @return the builder instance
         */
        public Builder targetBucketErn(String targetBucketErn) {
            this.targetBucketErn = targetBucketErn;
            return this;
        }

        /**
         * Sets key the object is written under.
         *
         * @param targetKey key the object is written under
         * @return the builder instance
         */
        public Builder targetKey(String targetKey) {
            this.targetKey = targetKey;
            return this;
        }

        /**
         * Builds and returns a new instance of CopyObjectRequest using the properties set on the Builder.
         *
         * @return a new CopyObjectRequest instance.
         */
        public CopyObjectRequest build() {
            return new CopyObjectRequest(sourceBucketErn, sourceKey, targetBucketErn, targetKey);
        }
    }
}
