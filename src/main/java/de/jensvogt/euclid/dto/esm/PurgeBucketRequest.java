package de.jensvogt.euclid.dto.esm;

/**
 * Request to purge a bucket, deleting its objects while leaving the bucket itself in place,
 * optionally restricted to a key prefix.
 *
 * @param ern    the ERN of the bucket to purge
 * @param prefix only objects whose key starts with this prefix are deleted
 * @param async  whether the server answers as soon as it has taken the work on rather than when it
 *               has finished, which is what a bucket too large to empty within one request wants
 */
public record PurgeBucketRequest(String ern, String prefix, boolean async) {

    /**
     * Creates a new instance of the Builder for constructing a PurgeBucketRequest object.
     *
     * @return a new Builder instance for constructing PurgeBucketRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link PurgeBucketRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN of the bucket to purge.
         */
        private String ern = "";

        /**
         * Only objects whose key starts with this prefix are deleted.
         */
        private String prefix = "";

        /**
         * Whether the server answers as soon as it has taken the work on.
         */
        private boolean async;

        /**
         * Sets the ERN of the bucket to purge.
         *
         * @param ern the ERN of the bucket to purge
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets only objects whose key starts with this prefix are deleted.
         *
         * @param prefix only objects whose key starts with this prefix are deleted
         * @return the builder instance
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Sets whether the server answers before it has finished removing the objects.
         *
         * @param async whether to purge in the background
         * @return the builder instance
         */
        public Builder async(boolean async) {
            this.async = async;
            return this;
        }

        /**
         * Builds and returns a new instance of PurgeBucketRequest using the properties set on the Builder.
         *
         * @return a new PurgeBucketRequest instance.
         */
        public PurgeBucketRequest build() {
            return new PurgeBucketRequest(ern, prefix, async);
        }
    }
}
