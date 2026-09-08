package de.jensvogt.euclid.dto.ess;

/**
 * Response returned after a secret has been deleted.
 *
 * @param ern  the ERN of the deleted secret
 * @param name the name of the deleted secret
 */
public record DeleteSecretResponse(String ern, String name) {

    /**
     * Creates a new instance of the Builder for constructing a DeleteSecretResponse object.
     *
     * @return a new Builder instance for constructing DeleteSecretResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link DeleteSecretResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN of the deleted secret.
         */
        private String ern;

        /**
         * The name of the deleted secret.
         */
        private String name;

        /**
         * Sets the ERN of the deleted secret.
         *
         * @param ern the secret ERN
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the name of the deleted secret.
         *
         * @param name the secret name
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Builds and returns a new instance of DeleteSecretResponse using the properties set on the Builder.
         *
         * @return a new DeleteSecretResponse instance.
         */
        public DeleteSecretResponse build() {
            return new DeleteSecretResponse(ern, name);
        }
    }
}
