package de.jensvogt.euclid.dto.eam;

/**
 * Request to give a user a different user ID.
 * <p>
 * Both ids are named outright, and the old one is not defaulted to the caller's own the way
 * {@link ChangePasswordRequest}'s is. Renaming is an administrator's action on somebody, and a
 * request that could mean "me" by saying nothing is one keystroke from renaming the wrong account.
 *
 * @param userId    the user to rename
 * @param newUserId the id they should have from now on
 */
public record ChangeUserIdRequest(String userId, String newUserId) {

    /**
     * Creates a new instance of the Builder for constructing a ChangeUserIdRequest object.
     *
     * @return a new Builder instance for constructing ChangeUserIdRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ChangeUserIdRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The user to rename.
         */
        private String userId;

        /**
         * The id they should have from now on.
         */
        private String newUserId;

        /**
         * Sets the user to rename.
         *
         * @param userId the user's current id
         * @return the builder instance
         */
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * Sets the id the user should have from now on.
         *
         * @param newUserId the new id; refused if another user already holds it
         * @return the builder instance
         */
        public Builder newUserId(String newUserId) {
            this.newUserId = newUserId;
            return this;
        }

        /**
         * Builds and returns a new instance of ChangeUserIdRequest using the properties set on the Builder.
         *
         * @return a new ChangeUserIdRequest instance.
         */
        public ChangeUserIdRequest build() {
            return new ChangeUserIdRequest(userId, newUserId);
        }
    }
}
