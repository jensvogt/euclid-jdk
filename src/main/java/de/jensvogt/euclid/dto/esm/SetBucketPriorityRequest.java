package de.jensvogt.euclid.dto.esm;

/**
 * Request to set the priority the notifications a bucket sends are given.
 *
 * <p>The bucket does nothing with it. A bucket is not consumed from and has no queue of its own, so
 * there is nothing here for a priority to mean - it exists to be handed on, to the messages a
 * subscription of this bucket turns an object event into. "Everything that lands in this bucket is
 * urgent" is the statement it makes, and the queue on the other side of the subscription is where
 * that statement finally has an effect.
 *
 * <p><b>Which priority wins.</b> Four statements can be in play about one message, least specific
 * first: the target queue's own default, this, the priority in the object's own system attributes,
 * and a priority a message already had when a topic passed it on. The object's beats the bucket's
 * because it is the narrower claim - which is what lets a bucket set a floor without taking away the
 * ability to say more about a particular object.
 *
 * <p><b>Empty is not MEDIUM.</b> An empty priority clears it, and is the only way back to letting
 * the queue decide. A bucket that says nothing leaves a queue created with {@code LOW} delivering at
 * {@code LOW}, where a bucket saying {@code MEDIUM} would override it.
 *
 * @param ern      the ERN (Entity Resource Name) of the bucket
 * @param priority {@code "LOW"}, {@code "MEDIUM"} or {@code "HIGH"}, or empty to clear it. Case is
 *                 not significant; anything else is refused rather than ignored
 */
public record SetBucketPriorityRequest(String ern, String priority) {

    /**
     * Creates a new instance of the Builder for constructing a SetBucketPriorityRequest object.
     *
     * @return a new Builder instance for constructing SetBucketPriorityRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SetBucketPriorityRequest} instances.
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
         * Sets ern.
         *
         * @param ern The ERN (Entity Resource Name) of the bucket.
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * The priority: {@code "LOW"}, {@code "MEDIUM"} or {@code "HIGH"}, or empty to clear it.
         */
        private String priority;

        /**
         * Sets priority.
         *
         * @param priority The priority: {@code "LOW"}, {@code "MEDIUM"} or {@code "HIGH"}, or empty to clear it.
         * @return the builder instance
         */
        public Builder priority(String priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Builds and returns a new instance of SetBucketPriorityRequest using the properties set on the Builder.
         *
         * @return a new SetBucketPriorityRequest instance.
         */
        public SetBucketPriorityRequest build() {
            return new SetBucketPriorityRequest(ern, priority);
        }
    }
}
