package de.jensvogt.euclid.dto.eam;

/**
 * Request to delete an existing account.
 *
 * @param accountId account ID to delete
 */
public record DeleteAccountRequest(String accountId) {

    /**
     * Creates a new instance of the Builder for constructing a DeleteAccountRequest object.
     *
     * @return a new Builder instance for constructing DeleteAccountRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link DeleteAccountRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The account ID to delete.
         */
        private String accountId;

        /**
         * Sets the account ID to delete.
         *
         * @param accountId the account ID
         * @return the builder instance
         */
        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        /**
         * Builds and returns a new instance of DeleteAccountRequest using the properties set on the Builder.
         *
         * @return a new DeleteAccountRequest instance.
         */
        public DeleteAccountRequest build() {
            return new DeleteAccountRequest(accountId);
        }
    }
}
