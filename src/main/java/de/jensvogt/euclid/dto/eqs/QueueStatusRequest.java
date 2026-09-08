package de.jensvogt.euclid.dto.eqs;

/**
 * Request to stop or start a queue.
 *
 * <p>One record for both actions, which differ only in the {@code stop-queue}/{@code start-queue}
 * action header they are sent under - the same split
 * {@link de.jensvogt.euclid.dto.esm.ObjectAttributeRequest} makes in ESM.
 *
 * @param ern the ERN (Entity Resource Name) of the queue to stop or start
 */
public record QueueStatusRequest(String ern) {

    /**
     * Creates a new instance of the Builder for constructing a QueueStatusRequest object.
     *
     * @return a new Builder instance for constructing QueueStatusRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link QueueStatusRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN (Entity Resource Name) of the queue to stop or start.
         */
        private String ern;

        /**
         * Sets the ERN of the queue to stop or start.
         *
         * @param ern the ERN of the queue
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Builds and returns a new instance of QueueStatusRequest using the properties set on the Builder.
         *
         * @return a new QueueStatusRequest instance populated with the ERN value.
         */
        public QueueStatusRequest build() {
            return new QueueStatusRequest(ern);
        }
    }
}
