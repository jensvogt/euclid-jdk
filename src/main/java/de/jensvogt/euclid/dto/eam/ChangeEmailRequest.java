package de.jensvogt.euclid.dto.eam;

/**
 * Request to give a user a different email address.
 * <p>
 * The user is named outright, as in {@link ChangeUserIdRequest} and not as in
 * {@link ChangePasswordRequest}: an email is a way in - login accepts it in place of the user id -
 * so a request that could mean "me" by saying nothing is one keystroke from pointing the wrong
 * account at an address.
 *
 * @param userId the user whose address is changing
 * @param email  the address they should have from now on
 */
public record ChangeEmailRequest(String userId, String email) {

    /**
     * Creates a new instance of the Builder for constructing a ChangeEmailRequest object.
     *
     * @return a new Builder instance for constructing ChangeEmailRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ChangeEmailRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The user whose address is changing.
         */
        private String userId;

        /**
         * The address they should have from now on.
         */
        private String email;

        /**
         * Sets the user whose address is changing.
         *
         * @param userId the user's id
         * @return the builder instance
         */
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * Sets the address the user should have from now on.
         *
         * @param email the new address; refused if another user already holds it
         * @return the builder instance
         */
        public Builder email(String email) {
            this.email = email;
            return this;
        }

        /**
         * Builds and returns a new instance of ChangeEmailRequest using the properties set on the Builder.
         *
         * @return a new ChangeEmailRequest instance.
         */
        public ChangeEmailRequest build() {
            return new ChangeEmailRequest(userId, email);
        }
    }
}
