package de.jensvogt.euclid.dto.ekm;

/**
 * Request to schedule a key for permanent deletion.
 *
 * @param keyId               the ID of the key to delete, as returned by create-key
 * @param pendingWindowInDays days to wait before the key is permanently removed; must be at least 1
 */
public record DeleteKeyRequest(String keyId, long pendingWindowInDays) {

    /**
     * Creates a new instance of the Builder for constructing a DeleteKeyRequest object.
     *
     * @return a new Builder instance for constructing DeleteKeyRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link DeleteKeyRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ID of the key to delete, as returned by create-key.
         */
        private String keyId = "";

        /**
         * Days to wait before the key is permanently removed; must be at least 1.
         */
        private long pendingWindowInDays = 7;

        /**
         * Sets the ID of the key to delete, as returned by create-key.
         *
         * @param keyId the ID of the key to delete, as returned by create-key
         * @return the builder instance
         */
        public Builder keyId(String keyId) {
            this.keyId = keyId;
            return this;
        }

        /**
         * Sets days to wait before the key is permanently removed; must be at least 1.
         *
         * @param pendingWindowInDays days to wait before the key is permanently removed; must be at least 1
         * @return the builder instance
         */
        public Builder pendingWindowInDays(long pendingWindowInDays) {
            this.pendingWindowInDays = pendingWindowInDays;
            return this;
        }

        /**
         * Builds and returns a new instance of DeleteKeyRequest using the properties set on the Builder.
         *
         * @return a new DeleteKeyRequest instance.
         */
        public DeleteKeyRequest build() {
            return new DeleteKeyRequest(keyId, pendingWindowInDays);
        }
    }
}
