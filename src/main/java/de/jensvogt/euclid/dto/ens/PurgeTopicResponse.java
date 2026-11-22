package de.jensvogt.euclid.dto.ens;

/**
 * Response returned after purging a topic.
 *
 * <p>When the request asked for it to run in the background, {@code async} is true, the server
 * answered HTTP 202 and {@code messages} is how many messages the topic held when the purge was
 * taken on - read then rather than afterwards, because by the time anyone looks the figure is on its
 * way to zero and the one that means anything is the one from the start. A purge done inline has
 * nothing to report but that it is done, so {@code messages} is zero there.
 *
 * @param ern      the ERN of the purged topic
 * @param async    whether the server is still removing the messages in the background
 * @param messages the messages the topic held when an asynchronous purge was accepted; zero otherwise
 */
public record PurgeTopicResponse(String ern, boolean async, long messages) {

    /**
     * Creates a new instance of the Builder for constructing a PurgeTopicResponse object.
     *
     * @return a new Builder instance for constructing PurgeTopicResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link PurgeTopicResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN of the purged topic.
         */
        private String ern;

        /**
         * Whether the server is still removing the messages in the background.
         */
        private boolean async;

        /**
         * The messages the topic held when an asynchronous purge was accepted.
         */
        private long messages;

        /**
         * Sets the ERN of the purged topic.
         *
         * @param ern the ERN of the topic
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets whether the server is still removing the messages in the background.
         *
         * @param async whether the purge is running in the background
         * @return the builder instance
         */
        public Builder async(boolean async) {
            this.async = async;
            return this;
        }

        /**
         * Sets the messages the topic held when an asynchronous purge was accepted.
         *
         * @param messages the number of messages taken on
         * @return the builder instance
         */
        public Builder messages(long messages) {
            this.messages = messages;
            return this;
        }

        /**
         * Builds and returns a new instance of PurgeTopicResponse using the properties set on the Builder.
         *
         * @return a new PurgeTopicResponse instance.
         */
        public PurgeTopicResponse build() {
            return new PurgeTopicResponse(ern, async, messages);
        }
    }
}
