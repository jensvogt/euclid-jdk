package de.jensvogt.euclid.dto.eqs;

/**
 * Response containing the metadata of a queue.
 *
 * @param region    the region the queue lives in
 * @param accountId ID of the account the queue belongs to
 * @param owner     the user ID that owns the queue
 * @param nameSpace the namespace the queue belongs to
 * @param name      the queue's name
 * @param ern       the queue's ERN (Entity Resource Name)
 * @param size      total size in bytes of all messages currently in the queue
 * @param messages  number of messages currently in the queue
 */
public record GetQueueMetadataResponse(String region, String accountId, String owner, String nameSpace,
                                        String name, String ern, long size, long messages) {

    /**
     * Creates a new instance of the Builder for constructing a GetQueueMetadataResponse object.
     *
     * @return a new Builder instance for constructing GetQueueMetadataResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetQueueMetadataResponse} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The region the queue lives in.
         */
        private String region;

        /**
         * ID of the account the queue belongs to.
         */
        private String accountId;

        /**
         * The user ID that owns the queue.
         */
        private String owner;

        /**
         * The namespace the queue belongs to.
         */
        private String nameSpace;

        /**
         * The queue's name.
         */
        private String name;

        /**
         * The queue's ERN (Entity Resource Name).
         */
        private String ern;

        /**
         * Total size in bytes of all messages currently in the queue.
         */
        private long size;

        /**
         * Number of messages currently in the queue.
         */
        private long messages;

        /**
         * Sets the region the queue lives in.
         *
         * @param region the region
         * @return the builder instance
         */
        public Builder region(String region) {
            this.region = region;
            return this;
        }

        /**
         * Sets the ID of the account the queue belongs to.
         *
         * @param accountId the account ID
         * @return the builder instance
         */
        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        /**
         * Sets the user ID that owns the queue.
         *
         * @param owner the owner's user ID
         * @return the builder instance
         */
        public Builder owner(String owner) {
            this.owner = owner;
            return this;
        }

        /**
         * Sets the namespace the queue belongs to.
         *
         * @param nameSpace the namespace
         * @return the builder instance
         */
        public Builder nameSpace(String nameSpace) {
            this.nameSpace = nameSpace;
            return this;
        }

        /**
         * Sets the queue's name.
         *
         * @param name the queue name
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the queue's ERN.
         *
         * @param ern the queue ERN
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the total size in bytes of all messages currently in the queue.
         *
         * @param size the size in bytes
         * @return the builder instance
         */
        public Builder size(long size) {
            this.size = size;
            return this;
        }

        /**
         * Sets the number of messages currently in the queue.
         *
         * @param messages the number of messages
         * @return the builder instance
         */
        public Builder messages(long messages) {
            this.messages = messages;
            return this;
        }

        /**
         * Builds and returns a new instance of GetQueueMetadataResponse using the properties set on the Builder.
         *
         * @return a new GetQueueMetadataResponse instance populated with the queue metadata values.
         */
        public GetQueueMetadataResponse build() {
            return new GetQueueMetadataResponse(region, accountId, owner, nameSpace, name, ern, size, messages);
        }
    }
}
