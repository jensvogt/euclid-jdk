package de.jensvogt.euclid.dto.eqs;

/**
 * Request for one queue, by ERN or by name.
 * <p>
 * Exactly one of the two is sent. A name is resolved in the caller's own account and namespace, so
 * it means "my queue of that name"; an ERN names one queue in the installation.
 *
 * @param ern  the ERN uniquely identifying the queue, or null when it is named instead
 * @param name the queue's name, used when no ERN is given
 */
public record GetQueueRequest(String ern, String name) {

    /**
     * Creates a new instance of the Builder for constructing a GetQueueRequest object.
     *
     * @return a new Builder instance for constructing GetQueueRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetQueueRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN uniquely identifying the queue.
         */
        private String ern;

        /**
         * The queue's name.
         */
        private String name;

        /**
         * Sets the ERN of the queue.
         *
         * @param ern the ERN uniquely identifying the queue
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the name of the queue.
         *
         * @param name the queue's name, resolved in the caller's own account and namespace
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Builds and returns a new instance of GetQueueRequest using the properties set on the Builder.
         *
         * @return a new GetQueueRequest instance.
         */
        public GetQueueRequest build() {
            return new GetQueueRequest(ern, name);
        }
    }
}
