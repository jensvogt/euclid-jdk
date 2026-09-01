package de.jensvogt.euclid.dto.ekm;

/**
 * The key a create-key action minted.
 *
 * @param ern       the new key's ERN
 * @param name      the generated key ID, which encrypt/decrypt and delete-key take
 * @param algorithm the key algorithm
 * @param length    the key length in bits
 * @param status    the key's lifecycle status, {@code "AVAILABLE"} for a new key
 */
public record CreateKeyResponse(String ern, String name, String algorithm, long length, String status) {

    /**
     * Creates a new instance of the Builder for constructing a CreateKeyResponse object.
     *
     * @return a new Builder instance for constructing CreateKeyResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CreateKeyResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The new key's ERN.
         */
        private String ern;

        /**
         * The generated key ID, which encrypt/decrypt and delete-key take.
         */
        private String name;

        /**
         * The key algorithm.
         */
        private String algorithm;

        /**
         * The key length in bits.
         */
        private long length = 0;

        /**
         * The key's lifecycle status, {@code "AVAILABLE"} for a new key.
         */
        private String status;

        /**
         * Sets the new key's ERN.
         *
         * @param ern the new key's ERN
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the generated key ID, which encrypt/decrypt and delete-key take.
         *
         * @param name the generated key ID, which encrypt/decrypt and delete-key take
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the key algorithm.
         *
         * @param algorithm the key algorithm
         * @return the builder instance
         */
        public Builder algorithm(String algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        /**
         * Sets the key length in bits.
         *
         * @param length the key length in bits
         * @return the builder instance
         */
        public Builder length(long length) {
            this.length = length;
            return this;
        }

        /**
         * Sets the key's lifecycle status, {@code "AVAILABLE"} for a new key.
         *
         * @param status the key's lifecycle status, {@code "AVAILABLE"} for a new key
         * @return the builder instance
         */
        public Builder status(String status) {
            this.status = status;
            return this;
        }

        /**
         * Builds and returns a new instance of CreateKeyResponse using the properties set on the Builder.
         *
         * @return a new CreateKeyResponse instance.
         */
        public CreateKeyResponse build() {
            return new CreateKeyResponse(ern, name, algorithm, length, status);
        }
    }
}
