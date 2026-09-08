package de.jensvogt.euclid.dto.esm;

/**
 * Request to mark a bucket as euclid's own plumbing, or to stop doing so.
 *
 * <p>An internal bucket is left out of an ordinary listing, so a listing shows what a person would
 * recognise. Separate from create-bucket because the bucket this exists for usually predates
 * anybody thinking about it, and reversible for the same reason.
 *
 * @param ern      the ERN (Entity Resource Name) of the bucket
 * @param internal whether the bucket is euclid's own
 */
public record SetBucketInternalRequest(String ern, boolean internal) {

    /**
     * Creates a new instance of the Builder for constructing a SetBucketInternalRequest object.
     *
     * @return a new Builder instance for constructing SetBucketInternalRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SetBucketInternalRequest} instances.
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
         * Whether the bucket is euclid's own. Defaults to true, matching the server, which reads a
         * missing flag as "hide this bucket" - the reason the action is usually reached for.
         */
        private boolean internal = true;

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
         * Sets whether the bucket is euclid's own.
         *
         * @param internal true to hide the bucket from ordinary listings, false to show it again
         * @return the builder instance
         */
        public Builder internal(boolean internal) {
            this.internal = internal;
            return this;
        }

        /**
         * Builds and returns a new instance of SetBucketInternalRequest using the properties set on the Builder.
         *
         * @return a new SetBucketInternalRequest instance populated with the ERN and internal values.
         */
        public SetBucketInternalRequest build() {
            return new SetBucketInternalRequest(ern, internal);
        }
    }
}
