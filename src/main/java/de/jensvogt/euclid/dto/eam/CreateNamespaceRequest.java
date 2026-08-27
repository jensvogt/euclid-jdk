package de.jensvogt.euclid.dto.eam;

/**
 * Request to create a new namespace under an account.
 *
 * @param accountId   the account the namespace belongs to
 * @param name        namespace name, unique within accountId
 * @param description free-text description of the namespace's purpose
 */
public record CreateNamespaceRequest(String accountId, String name, String description) {

    /**
     * Creates a new instance of the Builder for constructing a CreateNamespaceRequest object.
     *
     * @return a new Builder instance for constructing CreateNamespaceRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CreateNamespaceRequest} instances.
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
         * The namespace name.
         */
        private String name;

        /**
         * The namespace description.
         */
        private String description = "";

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
         * Sets the namespace name.
         *
         * @param name the namespace name
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the namespace description.
         *
         * @param description the namespace description
         * @return the builder instance
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Builds and returns a new instance of CreateNamespaceRequest using the properties set on the Builder.
         *
         * @return a new CreateNamespaceRequest instance.
         */
        public CreateNamespaceRequest build() {
            return new CreateNamespaceRequest(accountId, name, description);
        }
    }
}
