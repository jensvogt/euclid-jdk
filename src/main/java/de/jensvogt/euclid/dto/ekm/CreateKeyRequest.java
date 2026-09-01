package de.jensvogt.euclid.dto.ekm;

/**
 * Request to create a new encryption key. The key is identified by a randomly generated ID the
 * server mints and returns - there is no caller-chosen name.
 *
 * @param algorithm the key algorithm; only {@code "AES"} is supported so far
 * @param length    the key length in bits, 128 or 256
 */
public record CreateKeyRequest(String algorithm, long length) {

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
         * Builds and returns a new instance of CreateKeyRequest using the properties set on the Builder.
         *
         * @return a new CreateKeyRequest instance.
         */
        public CreateKeyRequest build() {
            return new CreateKeyRequest(algorithm, length);
        }
    }
}
