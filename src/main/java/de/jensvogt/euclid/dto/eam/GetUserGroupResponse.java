package de.jensvogt.euclid.dto.eam;

import de.jensvogt.euclid.dto.eam.model.UserGroup;

/**
 * One user group, as a listing describes each of its own.
 * <p>
 * The same {@link UserGroup} a {@code listUserGroups} answer carries, member ids included, rather
 * than a shape of its own, so that what a listing shows and what this shows cannot drift apart.
 *
 * @param userGroup the group
 */
public record GetUserGroupResponse(UserGroup userGroup) {

    /**
     * Creates a new instance of the Builder for constructing a GetUserGroupResponse object.
     *
     * @return a new Builder instance for constructing GetUserGroupResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetUserGroupResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The group.
         */
        private UserGroup userGroup;

        /**
         * Sets the group.
         *
         * @param userGroup the group
         * @return the builder instance
         */
        public Builder userGroup(UserGroup userGroup) {
            this.userGroup = userGroup;
            return this;
        }

        /**
         * Builds and returns a new instance of GetUserGroupResponse using the properties set on the Builder.
         *
         * @return a new GetUserGroupResponse instance.
         */
        public GetUserGroupResponse build() {
            return new GetUserGroupResponse(userGroup);
        }
    }
}
