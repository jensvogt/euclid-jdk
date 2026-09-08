package de.jensvogt.euclid.dto.esm;

/**
 * Response returned after encryption at rest has been turned on for a bucket.
 *
 * @param ern             the ERN (Entity Resource Name) of the bucket
 * @param name            the name of the bucket
 * @param keyErn          the ERN of the EKM key objects are now encrypted under
 * @param keyId           the name of that key
 * @param algorithm       the key's algorithm, {@code "AES-256"} for a key created by this call
 * @param keyCreated      whether the key was created by this call rather than named by the caller
 * @param existingObjects how many objects were already in the bucket and are therefore still stored
 *                        as they were - enabling encryption does not rewrite them
 */
public record EnableEncryptionResponse(String ern, String name, String keyErn, String keyId, String algorithm,
                                       boolean keyCreated, long existingObjects) {

    /**
     * Creates a new instance of the Builder for constructing an EnableEncryptionResponse object.
     *
     * @return a new Builder instance for constructing EnableEncryptionResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link EnableEncryptionResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN (Entity Resource Name) of the bucket.
         */
        private String ern;

        /**
         * The name of the bucket.
         */
        private String name;

        /**
         * The ERN of the EKM key objects are now encrypted under.
         */
        private String keyErn;

        /**
         * The name of that key.
         */
        private String keyId;

        /**
         * The key's algorithm.
         */
        private String algorithm;

        /**
         * Whether the key was created by this call.
         */
        private boolean keyCreated;

        /**
         * How many objects were already in the bucket.
         */
        private long existingObjects;

        /**
         * Sets the ERN of the bucket.
         *
         * @param ern the ERN of the bucket
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the name of the bucket.
         *
         * @param name the bucket name
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the ERN of the EKM key.
         *
         * @param keyErn the key ERN
         * @return the builder instance
         */
        public Builder keyErn(String keyErn) {
            this.keyErn = keyErn;
            return this;
        }

        /**
         * Sets the name of the EKM key.
         *
         * @param keyId the key name
         * @return the builder instance
         */
        public Builder keyId(String keyId) {
            this.keyId = keyId;
            return this;
        }

        /**
         * Sets the key's algorithm.
         *
         * @param algorithm the algorithm
         * @return the builder instance
         */
        public Builder algorithm(String algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        /**
         * Sets whether the key was created by this call.
         *
         * @param keyCreated true when the server created the key
         * @return the builder instance
         */
        public Builder keyCreated(boolean keyCreated) {
            this.keyCreated = keyCreated;
            return this;
        }

        /**
         * Sets how many objects were already in the bucket.
         *
         * @param existingObjects the number of pre-existing objects
         * @return the builder instance
         */
        public Builder existingObjects(long existingObjects) {
            this.existingObjects = existingObjects;
            return this;
        }

        /**
         * Builds and returns a new instance of EnableEncryptionResponse using the properties set on the Builder.
         *
         * @return a new EnableEncryptionResponse instance.
         */
        public EnableEncryptionResponse build() {
            return new EnableEncryptionResponse(ern, name, keyErn, keyId, algorithm, keyCreated, existingObjects);
        }
    }
}
