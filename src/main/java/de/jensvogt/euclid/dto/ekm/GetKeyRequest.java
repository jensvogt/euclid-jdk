package de.jensvogt.euclid.dto.ekm;

/**
 * Request for one key, by ERN or by name.
 * <p>
 * Exactly one of the two is sent. A name is resolved in the caller's own account and namespace -
 * the pair {@code createKey} built the ERN from - so it means "my key of that name". An ERN names
 * one key in the installation and is what an encrypted bucket or secret carries.
 * <p>
 * What comes back is the key's description, never its material.
 *
 * @param ern  the ERN uniquely identifying the key, or null when it is named instead
 * @param name the key's name, used when no ERN is given
 */
public record GetKeyRequest(String ern, String name) {

    /**
     * Creates a new instance of the Builder for constructing a GetKeyRequest object.
     *
     * @return a new Builder instance for constructing GetKeyRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetKeyRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN uniquely identifying the key.
         */
        private String ern;

        /**
         * The key's name.
         */
        private String name;

        /**
         * Sets the ERN of the key.
         *
         * @param ern the ERN uniquely identifying the key
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the name of the key.
         *
         * @param name the key's name, resolved in the caller's own account and namespace
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Builds and returns a new instance of GetKeyRequest using the properties set on the Builder.
         *
         * @return a new GetKeyRequest instance.
         */
        public GetKeyRequest build() {
            return new GetKeyRequest(ern, name);
        }
    }
}
