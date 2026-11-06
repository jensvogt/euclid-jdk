package de.jensvogt.euclid.dto.eam;

import de.jensvogt.euclid.dto.eam.model.Account;

import java.util.List;

/**
 * Response returned from list-my-accounts: the accounts the caller may work in.
 * <p>
 * Not {@link ListAccountsResponse} with a different filter: that one answers "every account there
 * is" and is an administrator's view of the installation. This one is about the caller, which is
 * why it carries a field that one has no use for - a client offering a choice has to know which of
 * them to offer first, and the home account is the answer whenever nothing else is known.
 *
 * @param accounts      the accounts the caller may work in, ordered by account id
 * @param total         total number of accounts
 * @param homeAccountId the caller's own account - the one they were registered in; always among
 *                      {@code accounts}, and the one to select when the user has not said otherwise
 */
public record ListMyAccountsResponse(List<Account> accounts, long total, String homeAccountId) {

    /**
     * Creates a new instance of the Builder for constructing a ListMyAccountsResponse object.
     *
     * @return a new Builder instance for constructing ListMyAccountsResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListMyAccountsResponse} instances.
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
         * The caller's home account.
         */
        private String homeAccountId;

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
         * Sets the caller's home account.
         *
         * @param homeAccountId the id of the account the caller was registered in
         * @return the builder instance
         */
        public Builder homeAccountId(String homeAccountId) {
            this.homeAccountId = homeAccountId;
            return this;
        }

        /**
         * Builds and returns a new instance of ListMyAccountsResponse using the properties set on the Builder.
         *
         * @return a new ListMyAccountsResponse instance.
         */
        public ListMyAccountsResponse build() {
            return new ListMyAccountsResponse(accounts, total, homeAccountId);
        }
    }
}
