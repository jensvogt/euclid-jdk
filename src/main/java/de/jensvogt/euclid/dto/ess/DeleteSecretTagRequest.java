package de.jensvogt.euclid.dto.ess;

/**
 * Request to remove a tag from a secret.
 *
 * @param name the name of the secret the tag belongs to
 * @param key  the tag key to remove
 */
public record DeleteSecretTagRequest(String name, String key) {

    /**
     * Creates a new instance of the Builder for constructing a DeleteSecretTagRequest object.
     *
     * @return a new Builder instance for constructing DeleteSecretTagRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link DeleteSecretTagRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The name of the secret the tag belongs to.
         */
        private String name;

        /**
         * The tag key to remove.
         */
        private String key;

        /**
         * Sets the name of the secret the tag belongs to.
         *
         * @param name the secret name
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the tag key to remove.
         *
         * @param key the tag key
         * @return the builder instance
         */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /**
         * Builds and returns a new instance of DeleteSecretTagRequest using the properties set on the Builder.
         *
         * @return a new DeleteSecretTagRequest instance.
         */
        public DeleteSecretTagRequest build() {
            return new DeleteSecretTagRequest(name, key);
        }
    }
}
