package de.jensvogt.euclid.dto.ekm;

/**
 * Request to revoke a key, blocking further encryption with it while leaving decryption intact.
 *
 * @param ern the ERN of the key to revoke
 */
public record RevokeKeyRequest(String ern) {

    /**
     * Creates a new instance of the Builder for constructing a RevokeKeyRequest object.
     *
     * @return a new Builder instance for constructing RevokeKeyRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link RevokeKeyRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN of the key to revoke.
         */
        private String ern = "";

        /**
         * Sets the ERN of the key to revoke.
         *
         * @param ern the ERN of the key to revoke
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Builds and returns a new instance of RevokeKeyRequest using the properties set on the Builder.
         *
         * @return a new RevokeKeyRequest instance.
         */
        public RevokeKeyRequest build() {
            return new RevokeKeyRequest(ern);
        }
    }
}
