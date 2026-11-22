package de.jensvogt.euclid.dto.esm;

/**
 * Request to delete a bucket.
 *
 * @param ern     the ERN (Entity Resource Name) of the bucket to delete
 * @param async   whether the server answers as soon as it has taken the work on rather than when it
 *                has finished, which is what a bucket too large to empty within one request wants
 * @param ifEmpty whether the bucket is only to be removed when nothing is in it - HTTP 409 naming
 *                how many objects remain otherwise, and nothing deleted. What an automated caller
 *                means when it tidies up after itself, as opposed to an operator who has decided the
 *                contents go too: a provisioning run that drops a line from a manifest must not be
 *                one mistake away from destroying a bucket full of deliveries. Wins over
 *                {@code async}, since there is nothing to empty in the background
 */
public record DeleteBucketRequest(String ern, boolean async, boolean ifEmpty) {

    /**
     * Creates a new instance of the Builder for constructing a DeleteBucketRequest object.
     *
     * @return a new Builder instance for constructing DeleteBucketRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link DeleteBucketRequest} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN (Entity Resource Name) of the bucket to delete.
         */
        private String ern;

        /**
         * Whether the server answers as soon as it has taken the work on.
         */
        private boolean async;

        /**
         * Whether the bucket is only removed when nothing is in it.
         */
        private boolean ifEmpty;

        /**
         * Sets the ERN of the bucket to delete.
         *
         * @param ern the ERN of the bucket
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets whether the server answers before the bucket and its objects are gone.
         *
         * @param async   whether to delete in the background
         * @return the builder instance
         */
        public Builder async(boolean async) {
            this.async = async;
            return this;
        }

        /**
         * Sets whether the bucket is only removed when nothing is in it.
         *
         * @param ifEmpty true to have a bucket with objects in it refused with HTTP 409 rather than
         *                emptied
         * @return the builder instance
         */
        public Builder ifEmpty(boolean ifEmpty) {
            this.ifEmpty = ifEmpty;
            return this;
        }

        /**
         * Builds and returns a new instance of DeleteBucketRequest using the properties set on the Builder.
         *
         * @return a new DeleteBucketRequest instance populated with the ERN value.
         */
        public DeleteBucketRequest build() {
            return new DeleteBucketRequest(ern, async, ifEmpty);
        }
    }
}
