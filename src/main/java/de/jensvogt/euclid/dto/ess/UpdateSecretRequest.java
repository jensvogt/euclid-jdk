package de.jensvogt.euclid.dto.ess;

/**
 * Request to change a secret that already exists: a rotation, a change of description, or a move to
 * another key. Only what the request names changes.
 *
 * <p>Absent and empty mean different things here, which is why the unset fields are {@code null}
 * rather than {@code ""}. The server asks whether a field is present at all: leaving
 * {@link #description()} null leaves the stored description alone, while sending {@code ""} clears
 * it. {@link de.jensvogt.euclid.module.ess.EuclidEss} serializes with a mapper that drops null
 * fields rather than writing them as {@code null}, so the distinction survives serialization.
 *
 * <p>A request that names none of the three is refused with HTTP 400 rather than treated as a
 * no-op, as is a {@code value} of {@code ""} - an empty secret is almost always a mistake.
 *
 * @param name        the name of the secret to change
 * @param value       the new value, or {@code null} to leave it alone. Setting a value is what
 *                    counts as a rotation and what bumps {@code version}
 * @param description the new description, or {@code null} to leave it alone; {@code ""} clears it
 * @param keyErn      the ERN of another EKM key to move the secret to, or {@code null}/empty to
 *                    leave it under the key it has. Moving it re-encrypts the stored value but is
 *                    not a rotation: it changes how the secret is protected, not what it is
 */
public record UpdateSecretRequest(String name, String value, String description, String keyErn) {

    /**
     * Creates a new instance of the Builder for constructing an UpdateSecretRequest object.
     *
     * @return a new Builder instance for constructing UpdateSecretRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link UpdateSecretRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The name of the secret to change.
         */
        private String name;

        /**
         * The new value, null to leave it alone.
         */
        private String value;

        /**
         * The new description, null to leave it alone.
         */
        private String description;

        /**
         * The ERN of another EKM key to move the secret to, null to leave it where it is.
         */
        private String keyErn;

        /**
         * Sets the name of the secret to change.
         *
         * @param name the secret name
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the new value.
         *
         * @param value the new secret value, or {@code null} to leave it alone
         * @return the builder instance
         */
        public Builder value(String value) {
            this.value = value;
            return this;
        }

        /**
         * Sets the new description.
         *
         * @param description the new description, {@code null} to leave it alone, {@code ""} to clear it
         * @return the builder instance
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the ERN of another EKM key to move the secret to.
         *
         * @param keyErn the key ERN, or {@code null} to leave the secret under the key it has
         * @return the builder instance
         */
        public Builder keyErn(String keyErn) {
            this.keyErn = keyErn;
            return this;
        }

        /**
         * Builds and returns a new instance of UpdateSecretRequest using the properties set on the Builder.
         *
         * @return a new UpdateSecretRequest instance.
         */
        public UpdateSecretRequest build() {
            return new UpdateSecretRequest(name, value, description, keyErn);
        }
    }
}
