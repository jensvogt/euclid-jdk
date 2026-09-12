package de.jensvogt.euclid.dto.ens;

/**
 * Request to change the largest message a topic accepts.
 *
 * <p>Applies to what is published after it: a message already in the topic is not re-checked and is
 * not removed by lowering this, because it was accepted under the rule that was in force when it
 * arrived.
 *
 * <p>Note that this is stricter than the queue equivalent. Zero is refused here rather than read as
 * "follow the default": a topic that accepts nothing is a mistake rather than a configuration, and
 * {@link de.jensvogt.euclid.module.ens.EuclidEns#stopTopic} is what says "take nothing for now" -
 * reversibly, and without losing what arrives meanwhile.
 *
 * @param ern              the ERN of the topic
 * @param maxMessageLength the largest message the topic accepts from now on, in bytes. Has to be
 *                         positive; zero or negative is refused with HTTP 400
 */
public record SetTopicMaxMessageLengthRequest(String ern, long maxMessageLength) {

    /**
     * Creates a new instance of the Builder for constructing a SetTopicMaxMessageLengthRequest object.
     *
     * @return a new Builder instance for constructing SetTopicMaxMessageLengthRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SetTopicMaxMessageLengthRequest} instances.
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
         * The largest message the topic accepts, in bytes.
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
         * Sets the largest message the topic accepts.
         *
         * @param maxMessageLength the limit in bytes, which has to be positive
         * @return the builder instance
         */
        public Builder maxMessageLength(long maxMessageLength) {
            this.maxMessageLength = maxMessageLength;
            return this;
        }

        /**
         * Builds and returns a new instance of SetTopicMaxMessageLengthRequest using the properties set on the Builder.
         *
         * @return a new SetTopicMaxMessageLengthRequest instance.
         */
        public SetTopicMaxMessageLengthRequest build() {
            return new SetTopicMaxMessageLengthRequest(ern, maxMessageLength);
        }
    }
}
