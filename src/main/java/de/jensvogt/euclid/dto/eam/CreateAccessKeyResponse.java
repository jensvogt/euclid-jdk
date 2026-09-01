package de.jensvogt.euclid.dto.eam;

import de.jensvogt.euclid.dto.Metadata;

/**
 * Response returned after successfully creating a SigV4 access key.
 *
 * @param metadata        the caller identity the server resolved the request to
 * @param accessKeyId     public identifier of the newly created key, e.g. "AKIA..."
 * @param secretAccessKey secret used to sign requests; shown only this once
 * @param createdAt       creation timestamp, ISO8601
 */
public record CreateAccessKeyResponse(Metadata metadata, String accessKeyId, String secretAccessKey, String createdAt) {

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
         * The caller identity the server resolved the request to.
         */
        private Metadata metadata;

        /**
         * Public identifier of the newly created key, e.g. "AKIA...".
         */
        private String accessKeyId;

        /**
         * Secret used to sign requests; shown only this once.
         */
        private String secretAccessKey;

        /**
         * Creation timestamp, ISO8601.
         */
        private String createdAt;

        /**
         * Sets the caller identity the server resolved the request to.
         *
         * @param metadata the caller identity the server resolved the request to
         * @return the builder instance
         */
        public Builder metadata(Metadata metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Sets public identifier of the newly created key, e.g. "AKIA...".
         *
         * @param accessKeyId public identifier of the newly created key, e.g. "AKIA..."
         * @return the builder instance
         */
        public Builder accessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
            return this;
        }

        /**
         * Sets secret used to sign requests; shown only this once.
         *
         * @param secretAccessKey secret used to sign requests; shown only this once
         * @return the builder instance
         */
        public Builder secretAccessKey(String secretAccessKey) {
            this.secretAccessKey = secretAccessKey;
            return this;
        }

        /**
         * Sets creation timestamp, ISO8601.
         *
         * @param createdAt creation timestamp, ISO8601
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
            return new CreateAccessKeyResponse(metadata, accessKeyId, secretAccessKey, createdAt);
        }
    }
}
