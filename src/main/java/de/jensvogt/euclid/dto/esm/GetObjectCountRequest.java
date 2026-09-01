package de.jensvogt.euclid.dto.esm;

/**
 * Request for the number of objects in a bucket, optionally restricted to a key prefix.
 *
 * @param ern    the ERN of the bucket whose objects are counted
 * @param prefix only objects whose key starts with this prefix are counted
 */
public record GetObjectCountRequest(String ern, String prefix) {

    /**
     * Creates a new instance of the Builder for constructing a GetObjectCountRequest object.
     *
     * @return a new Builder instance for constructing GetObjectCountRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetObjectCountRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN of the bucket whose objects are counted.
         */
        private String ern = "";

        /**
         * Only objects whose key starts with this prefix are counted.
         */
        private String prefix = "";

        /**
         * Sets the ERN of the bucket whose objects are counted.
         *
         * @param ern the ERN of the bucket whose objects are counted
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets only objects whose key starts with this prefix are counted.
         *
         * @param prefix only objects whose key starts with this prefix are counted
         * @return the builder instance
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Builds and returns a new instance of GetObjectCountRequest using the properties set on the Builder.
         *
         * @return a new GetObjectCountRequest instance.
         */
        public GetObjectCountRequest build() {
            return new GetObjectCountRequest(ern, prefix);
        }
    }
}
