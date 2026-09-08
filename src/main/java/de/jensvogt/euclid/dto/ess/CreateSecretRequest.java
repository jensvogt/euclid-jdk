package de.jensvogt.euclid.dto.ess;

/**
 * Request to store a new secret.
 *
 * <p>Refused with HTTP 409 if a secret of that name already exists: create-secret quietly replacing
 * a value would be a way to destroy one by accident, and update-secret is the action that says it
 * means to.
 *
 * @param name        the name to store the secret under. Named rather than given as an ERN - a
 *                    value starting with {@code "ern:"} is refused
 * @param description what the secret is for, or {@code null}/empty
 * @param value       the secret itself, which must not be empty. An empty secret is almost always a
 *                    caller whose shell ate the value, and the server refuses it rather than
 *                    storing it for something to read later
 * @param keyErn      the ERN of the EKM key to encrypt under, or {@code null}/empty to use the
 *                    namespace's own key
 */
public record CreateSecretRequest(String name, String description, String value, String keyErn) {

    /**
     * Creates a new instance of the Builder for constructing a CreateSecretRequest object.
     *
     * @return a new Builder instance for constructing CreateSecretRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CreateSecretRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The name to store the secret under.
         */
        private String name;

        /**
         * What the secret is for.
         */
        private String description = "";

        /**
         * The secret itself.
         */
        private String value;

        /**
         * The ERN of the EKM key to encrypt under, empty for the namespace's own key.
         */
        private String keyErn = "";

        /**
         * Sets the name to store the secret under.
         *
         * @param name the secret name
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets what the secret is for.
         *
         * @param description the description
         * @return the builder instance
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the secret itself.
         *
         * @param value the secret value
         * @return the builder instance
         */
        public Builder value(String value) {
            this.value = value;
            return this;
        }

        /**
         * Sets the ERN of the EKM key to encrypt under.
         *
         * @param keyErn the key ERN, or empty for the namespace's own key
         * @return the builder instance
         */
        public Builder keyErn(String keyErn) {
            this.keyErn = keyErn;
            return this;
        }

        /**
         * Builds and returns a new instance of CreateSecretRequest using the properties set on the Builder.
         *
         * @return a new CreateSecretRequest instance.
         */
        public CreateSecretRequest build() {
            return new CreateSecretRequest(name, description, value, keyErn);
        }
    }
}
