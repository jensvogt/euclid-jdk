package de.jensvogt.euclid.dto.eam;

/**
 * Request to create a new account.
 *
 * @param accountId   account ID, unique across the deployment
 * @param name        human-readable account name
 * @param description free-text description of the account's purpose
 */
public record CreateAccountRequest(String accountId, String name, String description) {

    /**
     * Creates a new instance of the Builder for constructing a CreateAccountRequest object.
     *
     * @return a new Builder instance for constructing CreateAccountRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CreateAccountRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The account ID.
         */
        private String accountId;

        /**
         * The account name.
         */
        private String name;

        /**
         * The account description.
         */
        private String description = "";

        /**
         * Sets the account ID.
         *
         * @param accountId the account ID
         * @return the builder instance
         */
        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        /**
         * Sets the account name.
         *
         * @param name the account name
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the account description.
         *
         * @param description the account description
         * @return the builder instance
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Builds and returns a new instance of CreateAccountRequest using the properties set on the Builder.
         *
         * @return a new CreateAccountRequest instance.
         */
        public CreateAccountRequest build() {
            return new CreateAccountRequest(accountId, name, description);
        }
    }
}
