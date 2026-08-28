package de.jensvogt.euclid.dto.ens;

/**
 * Request to delete all messages from every topic in a region/account, optionally restricted to
 * a single namespace.
 *
 * @param region    region of the topics
 * @param accountId account ID of the topics
 * @param nameSpace namespace to restrict the purge to, or empty to purge every namespace
 */
public record PurgeAllTopicsRequest(String region, String accountId, String nameSpace) {

    /**
     * Creates a new instance of the Builder for constructing a PurgeAllTopicsRequest object.
     *
     * @return a new Builder instance for constructing PurgeAllTopicsRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link PurgeAllTopicsRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The region of the topics.
         */
        private String region;

        /**
         * The account ID of the topics.
         */
        private String accountId;

        /**
         * The namespace to restrict the purge to.
         */
        private String nameSpace = "";

        /**
         * Sets the region of the topics.
         *
         * @param region the region
         * @return the builder instance
         */
        public Builder region(String region) {
            this.region = region;
            return this;
        }

        /**
         * Sets the account ID of the topics.
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
         * @param nameSpace the namespace, or empty to purge every namespace
         * @return the builder instance
         */
        public Builder nameSpace(String nameSpace) {
            this.nameSpace = nameSpace;
            return this;
        }

        /**
         * Builds and returns a new instance of PurgeAllTopicsRequest using the properties set on the Builder.
         *
         * @return a new PurgeAllTopicsRequest instance.
         */
        public PurgeAllTopicsRequest build() {
            return new PurgeAllTopicsRequest(region, accountId, nameSpace);
        }
    }
}
