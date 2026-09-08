package de.jensvogt.euclid.dto.esm;

/**
 * Response returned after a bulk delete.
 *
 * <p>{@code asked} and {@code objects} can differ: a key that names no object is not an error, it
 * simply is not there to delete, so a caller that cares compares the two. When the request asked
 * for it to run in the background, {@code async} is true and {@code objects} is how many the server
 * took on rather than how many it has already removed.
 *
 * @param ern     the ERN (Entity Resource Name) of the bucket
 * @param asked   the number of keys the request named
 * @param objects the number of objects actually deleted, or taken on when {@code async} is true
 * @param async   whether the server is still working through them in the background
 */
public record DeleteObjectsResponse(String ern, long asked, long objects, boolean async) {

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
         * Builds and returns a new instance of DeleteObjectsResponse using the properties set on the Builder.
         *
         * @return a new DeleteObjectsResponse instance.
         */
        public DeleteObjectsResponse build() {
            return new DeleteObjectsResponse(ern, asked, objects, async);
        }
    }
}
