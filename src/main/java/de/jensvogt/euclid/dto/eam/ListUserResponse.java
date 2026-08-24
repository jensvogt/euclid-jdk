package de.jensvogt.euclid.dto.eam;

import de.jensvogt.euclid.dto.eam.model.User;

import java.util.List;

/**
 * Response containing a page of users.
 *
 * @param users the list of users returned
 * @param total the total number of users matching the request, across all pages
 */
public record ListUserResponse(List<User> users, long total) {

    /**
     * Creates a new instance of the Builder for constructing a ListUserResponse object.
     *
     * @return a new Builder instance for constructing ListUserResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListUserResponse} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The list of users returned.
         */
        private List<User> users;

        /**
         * The total number of users matching the request, across all pages.
         */
        private long total;

        /**
         * Sets the list of users.
         *
         * @param users the list of users returned
         * @return the builder instance
         */
        public Builder users(List<User> users) {
            this.users = users;
            return this;
        }

        /**
         * Sets the total number of users matching the request.
         *
         * @param total the total number of users, across all pages
         * @return the builder instance
         */
        public Builder total(long total) {
            this.total = total;
            return this;
        }

        /**
         * Builds and returns a new instance of ListUserResponse using the properties set on the Builder.
         *
         * @return a new ListUserResponse instance populated with the users and total values.
         */
        public ListUserResponse build() {
            return new ListUserResponse(users, total);
        }
    }
}
