package de.jensvogt.euclid.dto.esm;

/**
 * The number of objects a bucket holds.
 *
 * @param ern   the ERN of the bucket the count belongs to
 * @param count the number of objects in the bucket
 */
public record GetObjectCountResponse(String ern, long count) {

    /**
     * Creates a new instance of the Builder for constructing a GetObjectCountResponse object.
     *
     * @return a new Builder instance for constructing GetObjectCountResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetObjectCountResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN of the bucket the count belongs to.
         */
        private String ern;

        /**
         * The number of objects in the bucket.
         */
        private long count = 0;

        /**
         * Sets the ERN of the bucket the count belongs to.
         *
         * @param ern the ERN of the bucket the count belongs to
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the number of objects in the bucket.
         *
         * @param count the number of objects in the bucket
         * @return the builder instance
         */
        public Builder count(long count) {
            this.count = count;
            return this;
        }

        /**
         * Builds and returns a new instance of GetObjectCountResponse using the properties set on the Builder.
         *
         * @return a new GetObjectCountResponse instance.
         */
        public GetObjectCountResponse build() {
            return new GetObjectCountResponse(ern, count);
        }
    }
}
