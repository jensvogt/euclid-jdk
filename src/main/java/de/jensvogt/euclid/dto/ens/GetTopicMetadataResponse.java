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
 */
public record GetTopicMetadataResponse(String region, String accountId, String owner, String nameSpace, String name,
                                        String ern, long size, long messages) {

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
         * Builds and returns a new instance of GetTopicMetadataResponse using the properties set on the Builder.
         *
         * @return a new GetTopicMetadataResponse instance.
         */
        public GetTopicMetadataResponse build() {
            return new GetTopicMetadataResponse(region, accountId, owner, nameSpace, name, ern, size, messages);
        }
    }
}
