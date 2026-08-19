package de.jensvogt.euclid.dto.access;

import de.jensvogt.euclid.dto.access.model.User;

import java.util.List;

public record ListUserResponse(List<User> users, long total) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private List<User> users;
        private long total;

        public Builder users(List<User> users) {
            this.users = users;
            return this;
        }

        public Builder total(long total) {
            this.total = total;
            return this;
        }

        public ListUserResponse build() {
            return new ListUserResponse(users, total);
        }
    }
}
