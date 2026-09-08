package de.jensvogt.euclid.dto.ess;

import de.jensvogt.euclid.dto.ess.model.Secret;

/**
 * Response to get-secret: the secret's metadata together with its decrypted value.
 *
 * <p>The one response in this module that carries a plaintext. get-secret is the only ESS action
 * that decrypts anything - everything else works on ciphertext it never looks inside - so this is
 * the only object worth being careful with: do not log it, and do not hold it longer than the call
 * that needed it.
 *
 * @param secret the secret's metadata
 * @param value  the decrypted secret
 */
public record GetSecretResponse(Secret secret, String value) {

    /**
     * Creates a new instance of the Builder for constructing a GetSecretResponse object.
     *
     * @return a new Builder instance for constructing GetSecretResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetSecretResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The secret's metadata.
         */
        private Secret secret;

        /**
         * The decrypted secret.
         */
        private String value;

        /**
         * Sets the secret's metadata.
         *
         * @param secret the metadata
         * @return the builder instance
         */
        public Builder secret(Secret secret) {
            this.secret = secret;
            return this;
        }

        /**
         * Sets the decrypted secret.
         *
         * @param value the plaintext value
         * @return the builder instance
         */
        public Builder value(String value) {
            this.value = value;
            return this;
        }

        /**
         * Builds and returns a new instance of GetSecretResponse using the properties set on the Builder.
         *
         * @return a new GetSecretResponse instance.
         */
        public GetSecretResponse build() {
            return new GetSecretResponse(secret, value);
        }
    }
}
