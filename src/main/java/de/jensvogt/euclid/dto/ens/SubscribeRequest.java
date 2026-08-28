package de.jensvogt.euclid.dto.ens;

/**
 * Request to subscribe a target resource to a topic.
 *
 * @param sourceErn ERN of the topic messages are published to
 * @param type      delivery protocol; only "SQS" is currently supported
 * @param targetErn ERN of the delivery target; for type "SQS", an EQS queue ERN
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
         * The ERN of the topic messages are published to.
         */
        private String sourceErn;

        /**
         * The delivery protocol.
         */
        private String type = "SQS";

        /**
         * The ERN of the delivery target.
         */
        private String targetErn;

        /**
         * Sets the ERN of the topic messages are published to.
         *
         * @param sourceErn the topic ERN
         * @return the builder instance
         */
        public Builder sourceErn(String sourceErn) {
            this.sourceErn = sourceErn;
            return this;
        }

        /**
         * Sets the delivery protocol.
         *
         * @param type the delivery protocol; only "SQS" is currently supported
         * @return the builder instance
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the ERN of the delivery target.
         *
         * @param targetErn the target ERN
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
