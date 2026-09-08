package de.jensvogt.euclid.dto.esm;

/**
 * Response returned after a bucket has been renamed.
 *
 * <p>A rename changes the bucket's ERN as well as its name, so {@code ern} is the value later calls
 * have to use - the old one no longer answers to anything.
 *
 * @param name          the bucket's new name
 * @param ern           the bucket's new ERN (Entity Resource Name)
 * @param objects       the number of objects repointed at the new ERN
 * @param subscriptions the number of subscriptions repointed at the new ERN
 */
public record RenameBucketResponse(String name, String ern, long objects, long subscriptions) {

    /**
     * Creates a new instance of the Builder for constructing a RenameBucketResponse object.
     *
     * @return a new Builder instance for constructing RenameBucketResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link RenameBucketResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The bucket's new name.
         */
        private String name;

        /**
         * The bucket's new ERN (Entity Resource Name).
         */
        private String ern;

        /**
         * The number of objects repointed at the new ERN.
         */
        private long objects;

        /**
         * The number of subscriptions repointed at the new ERN.
         */
        private long subscriptions;

        /**
         * Sets the bucket's new name.
         *
         * @param name the new bucket name
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the bucket's new ERN.
         *
         * @param ern the new bucket ERN
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the number of objects repointed at the new ERN.
         *
         * @param objects the number of repointed objects
         * @return the builder instance
         */
        public Builder objects(long objects) {
            this.objects = objects;
            return this;
        }

        /**
         * Sets the number of subscriptions repointed at the new ERN.
         *
         * @param subscriptions the number of repointed subscriptions
         * @return the builder instance
         */
        public Builder subscriptions(long subscriptions) {
            this.subscriptions = subscriptions;
            return this;
        }

        /**
         * Builds and returns a new instance of RenameBucketResponse using the properties set on the Builder.
         *
         * @return a new RenameBucketResponse instance.
         */
        public RenameBucketResponse build() {
            return new RenameBucketResponse(name, ern, objects, subscriptions);
        }
    }
}
