package de.jensvogt.euclid.dto.esm;

/**
 * Request to subscribe a target resource to a bucket's events, so an object created in that
 * bucket is announced to the target - a queue or a topic, depending on {@code type}.
 *
 * @param sourceErn the ERN of the bucket whose events are subscribed to
 * @param type      the target resource type, {@code "queue"} or {@code "topic"}
 * @param targetErn the ERN of the queue or topic the events are delivered to
 */
public record SubscribeRequest(String sourceErn, String type, String targetErn) {

    /**
     * Creates a new instance of the Builder for constructing a SubscribeRequest object.
     *
     * @return a new Builder instance for constructing SubscribeRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SubscribeRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

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
         * Builds and returns a new instance of SubscribeRequest using the properties set on the Builder.
         *
         * @return a new SubscribeRequest instance.
         */
        public SubscribeRequest build() {
            return new SubscribeRequest(sourceErn, type, targetErn);
        }
    }
}
