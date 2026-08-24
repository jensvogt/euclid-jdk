package de.jensvogt.euclid.dto.esm;

/**
 * Request to delete an object.
 *
 * @param ern the ERN (Entity Resource Name) of the object to delete
 */
public record DeleteObjectRequest(String ern) {

    /**
     * Creates a new instance of the Builder for constructing a DeleteObjectRequest object.
     *
     * @return a new Builder instance for constructing DeleteObjectRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link DeleteObjectRequest} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN (Entity Resource Name) of the object to delete.
         */
        private String ern;

        /**
         * Sets the ERN of the object to delete.
         *
         * @param ern the ERN of the object
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Builds and returns a new instance of DeleteObjectRequest using the properties set on the Builder.
         *
         * @return a new DeleteObjectRequest instance populated with the ERN value.
         */
        public DeleteObjectRequest build() {
            return new DeleteObjectRequest(ern);
        }
    }
}
