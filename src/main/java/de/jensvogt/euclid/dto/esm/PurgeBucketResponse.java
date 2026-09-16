package de.jensvogt.euclid.dto.esm;

/**
 * Response returned after successfully purging a bucket.
 *
 * <p>When the request asked for it to run in the background, {@code async} is true and {@code count}
 * is how many objects the bucket held when the purge was taken on rather than how many have gone.
 * {@code jobId} names the job that is doing it, which survives the instance that started it being
 * stopped - another picks the job up and carries on.
 *
 * @param ern   the ERN (Entity Resource Name) of the purged bucket
 * @param count the number of objects deleted, or taken on when {@code async} is true
 * @param jobId the background job doing the work, empty unless {@code async} is true
 * @param async whether the server is still working through them in the background
 */
public record PurgeBucketResponse(String ern, long count, String jobId, boolean async) {

    /**
     * Creates a new instance of the Builder for constructing a PurgeBucketResponse object.
     *
     * @return a new Builder instance for constructing PurgeBucketResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link PurgeBucketResponse} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN (Entity Resource Name) of the purged bucket.
         */
        private String ern;

        /**
         * The number of objects deleted from the bucket.
         */
        private long count;

        /**
         * The background job doing the work, empty unless the purge is asynchronous.
         */
        private String jobId = "";

        /**
         * Whether the server is still working through the objects in the background.
         */
        private boolean async;

        /**
         * Sets the ERN of the purged bucket.
         *
         * @param ern the ERN of the bucket
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the number of objects deleted from the bucket.
         *
         * @param count the number of deleted objects
         * @return the builder instance
         */
        public Builder count(long count) {
            this.count = count;
            return this;
        }

        /**
         * Sets the background job doing the work.
         *
         * @param jobId the job id
         * @return the builder instance
         */
        public Builder jobId(String jobId) {
            this.jobId = jobId;
            return this;
        }

        /**
         * Sets whether the server is still working through the objects in the background.
         *
         * @param async whether the purge is running in the background
         * @return the builder instance
         */
        public Builder async(boolean async) {
            this.async = async;
            return this;
        }

        /**
         * Builds and returns a new instance of PurgeBucketResponse using the properties set on the Builder.
         *
         * @return a new PurgeBucketResponse instance populated with the ERN and count values.
         */
        public PurgeBucketResponse build() {
            return new PurgeBucketResponse(ern, count, jobId, async);
        }
    }
}
