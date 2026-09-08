package de.jensvogt.euclid.dto.esm;

import java.util.List;

/**
 * Request to delete several objects from a bucket in one call, naming them by key.
 *
 * <p>The server also accepts a prefix here instead of keys, but refuses both at once - naming keys
 * and a prefix asks two different questions and answering both would delete more than either. The
 * prefix form is reached through {@code purgeBucket(ern, prefix)}, so this record only carries keys.
 *
 * @param ern   the ERN (Entity Resource Name) of the bucket the objects are in
 * @param keys  the keys of the objects to delete
 * @param async whether the server answers as soon as it has started rather than when it has finished
 */
public record DeleteObjectsRequest(String ern, List<String> keys, boolean async) {

    /**
     * Creates a new instance of the Builder for constructing a DeleteObjectsRequest object.
     *
     * @return a new Builder instance for constructing DeleteObjectsRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link DeleteObjectsRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN (Entity Resource Name) of the bucket the objects are in.
         */
        private String ern;

        /**
         * The keys of the objects to delete.
         */
        private List<String> keys = List.of();

        /**
         * Whether the server answers as soon as it has started rather than when it has finished.
         */
        private boolean async;

        /**
         * Sets the ERN of the bucket the objects are in.
         *
         * @param ern the ERN of the bucket
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the keys of the objects to delete.
         *
         * @param keys the object keys
         * @return the builder instance
         */
        public Builder keys(List<String> keys) {
            this.keys = keys;
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
         * Builds and returns a new instance of DeleteObjectsRequest using the properties set on the Builder.
         *
         * @return a new DeleteObjectsRequest instance.
         */
        public DeleteObjectsRequest build() {
            return new DeleteObjectsRequest(ern, keys, async);
        }
    }
}
