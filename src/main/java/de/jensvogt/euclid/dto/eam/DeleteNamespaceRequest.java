package de.jensvogt.euclid.dto.eam;

/**
 * Request to delete an existing namespace.
 *
 * @param accountId the account the namespace belongs to
 * @param name      namespace name to delete
 */
public record DeleteNamespaceRequest(String accountId, String name) {

    /**
     * Creates a new instance of the Builder for constructing a DeleteNamespaceRequest object.
     *
     * @return a new Builder instance for constructing DeleteNamespaceRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link DeleteNamespaceRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The account the namespace belongs to.
         */
        private String accountId;

        /**
         * The namespace name to delete.
         */
        private String name;

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
         * Sets the namespace name to delete.
         *
         * @param name the namespace name
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Builds and returns a new instance of DeleteNamespaceRequest using the properties set on the Builder.
         *
         * @return a new DeleteNamespaceRequest instance.
         */
        public DeleteNamespaceRequest build() {
            return new DeleteNamespaceRequest(accountId, name);
        }
    }
}
