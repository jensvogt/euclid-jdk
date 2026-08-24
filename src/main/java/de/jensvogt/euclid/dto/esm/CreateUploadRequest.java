package de.jensvogt.euclid.dto.esm;

/**
 * Request to create a multipart upload for an object.
 *
 * @param bucketErn the ARN (Amazon Resource Name) of the bucket the object will be uploaded to
 * @param key       the key of the object to be uploaded
 */
public record CreateUploadRequest(String bucketErn, String key) {

    /**
     * Creates a new instance of the Builder for constructing a CreateUploadRequest object.
     *
     * @return a new Builder instance for constructing CreateUploadRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CreateUploadRequest} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ARN (Amazon Resource Name) of the bucket the object will be uploaded to.
         */
        private String bucketErn;

        /**
         * The key of the object to be uploaded.
         */
        private String key;

        /**
         * Sets the ARN (Amazon Resource Name) of the bucket.
         *
         * @param bucketErn the ARN of the bucket the object will be uploaded to
         * @return the builder instance
         */
        public Builder bucketErn(String bucketErn) {
            this.bucketErn = bucketErn;
            return this;
        }

        /**
         * Sets the key of the object to be uploaded.
         *
         * @param key the key of the object
         * @return the builder instance
         */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /**
         * Builds and returns a new instance of CreateUploadRequest using the properties set on the Builder.
         *
         * @return a new CreateUploadRequest instance populated with the bucket ARN and key values.
         */
        public CreateUploadRequest build() {
            return new CreateUploadRequest(bucketErn, key);
        }
    }
}
