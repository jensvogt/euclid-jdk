package de.jensvogt.euclid.dto.eam;

/**
 * Response returned after successfully creating a SigV4 access key.
 *
 * @param accessKeyId     public identifier of the newly created key, e.g. "AKIA..."
 * @param secretAccessKey secret used to sign requests; shown only this once
 * @param createdAt       creation timestamp, ISO8601
 */
public record CreateAccessKeyResponse(String accessKeyId, String secretAccessKey, String createdAt) {

    /**
     * Creates a new instance of the Builder for constructing a CreateAccessKeyResponse object.
     *
     * @return a new Builder instance for constructing CreateAccessKeyResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CreateAccessKeyResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The public identifier of the newly created key.
         */
        private String accessKeyId;

        /**
         * The secret used to sign requests.
         */
        private String secretAccessKey;

        /**
         * The creation timestamp, ISO8601.
         */
        private String createdAt;

        /**
         * Sets the public identifier of the newly created key.
         *
         * @param accessKeyId the access key ID
         * @return the builder instance
         */
        public Builder accessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
            return this;
        }

        /**
         * Sets the secret used to sign requests.
         *
         * @param secretAccessKey the secret access key
         * @return the builder instance
         */
        public Builder secretAccessKey(String secretAccessKey) {
            this.secretAccessKey = secretAccessKey;
            return this;
        }

        /**
         * Sets the creation timestamp.
         *
         * @param createdAt the creation timestamp, ISO8601
         * @return the builder instance
         */
        public Builder createdAt(String createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * Builds and returns a new instance of CreateAccessKeyResponse using the properties set on the Builder.
         *
         * @return a new CreateAccessKeyResponse instance.
         */
        public CreateAccessKeyResponse build() {
            return new CreateAccessKeyResponse(accessKeyId, secretAccessKey, createdAt);
        }
    }
}
