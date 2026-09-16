package de.jensvogt.euclid.dto.esm;

/**
 * What a background bucket deletion took on.
 *
 * <p>Only a deletion asked to run in the background answers with anything at all - a bucket deleted
 * inline is simply gone, and the call returns nothing. So {@code async} is true whenever this is
 * worth reading, {@code count} is how many objects the bucket held when the work was taken on, and
 * {@code jobId} names the job doing it. That job outlives the instance that started it - one
 * stopped by the autoscaler, or lost to a crash, leaves a job another instance picks up.
 *
 * <p>The bucket itself goes when the emptying finishes, so it stays listed, and still deletable,
 * until it is genuinely gone.
 *
 * @param ern   the ERN (Entity Resource Name) of the bucket being deleted
 * @param count the number of objects the bucket held when the deletion was taken on
 * @param jobId the background job doing the work
 * @param async whether the server is still working through it in the background
 */
public record DeleteBucketResponse(String ern, long count, String jobId, boolean async) {

    /**
     * Creates a new instance of the Builder for constructing a DeleteBucketResponse object.
     *
     * @return a new Builder instance for constructing DeleteBucketResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link DeleteBucketResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN of the bucket being deleted.
         */
        private String ern;

        /**
         * The number of objects the bucket held when the deletion was taken on.
         */
        private long count;

        /**
         * The background job doing the work.
         */
        private String jobId = "";

        /**
         * Whether the server is still working through it in the background.
         */
        private boolean async;

        /**
         * Sets the ERN of the bucket being deleted.
         *
         * @param ern the ERN of the bucket
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the number of objects the bucket held.
         *
         * @param count the number of objects
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
         * Sets whether the server is still working through it in the background.
         *
         * @param async whether the deletion is running in the background
         * @return the builder instance
         */
        public Builder async(boolean async) {
            this.async = async;
            return this;
        }

        /**
         * Builds and returns a new instance of DeleteBucketResponse using the properties set on the Builder.
         *
         * @return a new DeleteBucketResponse instance.
         */
        public DeleteBucketResponse build() {
            return new DeleteBucketResponse(ern, count, jobId, async);
        }
    }
}
