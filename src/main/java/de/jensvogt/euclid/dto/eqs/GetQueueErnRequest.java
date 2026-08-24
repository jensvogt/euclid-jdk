package de.jensvogt.euclid.dto.eqs;

/**
 * Request to retrieve the ERN (Entity Resource Name) of a queue.
 *
 * @param name the name of the queue
 */
public record GetQueueErnRequest(String name) {

    /**
     * Creates a new instance of the Builder for constructing a GetQueueErnRequest object.
     *
     * @return a new Builder instance for constructing GetQueueErnRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetQueueErnRequest} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The name of the queue.
         */
        private String name;

        /**
         * Sets the name of the queue.
         *
         * @param name the name of the queue
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Builds and returns a new instance of GetQueueErnRequest using the properties set on the Builder.
         *
         * @return a new GetQueueErnRequest instance populated with the name value.
         */
        public GetQueueErnRequest build() {
            return new GetQueueErnRequest(name);
        }
    }
}
