package de.jensvogt.euclid.dto.ens;

/**
 * Request to set whether a topic deletes each message once it has been handed to every
 * subscription, instead of keeping it for its retention period.
 *
 * <p>Takes effect for messages published afterwards. The ones the topic already holds were
 * delivered under the old rule and stay until their retention runs out: turning this on is not a
 * purge, and {@code purge-topic} is what says so.
 *
 * @param ern                 the ERN of the topic
 * @param deleteAfterDelivery true to delete each message once it has been handed to every
 *                            subscription; false to keep it for the retention period, which is
 *                            where every topic starts out. A message on a topic with no
 *                            subscription is kept either way - it has been handed to nobody
 */
public record SetTopicDeleteAfterDeliveryRequest(String ern, boolean deleteAfterDelivery) {

    /**
     * Creates a new instance of the Builder for constructing a SetTopicDeleteAfterDeliveryRequest object.
     *
     * @return a new Builder instance for constructing SetTopicDeleteAfterDeliveryRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SetTopicDeleteAfterDeliveryRequest} instances.
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
        private String ern = "";

        /**
         * Whether a message is deleted once every subscription has it; off unless set.
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
         * Sets whether a message is deleted once it has been handed to every subscription.
         *
         * @param deleteAfterDelivery true to delete after delivery, false to keep for the retention period
         * @return the builder instance
         */
        public Builder deleteAfterDelivery(boolean deleteAfterDelivery) {
            this.deleteAfterDelivery = deleteAfterDelivery;
            return this;
        }

        /**
         * Builds and returns a new instance of SetTopicDeleteAfterDeliveryRequest using the properties set on the Builder.
         *
         * @return a new SetTopicDeleteAfterDeliveryRequest instance.
         */
        public SetTopicDeleteAfterDeliveryRequest build() {
            return new SetTopicDeleteAfterDeliveryRequest(ern, deleteAfterDelivery);
        }
    }
}
