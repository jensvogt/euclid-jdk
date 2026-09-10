package de.jensvogt.euclid.dto.ens;

/**
 * Response returned after a topic has been stopped or started.
 *
 * <p>A stopped topic still accepts and stores what is published to it - it simply does not fan it
 * out. That is the point: a subscriber being redeployed, or a downstream system taken down for the
 * evening, is a reason to hold delivery rather than to lose what arrives meanwhile. Starting the
 * topic again hands the backlog over, oldest first, before this answers.
 *
 * @param ern      the ERN of the topic
 * @param status   the topic's status afterwards, {@code "RUNNING"} or {@code "STOPPED"}
 * @param released how many held messages were delivered to the topic's subscriptions as part of
 *                 starting it. Always zero for a stop, and zero for a start of a topic that was
 *                 already running or held nothing. A start interrupted halfway through has
 *                 delivered a prefix of the backlog rather than none of it, so running it again
 *                 picks up where it stopped
 */
public record TopicStatusResponse(String ern, String status, long released) {

    /**
     * Creates a new instance of the Builder for constructing a TopicStatusResponse object.
     *
     * @return a new Builder instance for constructing TopicStatusResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link TopicStatusResponse} instances.
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
         * The topic's status afterwards.
         */
        private String status;

        /**
         * How many held messages were delivered as part of starting it.
         */
        private long released;

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
         * Sets the topic's status afterwards.
         *
         * @param status {@code "RUNNING"} or {@code "STOPPED"}
         * @return the builder instance
         */
        public Builder status(String status) {
            this.status = status;
            return this;
        }

        /**
         * Sets how many held messages were delivered.
         *
         * @param released the number of released messages
         * @return the builder instance
         */
        public Builder released(long released) {
            this.released = released;
            return this;
        }

        /**
         * Builds and returns a new instance of TopicStatusResponse using the properties set on the Builder.
         *
         * @return a new TopicStatusResponse instance.
         */
        public TopicStatusResponse build() {
            return new TopicStatusResponse(ern, status, released);
        }
    }
}
