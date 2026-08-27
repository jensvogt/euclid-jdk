package de.jensvogt.euclid.dto.eam;

import de.jensvogt.euclid.dto.eam.model.Account;

import java.util.List;

/**
 * Response returned from list-accounts.
 *
 * @param accounts the accounts
 * @param total    total number of accounts
 */
public record ListAccountsResponse(List<Account> accounts, long total) {

    /**
     * Creates a new instance of the Builder for constructing a ListAccountsResponse object.
     *
     * @return a new Builder instance for constructing ListAccountsResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListAccountsResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The accounts.
         */
        private List<Account> accounts;

        /**
         * The total number of accounts.
         */
        private long total;

        /**
         * Sets the accounts.
         *
         * @param accounts the accounts
         * @return the builder instance
         */
        public Builder accounts(List<Account> accounts) {
            this.accounts = accounts;
            return this;
        }

        /**
         * Sets the total number of accounts.
         *
         * @param total the total number of accounts
         * @return the builder instance
         */
        public Builder total(long total) {
            this.total = total;
            return this;
        }

        /**
         * Builds and returns a new instance of ListAccountsResponse using the properties set on the Builder.
         *
         * @return a new ListAccountsResponse instance.
         */
        public ListAccountsResponse build() {
            return new ListAccountsResponse(accounts, total);
        }
    }
}
