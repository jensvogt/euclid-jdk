package de.jensvogt.euclid.dto.ens;

/**
 * Request to delete all messages from a topic.
 * <p>
 * A topic holding a retention period's worth of messages can take longer to empty than the gateway
 * waits for an answer, so {@code async} lets the server answer as soon as it has taken the purge on.
 * Nothing is lost by not waiting: a purge interrupted halfway has removed part of the topic, and
 * asking again removes the rest.
 *
 * @param ern   topic ERN
 * @param async whether the server answers before it has finished removing the messages
 */
public record PurgeTopicRequest(String ern, boolean async) {

    /**
     * Creates a new instance of the Builder for constructing a PurgeTopicRequest object.
     *
     * @return a new Builder instance for constructing PurgeTopicRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link PurgeTopicRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The topic ERN.
         */
        private String ern;

        /**
         * Whether the purge runs in the background; off unless asked for, as the server has it.
         */
        private boolean async;

        /**
         * Sets the topic ERN.
         *
         * @param ern the topic ERN
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets whether the server answers before it has finished removing the messages.
         *
         * @param async whether the purge runs in the background
         * @return the builder instance
         */
        public Builder async(boolean async) {
            this.async = async;
            return this;
        }

        /**
         * Builds and returns a new instance of PurgeTopicRequest using the properties set on the Builder.
         *
         * @return a new PurgeTopicRequest instance.
         */
        public PurgeTopicRequest build() {
            return new PurgeTopicRequest(ern, async);
        }
    }
}
