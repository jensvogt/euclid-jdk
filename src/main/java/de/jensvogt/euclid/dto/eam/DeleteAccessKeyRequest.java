package de.jensvogt.euclid.dto.eam;

/**
 * Request to delete one of the caller's own access keys.
 *
 * @param accessKeyId access key ID to delete, e.g. "AKIA..."
 */
public record DeleteAccessKeyRequest(String accessKeyId) {

    /**
     * Creates a new instance of the Builder for constructing a DeleteAccessKeyRequest object.
     *
     * @return a new Builder instance for constructing DeleteAccessKeyRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link DeleteAccessKeyRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The access key ID to delete.
         */
        private String accessKeyId;

        /**
         * Sets the access key ID to delete.
         *
         * @param accessKeyId the access key ID
         * @return the builder instance
         */
        public Builder accessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
            return this;
        }

        /**
         * Builds and returns a new instance of DeleteAccessKeyRequest using the properties set on the Builder.
         *
         * @return a new DeleteAccessKeyRequest instance.
         */
        public DeleteAccessKeyRequest build() {
            return new DeleteAccessKeyRequest(accessKeyId);
        }
    }
}
