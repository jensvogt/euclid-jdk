package de.jensvogt.euclid.dto.eam;

/**
 * Request to replace a password.
 *
 * <p>One request shape behind two things the server does, told apart by whether a user is named:
 * an empty {@code userId} changes the caller's own password and needs {@code oldPassword} to prove
 * it may be changed; a {@code userId} naming somebody else is an administrator's reset and needs no
 * old password, because an administrator is not supposed to know one.
 *
 * @param userId      the user whose password to change, or empty for the caller's own
 * @param oldPassword the current password, required only when changing your own
 * @param newPassword the password to replace it with
 */
public record ChangePasswordRequest(String userId, String oldPassword, String newPassword) {

    /**
     * Creates a new instance of the Builder for constructing a ChangePasswordRequest object.
     *
     * @return a new Builder instance for constructing ChangePasswordRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ChangePasswordRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The user whose password to change; empty for the caller's own.
         */
        private String userId = "";

        /**
         * The current password.
         */
        private String oldPassword = "";

        /**
         * The new password.
         */
        private String newPassword = "";

        /**
         * Sets the user whose password to change.
         *
         * @param userId the user id, or empty for the caller's own
         * @return the builder instance
         */
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * Sets the current password, which is what proves your own may be changed.
         *
         * @param oldPassword the current password
         * @return the builder instance
         */
        public Builder oldPassword(String oldPassword) {
            this.oldPassword = oldPassword;
            return this;
        }

        /**
         * Sets the new password.
         *
         * @param newPassword the password to replace the current one with
         * @return the builder instance
         */
        public Builder newPassword(String newPassword) {
            this.newPassword = newPassword;
            return this;
        }

        /**
         * Builds and returns a new instance of ChangePasswordRequest using the properties set on the Builder.
         *
         * @return a new ChangePasswordRequest instance.
         */
        public ChangePasswordRequest build() {
            return new ChangePasswordRequest(userId, oldPassword, newPassword);
        }
    }
}
