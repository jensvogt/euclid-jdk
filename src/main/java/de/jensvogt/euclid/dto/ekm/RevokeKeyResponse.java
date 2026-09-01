package de.jensvogt.euclid.dto.ekm;

/**
 * The key a revoke-key action blocked from further encryption.
 *
 * @param ern    the key's ERN
 * @param name   the key ID
 * @param status the key's lifecycle status, {@code "REVOKED"} after this call
 */
public record RevokeKeyResponse(String ern, String name, String status) {

    /**
     * Creates a new instance of the Builder for constructing a RevokeKeyResponse object.
     *
     * @return a new Builder instance for constructing RevokeKeyResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link RevokeKeyResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The key's ERN.
         */
        private String ern;

        /**
         * The key ID.
         */
        private String name;

        /**
         * The key's lifecycle status, {@code "REVOKED"} after this call.
         */
        private String status;

        /**
         * Sets the key's ERN.
         *
         * @param ern the key's ERN
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the key ID.
         *
         * @param name the key ID
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the key's lifecycle status, {@code "REVOKED"} after this call.
         *
         * @param status the key's lifecycle status, {@code "REVOKED"} after this call
         * @return the builder instance
         */
        public Builder status(String status) {
            this.status = status;
            return this;
        }

        /**
         * Builds and returns a new instance of RevokeKeyResponse using the properties set on the Builder.
         *
         * @return a new RevokeKeyResponse instance.
         */
        public RevokeKeyResponse build() {
            return new RevokeKeyResponse(ern, name, status);
        }
    }
}
