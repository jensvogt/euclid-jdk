package de.jensvogt.euclid.dto.esm;

/**
 * Response returned after a bucket has stopped encrypting new objects.
 *
 * <p>A bucket that was not encrypting anyway is not an error: the caller asked for it to end up not
 * encrypting, and it does. An empty {@code previousKeyErn} is how that answer says nothing changed.
 *
 * @param ern              the ERN (Entity Resource Name) of the bucket
 * @param name             the name of the bucket
 * @param previousKeyErn   the ERN of the key the bucket was encrypting under, empty if it was not
 * @param previousKeyId    the name of that key
 * @param encryptedObjects how many objects in the bucket are still encrypted and are still read
 *                         back through the key they were written under
 */
public record DisableEncryptionResponse(String ern, String name, String previousKeyErn, String previousKeyId,
                                        long encryptedObjects) {

    /**
     * Creates a new instance of the Builder for constructing a DisableEncryptionResponse object.
     *
     * @return a new Builder instance for constructing DisableEncryptionResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link DisableEncryptionResponse} instances.
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
         * The ERN of the key the bucket was encrypting under.
         */
        private String previousKeyErn;

        /**
         * The name of that key.
         */
        private String previousKeyId;

        /**
         * How many objects in the bucket are still encrypted.
         */
        private long encryptedObjects;

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
         * Sets the ERN of the key the bucket was encrypting under.
         *
         * @param previousKeyErn the previous key ERN, empty if the bucket was not encrypting
         * @return the builder instance
         */
        public Builder previousKeyErn(String previousKeyErn) {
            this.previousKeyErn = previousKeyErn;
            return this;
        }

        /**
         * Sets the name of the key the bucket was encrypting under.
         *
         * @param previousKeyId the previous key name
         * @return the builder instance
         */
        public Builder previousKeyId(String previousKeyId) {
            this.previousKeyId = previousKeyId;
            return this;
        }

        /**
         * Sets how many objects in the bucket are still encrypted.
         *
         * @param encryptedObjects the number of still-encrypted objects
         * @return the builder instance
         */
        public Builder encryptedObjects(long encryptedObjects) {
            this.encryptedObjects = encryptedObjects;
            return this;
        }

        /**
         * Builds and returns a new instance of DisableEncryptionResponse using the properties set on the Builder.
         *
         * @return a new DisableEncryptionResponse instance.
         */
        public DisableEncryptionResponse build() {
            return new DisableEncryptionResponse(ern, name, previousKeyErn, previousKeyId, encryptedObjects);
        }
    }
}
