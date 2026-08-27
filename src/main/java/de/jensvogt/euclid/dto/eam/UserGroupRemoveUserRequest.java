package de.jensvogt.euclid.dto.eam;

/**
 * Request to remove a user from a user group.
 *
 * @param userGroup user group ERN
 * @param user      user ERN
 */
public record UserGroupRemoveUserRequest(String userGroup, String user) {

    /**
     * Creates a new instance of the Builder for constructing a UserGroupRemoveUserRequest object.
     *
     * @return a new Builder instance for constructing UserGroupRemoveUserRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link UserGroupRemoveUserRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The user group ERN.
         */
        private String userGroup;

        /**
         * The user ERN.
         */
        private String user;

        /**
         * Sets the user group ERN.
         *
         * @param userGroup the user group ERN
         * @return the builder instance
         */
        public Builder userGroup(String userGroup) {
            this.userGroup = userGroup;
            return this;
        }

        /**
         * Sets the user ERN.
         *
         * @param user the user ERN
         * @return the builder instance
         */
        public Builder user(String user) {
            this.user = user;
            return this;
        }

        /**
         * Builds and returns a new instance of UserGroupRemoveUserRequest using the properties set on the Builder.
         *
         * @return a new UserGroupRemoveUserRequest instance.
         */
        public UserGroupRemoveUserRequest build() {
            return new UserGroupRemoveUserRequest(userGroup, user);
        }
    }
}
