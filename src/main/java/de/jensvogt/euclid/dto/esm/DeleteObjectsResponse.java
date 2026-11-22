package de.jensvogt.euclid.dto.esm;

/**
 * Response returned after a bulk delete.
 *
 * <p>{@code asked} and {@code objects} can differ: a key that names no object is not an error, it
 * simply is not there to delete, so a caller that cares compares the two. When the request asked
 * for it to run in the background, {@code async} is true and {@code objects} is how many the server
 * took on rather than how many it has already removed.
 *
 * <p>A delete by prefix has no {@code asked} - it named no keys - and reports the prefix instead;
 * taken on in the background it also names the job doing it, which outlives the instance that took
 * it on.
 *
 * @param ern     the ERN (Entity Resource Name) of the bucket
 * @param asked   the number of keys the request named; 0 for a delete by prefix
 * @param objects the number of objects actually deleted, or taken on when {@code async} is true
 * @param async   whether the server is still working through them in the background
 * @param prefix  the prefix deleted under, for a delete by prefix; null for a delete by key
 * @param jobId   the background job doing the work, for a delete by prefix taken on with
 *                {@code async}; empty otherwise, since a delete by key in the background is not a
 *                recorded job
 */
public record DeleteObjectsResponse(String ern, long asked, long objects, boolean async, String prefix,
                                    String jobId) {

    /**
     * Creates a new instance of the Builder for constructing a DeleteObjectsResponse object.
     *
     * @return a new Builder instance for constructing DeleteObjectsResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link DeleteObjectsResponse} instances.
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
         * The number of keys the request named.
         */
        private long asked;

        /**
         * The number of objects actually deleted, or taken on when async.
         */
        private long objects;

        /**
         * Whether the server is still working through them in the background.
         */
        private boolean async;

        /**
         * The prefix deleted under, for a delete by prefix.
         */
        private String prefix;

        /**
         * The background job doing the work, empty when there is none.
         */
        private String jobId = "";

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
         * Sets the number of keys the request named.
         *
         * @param asked the number of keys asked for
         * @return the builder instance
         */
        public Builder asked(long asked) {
            this.asked = asked;
            return this;
        }

        /**
         * Sets the number of objects actually deleted.
         *
         * @param objects the number of deleted objects
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
         * Sets the prefix deleted under.
         *
         * @param prefix the prefix, or null for a delete by key
         * @return the builder instance
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Sets the background job doing the work.
         *
         * @param jobId the job's ID, empty when there is none
         * @return the builder instance
         */
        public Builder jobId(String jobId) {
            this.jobId = jobId;
            return this;
        }

        /**
         * Builds and returns a new instance of DeleteObjectsResponse using the properties set on the Builder.
         *
         * @return a new DeleteObjectsResponse instance.
         */
        public DeleteObjectsResponse build() {
            return new DeleteObjectsResponse(ern, asked, objects, async, prefix, jobId);
        }
    }
}
