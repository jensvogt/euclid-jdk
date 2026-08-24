package de.jensvogt.euclid.dto.esm;

/**
 * Response returned after successfully uploading a part of a multipart upload.
 *
 * @param uploadId   the id of the upload the part belongs to
 * @param partNumber the number of the uploaded part
 * @param size       the size in bytes of the uploaded part
 */
public record UploadPartResponse(String uploadId, long partNumber, long size) {

    /**
     * Creates a new instance of the Builder for constructing a UploadPartResponse object.
     *
     * @return a new Builder instance for constructing UploadPartResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link UploadPartResponse} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The id of the upload the part belongs to.
         */
        private String uploadId;

        /**
         * The number of the uploaded part.
         */
        private long partNumber;

        /**
         * The size in bytes of the uploaded part.
         */
        private long size;

        /**
         * Sets the id of the upload.
         *
         * @param uploadId the id of the upload the part belongs to
         * @return the builder instance
         */
        public Builder uploadId(String uploadId) {
            this.uploadId = uploadId;
            return this;
        }

        /**
         * Sets the number of the uploaded part.
         *
         * @param partNumber the part number
         * @return the builder instance
         */
        public Builder partNumber(long partNumber) {
            this.partNumber = partNumber;
            return this;
        }

        /**
         * Sets the size in bytes of the uploaded part.
         *
         * @param size the size in bytes
         * @return the builder instance
         */
        public Builder size(long size) {
            this.size = size;
            return this;
        }

        /**
         * Builds and returns a new instance of UploadPartResponse using the properties set on the Builder.
         *
         * @return a new UploadPartResponse instance populated with the upload id, part number and size values.
         */
        public UploadPartResponse build() {
            return new UploadPartResponse(uploadId, partNumber, size);
        }
    }
}
