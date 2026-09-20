package de.jensvogt.euclid.dto.esm;

/**
 * Request to throw away a multipart upload that will not be finished.
 *
 * @param uploadId the upload to discard, as create-upload returned it
 */
public record AbortUploadRequest(String uploadId) {

    /**
     * Creates a new instance of the Builder for constructing an AbortUploadRequest object.
     *
     * @return a new Builder instance for constructing AbortUploadRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link AbortUploadRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The upload to discard.
         */
        private String uploadId;

        /**
         * Sets the upload to discard.
         *
         * @param uploadId the upload's id, as create-upload returned it
         * @return the builder instance
         */
        public Builder uploadId(String uploadId) {
            this.uploadId = uploadId;
            return this;
        }

        /**
         * Builds and returns a new instance of AbortUploadRequest using the properties set on the Builder.
         *
         * @return a new AbortUploadRequest instance.
         */
        public AbortUploadRequest build() {
            return new AbortUploadRequest(uploadId);
        }
    }
}
