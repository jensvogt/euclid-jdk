package de.jensvogt.euclid.dto.esm;

/**
 * Request to list every user-defined attribute of an object.
 *
 * @param ern the ERN of the object whose attributes are listed
 */
public record ListObjectAttributesRequest(String ern) {

    /**
     * Creates a new instance of the Builder for constructing a ListObjectAttributesRequest object.
     *
     * @return a new Builder instance for constructing ListObjectAttributesRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListObjectAttributesRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN of the object whose attributes are listed.
         */
        private String ern;

        /**
         * Sets the ERN of the object whose attributes are listed.
         *
         * @param ern the ERN of the object whose attributes are listed
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Builds and returns a new instance of ListObjectAttributesRequest using the properties set on the Builder.
         *
         * @return a new ListObjectAttributesRequest instance.
         */
        public ListObjectAttributesRequest build() {
            return new ListObjectAttributesRequest(ern);
        }
    }
}
