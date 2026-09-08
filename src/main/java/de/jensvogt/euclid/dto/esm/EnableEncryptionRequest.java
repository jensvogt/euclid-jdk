package de.jensvogt.euclid.dto.esm;

/**
 * Request to turn on encryption at rest for a bucket.
 *
 * <p>From here on every object written to the bucket is encrypted under an EKM key before it
 * reaches the disk. Objects already in the bucket are left exactly as they are - each one records
 * the key it is under, so the bucket goes on serving what it held before.
 *
 * @param bucketErn the ERN (Entity Resource Name) of the bucket to encrypt
 * @param keyId     the name of an existing EKM key to use, or {@code null}/empty to have one
 *                  created - an unnamed key is created as AES-256 and belongs to EKM from that
 *                  moment on, which means deleting it there is what makes this bucket's objects
 *                  unrecoverable
 */
public record EnableEncryptionRequest(String bucketErn, String keyId) {

    /**
     * Creates a new instance of the Builder for constructing an EnableEncryptionRequest object.
     *
     * @return a new Builder instance for constructing EnableEncryptionRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link EnableEncryptionRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN (Entity Resource Name) of the bucket to encrypt.
         */
        private String bucketErn;

        /**
         * The name of an existing EKM key to use, empty to have one created.
         */
        private String keyId = "";

        /**
         * Sets the ERN of the bucket to encrypt.
         *
         * @param bucketErn the ERN of the bucket
         * @return the builder instance
         */
        public Builder bucketErn(String bucketErn) {
            this.bucketErn = bucketErn;
            return this;
        }

        /**
         * Sets the name of the EKM key to encrypt under.
         *
         * @param keyId the key name, or {@code null}/empty to have a key created
         * @return the builder instance
         */
        public Builder keyId(String keyId) {
            this.keyId = keyId;
            return this;
        }

        /**
         * Builds and returns a new instance of EnableEncryptionRequest using the properties set on the Builder.
         *
         * @return a new EnableEncryptionRequest instance populated with the bucket ERN and key id.
         */
        public EnableEncryptionRequest build() {
            return new EnableEncryptionRequest(bucketErn, keyId);
        }
    }
}
