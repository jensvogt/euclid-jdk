package de.jensvogt.euclid.dto.ekm;

/**
 * The key a set-key-description action described, as it now reads.
 *
 * @param ern         the key's ERN
 * @param name        the key ID
 * @param description the description the key now carries, empty if it was cleared
 */
public record SetKeyDescriptionResponse(String ern, String name, String description) {

    /**
     * Creates a new instance of the Builder for constructing a SetKeyDescriptionResponse object.
     *
     * @return a new Builder instance for constructing SetKeyDescriptionResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SetKeyDescriptionResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The key's ERN.
         */
        private String ern;

        /**
         * The key ID.
         */
        private String name;

        /**
         * The description the key now carries.
         */
        private String description;

        /**
         * Sets the key's ERN.
         *
         * @param ern the key's ERN
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the key ID.
         *
         * @param name the key ID
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the description the key now carries.
         *
         * @param description the description the key now carries
         * @return the builder instance
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Builds and returns a new instance of SetKeyDescriptionResponse using the properties set on the Builder.
         *
         * @return a new SetKeyDescriptionResponse instance.
         */
        public SetKeyDescriptionResponse build() {
            return new SetKeyDescriptionResponse(ern, name, description);
        }
    }
}
