package de.jensvogt.euclid.dto.ekm;

/**
 * Request to delete a tag from a key.
 *
 * @param ern the ERN of the key the tag belongs to
 * @param key the tag key to delete
 */
public record DeleteKeyTagRequest(String ern, String key) {

    /**
     * Creates a new instance of the Builder for constructing a DeleteKeyTagRequest object.
     *
     * @return a new Builder instance for constructing DeleteKeyTagRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link DeleteKeyTagRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN of the key the tag belongs to.
         */
        private String ern;

        /**
         * The tag key to delete.
         */
        private String key;

        /**
         * Sets the ERN of the key the tag belongs to.
         *
         * @param ern the ERN of the key the tag belongs to
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the tag key to delete.
         *
         * @param key the tag key to delete
         * @return the builder instance
         */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /**
         * Builds and returns a new instance of DeleteKeyTagRequest using the properties set on the Builder.
         *
         * @return a new DeleteKeyTagRequest instance.
         */
        public DeleteKeyTagRequest build() {
            return new DeleteKeyTagRequest(ern, key);
        }
    }
}
