package de.jensvogt.euclid.dto.esm;

/**
 * Response returned after successfully creating a multipart upload.
 *
 * @param uploadId  the id of the newly created upload
 * @param bucketErn the ARN (Amazon Resource Name) of the bucket the object will be uploaded to
 * @param key       the key of the object being uploaded
 */
public record CreateUploadResponse(String uploadId, String bucketErn, String key) {

    /**
     * Creates a new instance of the Builder for constructing a CreateUploadResponse object.
     *
     * @return a new Builder instance for constructing CreateUploadResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CreateUploadResponse} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The id of the newly created upload.
         */
        private String uploadId;

        /**
         * The ARN (Amazon Resource Name) of the bucket the object will be uploaded to.
         */
        private String bucketErn;

        /**
         * The key of the object being uploaded.
         */
        private String key;

        /**
         * Sets the id of the upload.
         *
         * @param uploadId the id of the newly created upload
         * @return the builder instance
         */
        public Builder uploadId(String uploadId) {
            this.uploadId = uploadId;
            return this;
        }

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
         * Sets the key of the object being uploaded.
         *
         * @param key the key of the object
         * @return the builder instance
         */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /**
         * Builds and returns a new instance of CreateUploadResponse using the properties set on the Builder.
         *
         * @return a new CreateUploadResponse instance populated with the upload id, bucket ARN and key values.
         */
        public CreateUploadResponse build() {
            return new CreateUploadResponse(uploadId, bucketErn, key);
        }
    }
}
