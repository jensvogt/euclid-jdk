package de.jensvogt.euclid.dto.eam;

/**
 * Request to grant a user access to a namespace within an account.
 *
 * @param user      user ERN to grant access to
 * @param accountId the account the namespace belongs to
 * @param namespace namespace within accountId to grant access to
 */
public record GrantNamespaceAccessRequest(String user, String accountId, String namespace) {

    /**
     * Creates a new instance of the Builder for constructing a GrantNamespaceAccessRequest object.
     *
     * @return a new Builder instance for constructing GrantNamespaceAccessRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GrantNamespaceAccessRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The user ERN to grant access to.
         */
        private String user;

        /**
         * The account the namespace belongs to.
         */
        private String accountId;

        /**
         * The namespace to grant access to.
         */
        private String namespace;

        /**
         * Sets the user ERN to grant access to.
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
         * Sets the namespace to grant access to.
         *
         * @param namespace the namespace name
         * @return the builder instance
         */
        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        /**
         * Builds and returns a new instance of GrantNamespaceAccessRequest using the properties set on the Builder.
         *
         * @return a new GrantNamespaceAccessRequest instance.
         */
        public GrantNamespaceAccessRequest build() {
            return new GrantNamespaceAccessRequest(user, accountId, namespace);
        }
    }
}
