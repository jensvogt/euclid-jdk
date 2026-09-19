package de.jensvogt.euclid.dto.eam;

/**
 * Request for one account, by account ID or by ERN.
 * <p>
 * Exactly one of the two is sent. An account is named by its ID rather than by its name: the ID is
 * what an ERN's fourth field carries and what every resource in the installation is scoped by,
 * while the name is descriptive and addresses nothing.
 *
 * @param accountId the account's ID, or null when it is named by ERN instead
 * @param ern       the ERN uniquely identifying the account, used when no ID is given
 */
public record GetAccountRequest(String accountId, String ern) {

    /**
     * Creates a new instance of the Builder for constructing a GetAccountRequest object.
     *
     * @return a new Builder instance for constructing GetAccountRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetAccountRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The account's ID.
         */
        private String accountId;

        /**
         * The ERN uniquely identifying the account.
         */
        private String ern;

        /**
         * Sets the ID of the account.
         *
         * @param accountId the account's ID
         * @return the builder instance
         */
        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        /**
         * Sets the ERN of the account.
         *
         * @param ern the ERN uniquely identifying the account
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Builds and returns a new instance of GetAccountRequest using the properties set on the Builder.
         *
         * @return a new GetAccountRequest instance.
         */
        public GetAccountRequest build() {
            return new GetAccountRequest(accountId, ern);
        }
    }
}
