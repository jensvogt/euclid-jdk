package de.jensvogt.euclid.dto.esm;

/**
 * Request to set a tag on a bucket, overwriting any value the key already had.
 *
 * @param ern   the ERN of the bucket to tag
 * @param key   the tag key
 * @param value the tag value
 */
public record SetBucketTagRequest(String ern, String key, String value) {

    /**
     * Creates a new instance of the Builder for constructing a SetBucketTagRequest object.
     *
     * @return a new Builder instance for constructing SetBucketTagRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SetBucketTagRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN of the bucket to tag.
         */
        private String ern;

        /**
         * The tag key.
         */
        private String key;

        /**
         * The tag value.
         */
        private String value;

        /**
         * Sets the ERN of the bucket to tag.
         *
         * @param ern the ERN of the bucket to tag
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the tag key.
         *
         * @param key the tag key
         * @return the builder instance
         */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /**
         * Sets the tag value.
         *
         * @param value the tag value
         * @return the builder instance
         */
        public Builder value(String value) {
            this.value = value;
            return this;
        }

        /**
         * Builds and returns a new instance of SetBucketTagRequest using the properties set on the Builder.
         *
         * @return a new SetBucketTagRequest instance.
         */
        public SetBucketTagRequest build() {
            return new SetBucketTagRequest(ern, key, value);
        }
    }
}
