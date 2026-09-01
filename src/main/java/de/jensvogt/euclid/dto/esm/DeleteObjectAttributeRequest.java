package de.jensvogt.euclid.dto.esm;

/**
 * Request to delete a user-defined attribute from an object.
 *
 * @param ern  the ERN of the object carrying the attribute
 * @param name the name of the attribute to delete
 */
public record DeleteObjectAttributeRequest(String ern, String name) {

    /**
     * Creates a new instance of the Builder for constructing a DeleteObjectAttributeRequest object.
     *
     * @return a new Builder instance for constructing DeleteObjectAttributeRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link DeleteObjectAttributeRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN of the object carrying the attribute.
         */
        private String ern;

        /**
         * The name of the attribute to delete.
         */
        private String name;

        /**
         * Sets the ERN of the object carrying the attribute.
         *
         * @param ern the ERN of the object carrying the attribute
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the name of the attribute to delete.
         *
         * @param name the name of the attribute to delete
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Builds and returns a new instance of DeleteObjectAttributeRequest using the properties set on the Builder.
         *
         * @return a new DeleteObjectAttributeRequest instance.
         */
        public DeleteObjectAttributeRequest build() {
            return new DeleteObjectAttributeRequest(ern, name);
        }
    }
}
