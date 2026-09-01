package de.jensvogt.euclid.dto.esm;

/**
 * Request to finish a multipart download, releasing the session's server-side scratch state.
 *
 * @param downloadId the ID of the download to complete
 */
public record CompleteDownloadRequest(String downloadId) {

    /**
     * Creates a new instance of the Builder for constructing a CompleteDownloadRequest object.
     *
     * @return a new Builder instance for constructing CompleteDownloadRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CompleteDownloadRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ID of the download to complete.
         */
        private String downloadId;

        /**
         * Sets the ID of the download to complete.
         *
         * @param downloadId the ID of the download to complete
         * @return the builder instance
         */
        public Builder downloadId(String downloadId) {
            this.downloadId = downloadId;
            return this;
        }

        /**
         * Builds and returns a new instance of CompleteDownloadRequest using the properties set on the Builder.
         *
         * @return a new CompleteDownloadRequest instance.
         */
        public CompleteDownloadRequest build() {
            return new CompleteDownloadRequest(downloadId);
        }
    }
}
