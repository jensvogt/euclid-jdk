package de.jensvogt.euclid.dto.eam;

/**
 * Request to register a new user.
 *
 * @param userId    the ID of the user to register
 * @param password  the user's password
 * @param email     the user's email address
 * @param accountId ID of the account the user belongs to
 * @param region    the user's region
 * @param isAdmin   whether the user should have administrator privileges
 */
public record RegisterRequest(String userId, String password, String email, String accountId, String region, boolean isAdmin) {

    /**
     * Creates a new instance of the Builder for constructing a RegisterRequest object.
     *
     * @return a new Builder instance for constructing RegisterRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link RegisterRequest} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ID of the user to register.
         */
        private String userId;

        /**
         * The user's password.
         */
        private String password;

        /**
         * The user's email address.
         */
        private String email;

        /**
         * ID of the account the user belongs to.
         */
        private String accountId;

        /**
         * The user's region.
         */
        private String region;

        /**
         * Whether the user should have administrator privileges.
         */
        private boolean isAdmin;

        /**
         * Sets the ID of the user to register.
         *
         * @param userId the user ID
         * @return the builder instance
         */
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * Sets the user's password.
         *
         * @param password the password
         * @return the builder instance
         */
        public Builder password(String password) {
            this.password = password;
            return this;
        }

        /**
         * Sets the user's email address.
         *
         * @param email the email address
         * @return the builder instance
         */
        public Builder email(String email) {
            this.email = email;
            return this;
        }

        /**
         * Sets the ID of the account the user belongs to.
         *
         * @param accountId the account ID
         * @return the builder instance
         */
        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        /**
         * Sets the user's region.
         *
         * @param region the region
         * @return the builder instance
         */
        public Builder region(String region) {
            this.region = region;
            return this;
        }

        /**
         * Sets whether the user should have administrator privileges.
         *
         * @param isAdmin {@code true} if the user should be an administrator
         * @return the builder instance
         */
        public Builder isAdmin(boolean isAdmin) {
            this.isAdmin = isAdmin;
            return this;
        }

        /**
         * Builds and returns a new instance of RegisterRequest using the properties set on the Builder.
         *
         * @return a new RegisterRequest instance populated with the user id, password, email, account id, region and admin flag values.
         */
        public RegisterRequest build() {
            return new RegisterRequest(userId, password, email, accountId, region, isAdmin);
        }
    }
}
