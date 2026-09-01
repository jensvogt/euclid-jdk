package de.jensvogt.euclid.dto.eqs;

/**
 * Response carrying the ERN a queue name resolves to.
 *
 * @param name the name of the queue
 * @param ern  the ERN (Entity Resource Name) of the queue
 */
public record GetQueueErnResponse(String name, String ern) {

    /**
     * Creates a new instance of the Builder for constructing a GetQueueErnResponse object.
     *
     * @return a new Builder instance for constructing GetQueueErnResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetQueueErnResponse} instances.
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
         * The ERN (Entity Resource Name) of the queue.
         */
        private String ern;

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
         * Sets the ERN (Entity Resource Name) of the queue.
         *
         * @param ern the ERN (Entity Resource Name) of the queue
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Builds and returns a new instance of GetQueueErnResponse using the properties set on the Builder.
         *
         * @return a new GetQueueErnResponse instance.
         */
        public GetQueueErnResponse build() {
            return new GetQueueErnResponse(name, ern);
        }
    }
}
