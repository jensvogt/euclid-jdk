package de.jensvogt.euclid.dto.esm;

/**
 * Request to re-announce objects that are already in a bucket, so a listener that missed their
 * creation events hears about them.
 *
 * <p>Nothing about the objects changes - not a byte, not their modified time. "Touch" here means
 * what it does to listeners, not what it does to storage.
 *
 * @param ern    the ERN (Entity Resource Name) of the bucket whose objects are re-announced
 * @param prefix only re-announce objects whose key starts with this, or empty for the whole bucket
 * @param async  whether the server answers as soon as it has started rather than when it has
 *               finished, which is what a bucket of any size wants
 */
public record TouchObjectRequest(String ern, String prefix, boolean async) {

    /**
     * Creates a new instance of the Builder for constructing a TouchObjectRequest object.
     *
     * @return a new Builder instance for constructing TouchObjectRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link TouchObjectRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN (Entity Resource Name) of the bucket whose objects are re-announced.
         */
        private String ern;

        /**
         * Only re-announce objects whose key starts with this, empty for the whole bucket.
         */
        private String prefix = "";

        /**
         * Whether the server answers as soon as it has started rather than when it has finished.
         */
        private boolean async;

        /**
         * Sets the ERN of the bucket whose objects are re-announced.
         *
         * @param ern the ERN of the bucket
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the key prefix to restrict the re-announcement to.
         *
         * @param prefix the key prefix, or empty for the whole bucket
         * @return the builder instance
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Sets whether the server answers as soon as it has started.
         *
         * @param async true to have the server work in the background
         * @return the builder instance
         */
        public Builder async(boolean async) {
            this.async = async;
            return this;
        }

        /**
         * Builds and returns a new instance of TouchObjectRequest using the properties set on the Builder.
         *
         * @return a new TouchObjectRequest instance.
         */
        public TouchObjectRequest build() {
            return new TouchObjectRequest(ern, prefix, async);
        }
    }
}
