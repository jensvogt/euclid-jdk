package de.jensvogt.euclid.dto.eam;

import de.jensvogt.euclid.dto.eam.model.UserGroup;

/**
 * Response returned after successfully creating a user group.
 *
 * @param userGroup the newly created group
 */
public record CreateUserGroupResponse(UserGroup userGroup) {

    /**
     * Creates a new instance of the Builder for constructing a CreateUserGroupResponse object.
     *
     * @return a new Builder instance for constructing CreateUserGroupResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CreateUserGroupResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The newly created group.
         */
        private UserGroup userGroup;

        /**
         * Sets the newly created group.
         *
         * @param userGroup the user group
         * @return the builder instance
         */
        public Builder userGroup(UserGroup userGroup) {
            this.userGroup = userGroup;
            return this;
        }

        /**
         * Builds and returns a new instance of CreateUserGroupResponse using the properties set on the Builder.
         *
         * @return a new CreateUserGroupResponse instance.
         */
        public CreateUserGroupResponse build() {
            return new CreateUserGroupResponse(userGroup);
        }
    }
}
