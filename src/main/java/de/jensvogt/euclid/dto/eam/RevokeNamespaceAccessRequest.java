package de.jensvogt.euclid.dto.eam;

/**
 * Request to revoke a user's access to a namespace within an account.
 *
 * @param user      user ERN to revoke access from
 * @param accountId the account the namespace belongs to
 * @param namespace namespace within accountId to revoke access from
 */
public record RevokeNamespaceAccessRequest(String user, String accountId, String namespace) {

    /**
     * Creates a new instance of the Builder for constructing a RevokeNamespaceAccessRequest object.
     *
     * @return a new Builder instance for constructing RevokeNamespaceAccessRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link RevokeNamespaceAccessRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The user ERN to revoke access from.
         */
        private String user;

        /**
         * The account the namespace belongs to.
         */
        private String accountId;

        /**
         * The namespace to revoke access from.
         */
        private String namespace;

        /**
         * Sets the user ERN to revoke access from.
         *
         * @param user the user ERN
         * @return the builder instance
         */
        public Builder user(String user) {
            this.user = user;
            return this;
        }

        /**
         * Sets the account the namespace belongs to.
         *
         * @param accountId the account ID
         * @return the builder instance
         */
        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        /**
         * Sets the namespace to revoke access from.
         *
         * @param namespace the namespace name
         * @return the builder instance
         */
        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        /**
         * Builds and returns a new instance of RevokeNamespaceAccessRequest using the properties set on the Builder.
         *
         * @return a new RevokeNamespaceAccessRequest instance.
         */
        public RevokeNamespaceAccessRequest build() {
            return new RevokeNamespaceAccessRequest(user, accountId, namespace);
        }
    }
}
