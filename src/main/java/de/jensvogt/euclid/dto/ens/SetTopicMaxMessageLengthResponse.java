package de.jensvogt.euclid.dto.ens;

/**
 * Response returned after a topic's message size limit has been changed.
 *
 * <p>One number rather than the queue equivalent's two: a topic's limit is always positive, so
 * there is no "carries none of its own" case for an effective figure to resolve.
 *
 * @param ern              the ERN of the topic
 * @param maxMessageLength the limit the topic now has, in bytes
 */
public record SetTopicMaxMessageLengthResponse(String ern, long maxMessageLength) {

    /**
     * Creates a new instance of the Builder for constructing a SetTopicMaxMessageLengthResponse object.
     *
     * @return a new Builder instance for constructing SetTopicMaxMessageLengthResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SetTopicMaxMessageLengthResponse} instances.
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
         * The limit the topic now has, in bytes.
         */
        private long maxMessageLength;

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
         * Sets the limit the topic now has.
         *
         * @param maxMessageLength the limit in bytes
         * @return the builder instance
         */
        public Builder maxMessageLength(long maxMessageLength) {
            this.maxMessageLength = maxMessageLength;
            return this;
        }

        /**
         * Builds and returns a new instance of SetTopicMaxMessageLengthResponse using the properties set on the Builder.
         *
         * @return a new SetTopicMaxMessageLengthResponse instance.
         */
        public SetTopicMaxMessageLengthResponse build() {
            return new SetTopicMaxMessageLengthResponse(ern, maxMessageLength);
        }
    }
}
