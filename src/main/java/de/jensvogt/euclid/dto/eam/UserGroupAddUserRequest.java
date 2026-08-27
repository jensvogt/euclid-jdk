package de.jensvogt.euclid.dto.eam;

/**
 * Request to add a user to a user group.
 *
 * @param userGroup user group ERN
 * @param user      user ERN
 */
public record UserGroupAddUserRequest(String userGroup, String user) {

    /**
     * Creates a new instance of the Builder for constructing a UserGroupAddUserRequest object.
     *
     * @return a new Builder instance for constructing UserGroupAddUserRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link UserGroupAddUserRequest} instances.
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
         * Builds and returns a new instance of UserGroupAddUserRequest using the properties set on the Builder.
         *
         * @return a new UserGroupAddUserRequest instance.
         */
        public UserGroupAddUserRequest build() {
            return new UserGroupAddUserRequest(userGroup, user);
        }
    }
}
