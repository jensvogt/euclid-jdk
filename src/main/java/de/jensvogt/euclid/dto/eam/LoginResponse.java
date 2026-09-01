package de.jensvogt.euclid.dto.eam;

import de.jensvogt.euclid.dto.Metadata;

/**
 * Response returned by a successful login.
 *
 * @param metadata        the caller identity the server resolved the login to
 * @param token           the JWT the session authenticates with
 * @param accessKeyId     public identifier of the SigV4 access key, reused if the user already has an active one
 * @param secretAccessKey secret paired with {@code accessKeyId}
 * @param createdAt       the access key's creation timestamp, ISO8601 - not the login time
 * @param isAdmin         whether the logged-in user has administrator privileges
 */
public record LoginResponse(Metadata metadata, String token, String accessKeyId, String secretAccessKey, String createdAt,
                            boolean isAdmin) {

    /**
     * Creates a new instance of the Builder for constructing a LoginResponse object.
     *
     * @return a new Builder instance for constructing LoginResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link LoginResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The caller identity the server resolved the login to.
         */
        private Metadata metadata;

        /**
         * The JWT the session authenticates with.
         */
        private String token;

        /**
         * Public identifier of the SigV4 access key, reused if the user already has an active one.
         */
        private String accessKeyId;

        /**
         * Secret paired with {@code accessKeyId}.
         */
        private String secretAccessKey;

        /**
         * The access key's creation timestamp, ISO8601 - not the login time.
         */
        private String createdAt;

        /**
         * Whether the logged-in user has administrator privileges.
         */
        private boolean isAdmin = false;

        /**
         * Sets the caller identity the server resolved the login to.
         *
         * @param metadata the caller identity the server resolved the login to
         * @return the builder instance
         */
        public Builder metadata(Metadata metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Sets the JWT the session authenticates with.
         *
         * @param token the JWT the session authenticates with
         * @return the builder instance
         */
        public Builder token(String token) {
            this.token = token;
            return this;
        }

        /**
         * Sets public identifier of the SigV4 access key, reused if the user already has an active one.
         *
         * @param accessKeyId public identifier of the SigV4 access key, reused if the user already has an active one
         * @return the builder instance
         */
        public Builder accessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
            return this;
        }

        /**
         * Sets secret paired with {@code accessKeyId}.
         *
         * @param secretAccessKey secret paired with {@code accessKeyId}
         * @return the builder instance
         */
        public Builder secretAccessKey(String secretAccessKey) {
            this.secretAccessKey = secretAccessKey;
            return this;
        }

        /**
         * Sets the access key's creation timestamp, ISO8601 - not the login time.
         *
         * @param createdAt the access key's creation timestamp, ISO8601 - not the login time
         * @return the builder instance
         */
        public Builder createdAt(String createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * Sets whether the logged-in user has administrator privileges.
         *
         * @param isAdmin whether the logged-in user has administrator privileges
         * @return the builder instance
         */
        public Builder isAdmin(boolean isAdmin) {
            this.isAdmin = isAdmin;
            return this;
        }

        /**
         * Builds and returns a new instance of LoginResponse using the properties set on the Builder.
         *
         * @return a new LoginResponse instance.
         */
        public LoginResponse build() {
            return new LoginResponse(metadata, token, accessKeyId, secretAccessKey, createdAt, isAdmin);
        }
    }
}
