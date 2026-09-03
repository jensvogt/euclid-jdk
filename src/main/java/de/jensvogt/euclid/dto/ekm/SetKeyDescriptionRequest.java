package de.jensvogt.euclid.dto.ekm;

/**
 * Request to replace what a key says it is for.
 *
 * @param ern         the ERN of the key to describe
 * @param description what the key is for; an empty string clears the description rather than
 *                    leaving it alone, since otherwise there would be no way to remove one
 */
public record SetKeyDescriptionRequest(String ern, String description) {

    /**
     * Creates a new instance of the Builder for constructing a SetKeyDescriptionRequest object.
     *
     * @return a new Builder instance for constructing SetKeyDescriptionRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SetKeyDescriptionRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN of the key to describe.
         */
        private String ern = "";

        /**
         * What the key is for; empty clears the description.
         */
        private String description = "";

        /**
         * Sets the ERN of the key to describe.
         *
         * @param ern the ERN of the key to describe
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets what the key is for.
         *
         * @param description what the key is for; {@code null} is treated as an empty description,
         *                    which clears it
         * @return the builder instance
         */
        public Builder description(String description) {
            this.description = description == null ? "" : description;
            return this;
        }

        /**
         * Builds and returns a new instance of SetKeyDescriptionRequest using the properties set on the Builder.
         *
         * @return a new SetKeyDescriptionRequest instance.
         */
        public SetKeyDescriptionRequest build() {
            return new SetKeyDescriptionRequest(ern, description);
        }
    }
}
