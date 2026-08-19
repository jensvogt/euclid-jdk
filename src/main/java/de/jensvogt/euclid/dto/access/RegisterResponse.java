package de.jensvogt.euclid.dto.access;

import de.jensvogt.euclid.dto.access.model.User;

public record RegisterResponse(User user) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private User user;

        public Builder user(User user) {
            this.user = user;
            return this;
        }

        public RegisterResponse build() {
            return new RegisterResponse(user);
        }
    }
}
