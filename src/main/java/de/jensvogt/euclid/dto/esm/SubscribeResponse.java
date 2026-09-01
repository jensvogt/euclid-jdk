package de.jensvogt.euclid.dto.esm;

/**
 * The subscription created by a subscribe action.
 *
 * @param ern       the subscription's own ERN, which unsubscribe takes
 * @param sourceErn the ERN of the bucket whose events are subscribed to
 * @param type      the target resource type, {@code "queue"} or {@code "topic"}
 * @param targetErn the ERN of the queue or topic the events are delivered to
 */
public record SubscribeResponse(String ern, String sourceErn, String type, String targetErn) {

    /**
     * Creates a new instance of the Builder for constructing a SubscribeResponse object.
     *
     * @return a new Builder instance for constructing SubscribeResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SubscribeResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The subscription's own ERN, which unsubscribe takes.
         */
        private String ern;

        /**
         * The ERN of the bucket whose events are subscribed to.
         */
        private String sourceErn;

        /**
         * The target resource type, {@code "queue"} or {@code "topic"}.
         */
        private String type;

        /**
         * The ERN of the queue or topic the events are delivered to.
         */
        private String targetErn;

        /**
         * Sets the subscription's own ERN, which unsubscribe takes.
         *
         * @param ern the subscription's own ERN, which unsubscribe takes
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the ERN of the bucket whose events are subscribed to.
         *
         * @param sourceErn the ERN of the bucket whose events are subscribed to
         * @return the builder instance
         */
        public Builder sourceErn(String sourceErn) {
            this.sourceErn = sourceErn;
            return this;
        }

        /**
         * Sets the target resource type, {@code "queue"} or {@code "topic"}.
         *
         * @param type the target resource type, {@code "queue"} or {@code "topic"}
         * @return the builder instance
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the ERN of the queue or topic the events are delivered to.
         *
         * @param targetErn the ERN of the queue or topic the events are delivered to
         * @return the builder instance
         */
        public Builder targetErn(String targetErn) {
            this.targetErn = targetErn;
            return this;
        }

        /**
         * Builds and returns a new instance of SubscribeResponse using the properties set on the Builder.
         *
         * @return a new SubscribeResponse instance.
         */
        public SubscribeResponse build() {
            return new SubscribeResponse(ern, sourceErn, type, targetErn);
        }
    }
}
