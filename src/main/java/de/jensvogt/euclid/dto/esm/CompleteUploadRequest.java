package de.jensvogt.euclid.dto.esm;

/**
 * Represents a request to complete an upload operation.
 *
 * This class is a record that encapsulates the information required to
 * finalize an upload process. It includes the upload ID that uniquely
 * identifies the upload session to be completed.
 *
 * The class provides a static {@code Builder} class to facilitate the
 * construction of immutable instances of {@code CompleteUploadRequest}.
 *
 * @param uploadId The unique identifier of the upload session to be completed.
 */
public record CompleteUploadRequest(String uploadId) {

    /**
     * Creates and returns a new {@code Builder} instance for constructing
     * instances of {@code CompleteUploadRequest}.
     *
     * @return a new {@code Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder class for constructing instances of {@code CompleteUploadRequest}.
     *
     * This class provides methods to set the necessary fields required to create
     * a {@code CompleteUploadRequest} instance. It follows the builder pattern, allowing
     * for the incremental construction of the request object and ensuring the final
     * object is in a valid state.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Represents the unique identifier for a specific upload operation.
         *
         * This identifier is used to track and associate parts of an upload
         * with the corresponding request and enable the completion of the
         * multipart upload process.
         */
        private String uploadId;

        /**
         * Sets the upload ID for the current builder instance.
         *
         * @param uploadId the unique identifier for the upload operation
         * @return the current {@code Builder} instance for method chaining
         */
        public Builder uploadId(String uploadId) {
            this.uploadId = uploadId;
            return this;
        }

        /**
         * Builds and returns a new {@code CompleteUploadRequest} instance with the current state
         * of the builder.
         *
         * @return a newly constructed {@code CompleteUploadRequest} containing the configured
         *         upload ID.
         */
        public CompleteUploadRequest build() {
            return new CompleteUploadRequest(uploadId);
        }
    }
}
