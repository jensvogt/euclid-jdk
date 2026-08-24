package de.jensvogt.euclid.dto.esm;

/**
 * Represents the response for completing an upload operation.
 * This record contains details about the completed upload, such as its identifiers,
 * metadata, and status.
 *
 * @param ern         The extended resource name of the upload.
 * @param bucketErn   The extended resource name of the bucket where the upload resides.
 * @param key         The storage key that uniquely identifies the uploaded object.
 * @param size        The size of the uploaded object in bytes.
 * @param status      The status of the upload operation (e.g., success or failure).
 * @param contentType The MIME type of the uploaded object.
 * @param md5Sum      The MD5 checksum of the uploaded object for integrity verification.
 */
public record CompleteUploadResponse(String ern, String bucketErn, String key, long size, String status,
                                      String contentType, String md5Sum) {

    /**
     * Creates a new instance of {@code Builder} for constructing {@code CompleteUploadResponse} objects.
     * The builder provides a fluent API for setting various properties before creating an instance.
     *
     * @return a new {@code Builder} instance for {@code CompleteUploadResponse}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing instances of {@code CompleteUploadResponse}.
     * Allows setting various properties of the response in a fluent manner.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ARN (Amazon Resource Name) associated with a specific resource.
         * This variable is typically used to uniquely identify and reference
         * an AWS resource such as a bucket, object, or data entity.
         * It serves as a globally unique identifier in the resource lifecycle.
         */
        private String ern;

        /**
         * The Amazon Resource Name (ARN) for a specific bucket. This variable is used to uniquely identify
         * and reference an AWS bucket resource. It provides a globally unique identifier that anchors the
         * bucket to its associated operations or lifecycle events.
         */
        private String bucketErn;

        /**
         * The key associated with a specific object or resource.
         * It is typically used to uniquely identify an object within a bucket or
         * to reference a specific resource in storage or database systems.
         */
        private String key;

        /**
         * Represents the size of a specific resource or object.
         * This variable is typically used to store the size in bytes,
         * providing a measure of the storage or data associated with the resource.
         */
        private long size;

        /**
         * Represents the status of an operation or resource.
         * This variable may indicate the current state, such as success, failure, or in-progress,
         * and is typically used to track the lifecycle or outcome of an action.
         */
        private String status;

        /**
         * Represents the content type of an resource or object.
         * Typically used to specify the MIME type of the resource content,
         * such as "application/json", "text/html", or "image/png".
         * This variable helps in determining how the data should be processed or displayed.
         */
        private String contentType;

        /**
         * Represents the MD5 checksum of an object or file, used to verify data integrity.
         */
        private String md5Sum;

        /**
         * Sets the ERN (Entity Resource Name) for the current builder instance.
         *
         * @param ern the ERN to set
         * @return the current builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the bucket ERN (Entity Resource Name) for the current builder instance.
         *
         * @param bucketErn the bucket ERN to set
         * @return the current builder instance
         */
        public Builder bucketErn(String bucketErn) {
            this.bucketErn = bucketErn;
            return this;
        }

        /**
         * Sets the key for the current builder instance.
         *
         * @param key the key to set
         * @return the current builder instance
         */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /**
         * Sets the size for the current builder instance.
         *
         * @param size the size to set
         * @return the current builder instance
         */
        public Builder size(long size) {
            this.size = size;
            return this;
        }

        /**
         * Sets the status for the current builder instance.
         *
         * @param status the status to set
         * @return the current builder instance
         */
        public Builder status(String status) {
            this.status = status;
            return this;
        }

        /**
         * Sets the content type for the current builder instance.
         *
         * @param contentType the content type to set
         * @return the current builder instance
         */
        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        /**
         * Sets the MD5 checksum for the current builder instance.
         *
         * @param md5Sum the MD5 checksum to set
         * @return the current builder instance
         */
        public Builder md5Sum(String md5Sum) {
            this.md5Sum = md5Sum;
            return this;
        }

        /**
         * Builds a new instance of {@code CompleteUploadResponse} using the parameters
         * previously set in the builder.
         *
         * @return a new {@code CompleteUploadResponse} instance containing the values
         *         specified for ERN, bucket ERN, key, size, status, content type, and MD5 checksum.
         */
        public CompleteUploadResponse build() {
            return new CompleteUploadResponse(ern, bucketErn, key, size, status, contentType, md5Sum);
        }
    }
}
