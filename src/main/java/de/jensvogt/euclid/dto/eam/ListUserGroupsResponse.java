package de.jensvogt.euclid.dto.eam;

import de.jensvogt.euclid.dto.eam.model.UserGroup;

import java.util.List;

/**
 * Response returned from list-user-groups.
 *
 * @param userGroups the user groups
 * @param total      total number of user groups
 */
public record ListUserGroupsResponse(List<UserGroup> userGroups, long total) {

    /**
     * Creates a new instance of the Builder for constructing a ListUserGroupsResponse object.
     *
     * @return a new Builder instance for constructing ListUserGroupsResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListUserGroupsResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The user groups.
         */
        private List<UserGroup> userGroups;

        /**
         * The total number of user groups.
         */
        private long total;

        /**
         * Sets the user groups.
         *
         * @param userGroups the user groups
         * @return the builder instance
         */
        public Builder userGroups(List<UserGroup> userGroups) {
            this.userGroups = userGroups;
            return this;
        }

        /**
         * Sets the total number of user groups.
         *
         * @param total the total number of user groups
         * @return the builder instance
         */
        public Builder total(long total) {
            this.total = total;
            return this;
        }

        /**
         * Builds and returns a new instance of ListUserGroupsResponse using the properties set on the Builder.
         *
         * @return a new ListUserGroupsResponse instance.
         */
        public ListUserGroupsResponse build() {
            return new ListUserGroupsResponse(userGroups, total);
        }
    }
}
