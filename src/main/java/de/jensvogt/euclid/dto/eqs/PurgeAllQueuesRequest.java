package de.jensvogt.euclid.dto.eqs;

/**
 * Request to purge all queues belonging to an account in a region.
 *
 * @param region    the region whose queues are purged
 * @param accountId ID of the account whose queues are purged
 * @param nameSpace the namespace to restrict the purge to; empty purges every namespace of the
 *                  account, which is what this action did before namespaces were part of it
 */
public record PurgeAllQueuesRequest(String region, String accountId, String nameSpace) {

    /**
     * Creates a new instance of the Builder for constructing a PurgeAllQueuesRequest object.
     *
     * @return a new Builder instance for constructing PurgeAllQueuesRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link PurgeAllQueuesRequest} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The region whose queues are purged.
         */
        private String region;

        /**
         * ID of the account whose queues are purged.
         */
        private String accountId;

        /**
         * The namespace to restrict the purge to; empty purges every namespace of the account.
         */
        private String nameSpace = "";

        /**
         * Sets the region whose queues are purged.
         *
         * @param region the region
         * @return the builder instance
         */
        public Builder region(String region) {
            this.region = region;
            return this;
        }

        /**
         * Sets the ID of the account whose queues are purged.
         *
         * @param accountId the account ID
         * @return the builder instance
         */
        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        /**
         * Sets the namespace to restrict the purge to.
         *
         * @param nameSpace the namespace, or empty to purge every namespace of the account
         * @return the builder instance
         */
        public Builder nameSpace(String nameSpace) {
            this.nameSpace = nameSpace;
            return this;
        }

        /**
         * Builds and returns a new instance of PurgeAllQueuesRequest using the properties set on the Builder.
         *
         * @return a new PurgeAllQueuesRequest instance populated with the region, account id and namespace values.
         */
        public PurgeAllQueuesRequest build() {
            return new PurgeAllQueuesRequest(region, accountId, nameSpace);
        }
    }
}
