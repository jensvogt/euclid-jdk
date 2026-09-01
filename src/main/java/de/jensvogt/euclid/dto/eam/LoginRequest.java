package de.jensvogt.euclid.dto.eam;

/**
 * Request to authenticate against the Euclid server.
 * <p>
 * The user is looked up by {@code userId}, falling back to {@code email} when no user ID is
 * given - so exactly one of the two identifies the account. The password is checked against the
 * stored PBKDF2-HMAC-SHA256 hash either way.
 *
 * @param userId   the user ID to log in as, or empty to identify the user by email
 * @param password the user's password
 * @param email    the email address to log in with when no user ID is given
 */
public record LoginRequest(String userId, String password, String email) {

    /**
     * Creates a new instance of the Builder for constructing a LoginRequest object.
     *
     * @return a new Builder instance for constructing LoginRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link LoginRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The user ID to log in as, or empty to identify the user by email.
         */
        private String userId = "";

        /**
         * The user's password.
         */
        private String password = "";

        /**
         * The email address to log in with when no user ID is given.
         */
        private String email = "";

        /**
         * Sets the user ID to log in as, or empty to identify the user by email.
         *
         * @param userId the user ID to log in as, or empty to identify the user by email
         * @return the builder instance
         */
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * Sets the user's password.
         *
         * @param password the user's password
         * @return the builder instance
         */
        public Builder password(String password) {
            this.password = password;
            return this;
        }

        /**
         * Sets the email address to log in with when no user ID is given.
         *
         * @param email the email address to log in with when no user ID is given
         * @return the builder instance
         */
        public Builder email(String email) {
            this.email = email;
            return this;
        }

        /**
         * Builds and returns a new instance of LoginRequest using the properties set on the Builder.
         *
         * @return a new LoginRequest instance.
         */
        public LoginRequest build() {
            return new LoginRequest(userId, password, email);
        }
    }
}
