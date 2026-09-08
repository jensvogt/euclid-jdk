package de.jensvogt.euclid.dto.esm;

/**
 * Request to stop encrypting new objects written to a bucket.
 *
 * <p>The mirror image of {@link EnableEncryptionRequest} in one respect and no other: it says what
 * happens to the next upload, and nothing else. It is not an undo - nothing already in the bucket
 * is decrypted or rewritten, and the key is left strictly alone rather than retired, because those
 * objects are still read back through it.
 *
 * @param bucketErn the ERN (Entity Resource Name) of the bucket to stop encrypting
 */
public record DisableEncryptionRequest(String bucketErn) {

    /**
     * Creates a new instance of the Builder for constructing a DisableEncryptionRequest object.
     *
     * @return a new Builder instance for constructing DisableEncryptionRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link DisableEncryptionRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN (Entity Resource Name) of the bucket to stop encrypting.
         */
        private String bucketErn;

        /**
         * Sets the ERN of the bucket to stop encrypting.
         *
         * @param bucketErn the ERN of the bucket
         * @return the builder instance
         */
        public Builder bucketErn(String bucketErn) {
            this.bucketErn = bucketErn;
            return this;
        }

        /**
         * Builds and returns a new instance of DisableEncryptionRequest using the properties set on the Builder.
         *
         * @return a new DisableEncryptionRequest instance populated with the bucket ERN.
         */
        public DisableEncryptionRequest build() {
            return new DisableEncryptionRequest(bucketErn);
        }
    }
}
