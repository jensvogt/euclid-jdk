package de.jensvogt.euclid.dto.ekm;

/**
 * Request to create a new encryption key. The key is identified by a randomly generated ID the
 * server mints and returns - there is no caller-chosen name.
 *
 * @param algorithm   the key algorithm; only {@code "AES"} is supported so far
 * @param length      the key length in bits, 128 or 256
 * @param description what the key is for, stored with it and reported by list-keys. Optional -
 *                    an empty string means none. Worth giving: the generated ID says nothing
 *                    about what the key protects, and delete-key is not reversible
 */
public record CreateKeyRequest(String algorithm, long length, String description) {

    /**
     * Creates a request without a description - the shape this record had before descriptions
     * existed, so code written against it still compiles.
     *
     * @param algorithm the key algorithm; only {@code "AES"} is supported so far
     * @param length    the key length in bits, 128 or 256
     */
    public CreateKeyRequest(String algorithm, long length) {
        this(algorithm, length, "");
    }

    /**
     * Creates a new instance of the Builder for constructing a CreateKeyRequest object.
     *
     * @return a new Builder instance for constructing CreateKeyRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CreateKeyRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The key algorithm; only {@code "AES"} is supported so far.
         */
        private String algorithm = "";

        /**
         * The key length in bits, 128 or 256.
         */
        private long length = 128;

        /**
         * What the key is for; empty when the caller gives none.
         */
        private String description = "";

        /**
         * Sets the key algorithm; only {@code "AES"} is supported so far.
         *
         * @param algorithm the key algorithm; only {@code "AES"} is supported so far
         * @return the builder instance
         */
        public Builder algorithm(String algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        /**
         * Sets the key length in bits, 128 or 256.
         *
         * @param length the key length in bits, 128 or 256
         * @return the builder instance
         */
        public Builder length(long length) {
            this.length = length;
            return this;
        }

        /**
         * Sets what the key is for, stored with it and reported by list-keys.
         *
         * @param description what the key is for; {@code null} is treated as none
         * @return the builder instance
         */
        public Builder description(String description) {
            this.description = description == null ? "" : description;
            return this;
        }

        /**
         * Builds and returns a new instance of CreateKeyRequest using the properties set on the Builder.
         *
         * @return a new CreateKeyRequest instance.
         */
        public CreateKeyRequest build() {
            return new CreateKeyRequest(algorithm, length, description);
        }
    }
}
