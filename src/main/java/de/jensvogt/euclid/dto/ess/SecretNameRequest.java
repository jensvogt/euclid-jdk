package de.jensvogt.euclid.dto.ess;

/**
 * Request naming a single secret, shared by get-secret and delete-secret - both take nothing but
 * the name.
 *
 * @param name the name of the secret the action applies to
 */
public record SecretNameRequest(String name) {

    /**
     * Creates a new instance of the Builder for constructing a SecretNameRequest object.
     *
     * @return a new Builder instance for constructing SecretNameRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SecretNameRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The name of the secret the action applies to.
         */
        private String name;

        /**
         * Sets the name of the secret the action applies to.
         *
         * @param name the name of the secret
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Builds and returns a new instance of SecretNameRequest using the properties set on the Builder.
         *
         * @return a new SecretNameRequest instance populated with the name.
         */
        public SecretNameRequest build() {
            return new SecretNameRequest(name);
        }
    }
}
