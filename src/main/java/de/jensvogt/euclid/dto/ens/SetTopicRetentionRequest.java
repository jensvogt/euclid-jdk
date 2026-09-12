package de.jensvogt.euclid.dto.ens;

/**
 * Request to set how long a topic keeps what is published to it.
 *
 * <p>Takes effect for messages published afterwards. The ones already stored keep the expiry they
 * were given, because that is what the database's TTL index acts on, and rewriting every message of
 * a busy topic to shorten its history is not something one call should quietly do.
 *
 * @param ern             the ERN of the topic
 * @param retentionPeriod how long a published message is kept, in seconds. Zero follows the
 *                        installation default rather than freezing a copy of it, so a topic that
 *                        has never been told what it wants tracks
 *                        {@code euclid.modules.ens.retention-period} as that changes. Minus one
 *                        keeps everything: the message is stored with no expiry at all rather than
 *                        with a very distant one, so nothing ever removes it. A value below -1 is
 *                        refused with HTTP 400
 */
public record SetTopicRetentionRequest(String ern, long retentionPeriod) {

    /**
     * Creates a new instance of the Builder for constructing a SetTopicRetentionRequest object.
     *
     * @return a new Builder instance for constructing SetTopicRetentionRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SetTopicRetentionRequest} instances.
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
         * How long a published message is kept, in seconds; zero follows the installation default.
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
         * Sets how long a published message is kept.
         *
         * @param retentionPeriod the retention period in seconds; zero follows the installation default
         * @return the builder instance
         */
        public Builder retentionPeriod(long retentionPeriod) {
            this.retentionPeriod = retentionPeriod;
            return this;
        }

        /**
         * Builds and returns a new instance of SetTopicRetentionRequest using the properties set on the Builder.
         *
         * @return a new SetTopicRetentionRequest instance.
         */
        public SetTopicRetentionRequest build() {
            return new SetTopicRetentionRequest(ern, retentionPeriod);
        }
    }
}
