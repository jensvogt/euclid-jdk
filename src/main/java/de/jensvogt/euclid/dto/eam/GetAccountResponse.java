package de.jensvogt.euclid.dto.eam;

import de.jensvogt.euclid.dto.eam.model.Account;

/**
 * One account, as a listing describes each of its own.
 * <p>
 * The same {@link Account} a {@code listAccounts} answer carries, rather than a shape of its own,
 * so that what a listing shows and what this shows cannot drift apart.
 *
 * @param account the account
 */
public record GetAccountResponse(Account account) {

    /**
     * Creates a new instance of the Builder for constructing a GetAccountResponse object.
     *
     * @return a new Builder instance for constructing GetAccountResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetAccountResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The account.
         */
        private Account account;

        /**
         * Sets the account.
         *
         * @param account the account
         * @return the builder instance
         */
        public Builder account(Account account) {
            this.account = account;
            return this;
        }

        /**
         * Builds and returns a new instance of GetAccountResponse using the properties set on the Builder.
         *
         * @return a new GetAccountResponse instance.
         */
        public GetAccountResponse build() {
            return new GetAccountResponse(account);
        }
    }
}
