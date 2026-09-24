package de.jensvogt.euclid.dto.esm;

/**
 * A bucket and the priority its notifications now carry.
 *
 * @param ern      the ERN of the bucket
 * @param name     the name of the bucket
 * @param priority the priority as stored - upper case, or empty when it was cleared
 */
public record SetBucketPriorityResponse(String ern, String name, String priority) {

    /**
     * Creates a new instance of the Builder for constructing a SetBucketPriorityResponse object.
     *
     * @return a new Builder instance for constructing SetBucketPriorityResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SetBucketPriorityResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN of the bucket.
         */
        private String ern;

        /**
         * Sets ern.
         *
         * @param ern The ERN of the bucket.
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * The name of the bucket.
         */
        private String name;

        /**
         * Sets name.
         *
         * @param name The name of the bucket.
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * The priority as stored - upper case, or empty when it was cleared.
         */
        private String priority;

        /**
         * Sets priority.
         *
         * @param priority The priority as stored - upper case, or empty when it was cleared.
         * @return the builder instance
         */
        public Builder priority(String priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Builds and returns a new instance of SetBucketPriorityResponse using the properties set on the Builder.
         *
         * @return a new SetBucketPriorityResponse instance.
         */
        public SetBucketPriorityResponse build() {
            return new SetBucketPriorityResponse(ern, name, priority);
        }
    }
}
