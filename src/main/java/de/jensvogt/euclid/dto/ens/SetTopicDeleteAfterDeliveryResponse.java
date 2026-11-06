package de.jensvogt.euclid.dto.ens;

/**
 * Response returned after a topic has been told whether to delete what it has delivered.
 *
 * @param ern                 the ERN of the topic
 * @param deleteAfterDelivery the setting the topic now has: true if it deletes each message once
 *                            every subscription has it, false if it keeps it for the retention
 *                            period
 */
public record SetTopicDeleteAfterDeliveryResponse(String ern, boolean deleteAfterDelivery) {

    /**
     * Creates a new instance of the Builder for constructing a SetTopicDeleteAfterDeliveryResponse object.
     *
     * @return a new Builder instance for constructing SetTopicDeleteAfterDeliveryResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SetTopicDeleteAfterDeliveryResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN of the topic.
         */
        private String ern;

        /**
         * Whether the topic now deletes each message once every subscription has it.
         */
        private boolean deleteAfterDelivery;

        /**
         * Sets the ERN of the topic.
         *
         * @param ern the topic ERN
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets whether the topic now deletes each message once every subscription has it.
         *
         * @param deleteAfterDelivery the setting the topic now has
         * @return the builder instance
         */
        public Builder deleteAfterDelivery(boolean deleteAfterDelivery) {
            this.deleteAfterDelivery = deleteAfterDelivery;
            return this;
        }

        /**
         * Builds and returns a new instance of SetTopicDeleteAfterDeliveryResponse using the properties set on the Builder.
         *
         * @return a new SetTopicDeleteAfterDeliveryResponse instance.
         */
        public SetTopicDeleteAfterDeliveryResponse build() {
            return new SetTopicDeleteAfterDeliveryResponse(ern, deleteAfterDelivery);
        }
    }
}
