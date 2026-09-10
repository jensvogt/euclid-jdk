package de.jensvogt.euclid.dto.ens;

/**
 * Response containing the metadata of a topic.
 *
 * @param region    the region the topic lives in
 * @param accountId ID of the account the topic belongs to
 * @param owner     the user ID that owns the topic
 * @param nameSpace the namespace the topic belongs to
 * @param name      the topic's name
 * @param ern       the topic's ERN
 * @param size      total size in bytes of all messages currently in the topic
 * @param messages  number of messages currently in the topic
 * @param status    whether the topic hands what is published to it to its subscribers,
 *                  {@code "RUNNING"} or {@code "STOPPED"}
 * @param retentionPeriod how long a message published to this topic is kept, in seconds. Zero means
 *                  the topic follows the installation default as that changes, rather than having
 *                  frozen a copy of whatever it was on the day the topic was created
 * @param held      how many messages are waiting to be handed over when the topic is started again.
 *                  Zero while the topic is running - there is nothing being held then, so the count
 *                  is not one the server pays for
 */
public record GetTopicMetadataResponse(String region, String accountId, String owner, String nameSpace, String name,
                                        String ern, long size, long messages, String status, long retentionPeriod,
                                        long held) {

    /**
     * Creates a new instance of the Builder for constructing a GetTopicMetadataResponse object.
     *
     * @return a new Builder instance for constructing GetTopicMetadataResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetTopicMetadataResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The region the topic lives in.
         */
        private String region;

        /**
         * The account the topic belongs to.
         */
        private String accountId;

        /**
         * The user ID that owns the topic.
         */
        private String owner;

        /**
         * The namespace the topic belongs to.
         */
        private String nameSpace;

        /**
         * The topic name.
         */
        private String name;

        /**
         * The topic ERN.
         */
        private String ern;

        /**
         * Total size in bytes of all messages currently in the topic.
         */
        private long size;

        /**
         * Number of messages currently in the topic.
         */
        private long messages;

        /**
         * Whether the topic hands what is published to it to its subscribers.
         */
        private String status;

        /**
         * How long a message published to this topic is kept, in seconds.
         */
        private long retentionPeriod;

        /**
         * How many messages are waiting to be handed over when the topic is started again.
         */
        private long held;

        /**
         * Sets the region the topic lives in.
         *
         * @param region the region
         * @return the builder instance
         */
        public Builder region(String region) {
            this.region = region;
            return this;
        }

        /**
         * Sets the account the topic belongs to.
         *
         * @param accountId the account ID
         * @return the builder instance
         */
        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        /**
         * Sets the user ID that owns the topic.
         *
         * @param owner the owner's user ID
         * @return the builder instance
         */
        public Builder owner(String owner) {
            this.owner = owner;
            return this;
        }

        /**
         * Sets the namespace the topic belongs to.
         *
         * @param nameSpace the namespace
         * @return the builder instance
         */
        public Builder nameSpace(String nameSpace) {
            this.nameSpace = nameSpace;
            return this;
        }

        /**
         * Sets the topic name.
         *
         * @param name the topic name
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

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
         * Sets the total size in bytes of all messages currently in the topic.
         *
         * @param size the size in bytes
         * @return the builder instance
         */
        public Builder size(long size) {
            this.size = size;
            return this;
        }

        /**
         * Sets the number of messages currently in the topic.
         *
         * @param messages the number of messages
         * @return the builder instance
         */
        public Builder messages(long messages) {
            this.messages = messages;
            return this;
        }

        /**
         * Sets whether the topic hands what is published to it to its subscribers.
         *
         * @param status {@code "RUNNING"} or {@code "STOPPED"}
         * @return the builder instance
         */
        public Builder status(String status) {
            this.status = status;
            return this;
        }

        /**
         * Sets how long a message published to this topic is kept.
         *
         * @param retentionPeriod the retention period in seconds
         * @return the builder instance
         */
        public Builder retentionPeriod(long retentionPeriod) {
            this.retentionPeriod = retentionPeriod;
            return this;
        }

        /**
         * Sets how many messages are waiting to be handed over when the topic is started again.
         *
         * @param held the number of held messages
         * @return the builder instance
         */
        public Builder held(long held) {
            this.held = held;
            return this;
        }

        /**
         * Builds and returns a new instance of GetTopicMetadataResponse using the properties set on the Builder.
         *
         * @return a new GetTopicMetadataResponse instance.
         */
        public GetTopicMetadataResponse build() {
            return new GetTopicMetadataResponse(region, accountId, owner, nameSpace, name, ern, size, messages, status,
                    retentionPeriod, held);
        }
    }
}
