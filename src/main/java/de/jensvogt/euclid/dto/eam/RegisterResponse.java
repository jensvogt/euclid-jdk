package de.jensvogt.euclid.dto.eam;

import de.jensvogt.euclid.dto.eam.model.User;

public record RegisterResponse(User user) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

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
