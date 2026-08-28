package de.jensvogt.euclid.dto.ens;

/**
 * Response returned after successfully creating a subscription.
 *
 * @param ern       ERN of the newly created subscription
 * @param sourceErn ERN of the topic messages are published to
 * @param type      delivery protocol
 * @param targetErn ERN of the delivery target
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
         * The ERN of the newly created subscription.
         */
        private String ern;

        /**
         * The ERN of the topic messages are published to.
         */
        private String sourceErn;

        /**
         * The delivery protocol.
         */
        private String type;

        /**
         * The ERN of the delivery target.
         */
        private String targetErn;

        /**
         * Sets the ERN of the newly created subscription.
         *
         * @param ern the subscription ERN
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

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
         * @param type the delivery protocol
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
         * Builds and returns a new instance of SubscribeResponse using the properties set on the Builder.
         *
         * @return a new SubscribeResponse instance.
         */
        public SubscribeResponse build() {
            return new SubscribeResponse(ern, sourceErn, type, targetErn);
        }
    }
}
