package de.jensvogt.euclid.dto.ens;

/**
 * Response returned after a topic's retention period has been set.
 *
 * @param ern             the ERN of the topic
 * @param retentionPeriod the retention period the topic now has, in seconds. Zero means it follows
 *                        the installation default rather than a period of its own; minus one means
 *                        it keeps everything published to it
 */
public record SetTopicRetentionResponse(String ern, long retentionPeriod) {

    /**
     * Creates a new instance of the Builder for constructing a SetTopicRetentionResponse object.
     *
     * @return a new Builder instance for constructing SetTopicRetentionResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SetTopicRetentionResponse} instances.
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
         * The retention period the topic now has, in seconds.
         */
        private long retentionPeriod;

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
         * Sets the retention period the topic now has.
         *
         * @param retentionPeriod the retention period in seconds
         * @return the builder instance
         */
        public Builder retentionPeriod(long retentionPeriod) {
            this.retentionPeriod = retentionPeriod;
            return this;
        }

        /**
         * Builds and returns a new instance of SetTopicRetentionResponse using the properties set on the Builder.
         *
         * @return a new SetTopicRetentionResponse instance.
         */
        public SetTopicRetentionResponse build() {
            return new SetTopicRetentionResponse(ern, retentionPeriod);
        }
    }
}
