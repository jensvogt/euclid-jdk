package de.jensvogt.euclid.dto.esm;

/**
 * Response returned after a bucket's objects have been re-announced.
 *
 * <p>When the request asked for it to run in the background, {@code async} is true and
 * {@code objects} is how many the server found to work through rather than how many it has already
 * announced.
 *
 * @param ern        the ERN (Entity Resource Name) of the bucket
 * @param bucketName the name of the bucket
 * @param prefix     the key prefix the re-announcement was restricted to, empty for the whole bucket
 * @param objects    the number of objects announced, or started on when {@code async} is true
 * @param async      whether the server is still working through them in the background
 */
public record TouchObjectResponse(String ern, String bucketName, String prefix, long objects, boolean async) {

    /**
     * Creates a new instance of the Builder for constructing a TouchObjectResponse object.
     *
     * @return a new Builder instance for constructing TouchObjectResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link TouchObjectResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN (Entity Resource Name) of the bucket.
         */
        private String ern;

        /**
         * The name of the bucket.
         */
        private String bucketName;

        /**
         * The key prefix the re-announcement was restricted to.
         */
        private String prefix;

        /**
         * The number of objects announced, or started on when async.
         */
        private long objects;

        /**
         * Whether the server is still working through them in the background.
         */
        private boolean async;

        /**
         * Sets the ERN of the bucket.
         *
         * @param ern the ERN of the bucket
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the name of the bucket.
         *
         * @param bucketName the bucket name
         * @return the builder instance
         */
        public Builder bucketName(String bucketName) {
            this.bucketName = bucketName;
            return this;
        }

        /**
         * Sets the key prefix the re-announcement was restricted to.
         *
         * @param prefix the key prefix
         * @return the builder instance
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Sets the number of objects announced.
         *
         * @param objects the number of objects
         * @return the builder instance
         */
        public Builder objects(long objects) {
            this.objects = objects;
            return this;
        }

        /**
         * Sets whether the server is still working through them in the background.
         *
         * @param async true when the server answered before finishing
         * @return the builder instance
         */
        public Builder async(boolean async) {
            this.async = async;
            return this;
        }

        /**
         * Builds and returns a new instance of TouchObjectResponse using the properties set on the Builder.
         *
         * @return a new TouchObjectResponse instance.
         */
        public TouchObjectResponse build() {
            return new TouchObjectResponse(ern, bucketName, prefix, objects, async);
        }
    }
}
