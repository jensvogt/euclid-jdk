package de.jensvogt.euclid.dto.eam;

import de.jensvogt.euclid.dto.eam.model.Account;

/**
 * Response returned after successfully creating an account.
 *
 * @param account the newly created account
 */
public record CreateAccountResponse(Account account) {

    /**
     * Creates a new instance of the Builder for constructing a CreateAccountResponse object.
     *
     * @return a new Builder instance for constructing CreateAccountResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CreateAccountResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The newly created account.
         */
        private Account account;

        /**
         * Sets the newly created account.
         *
         * @param account the account
         * @return the builder instance
         */
        public Builder account(Account account) {
            this.account = account;
            return this;
        }

        /**
         * Builds and returns a new instance of CreateAccountResponse using the properties set on the Builder.
         *
         * @return a new CreateAccountResponse instance.
         */
        public CreateAccountResponse build() {
            return new CreateAccountResponse(account);
        }
    }
}
