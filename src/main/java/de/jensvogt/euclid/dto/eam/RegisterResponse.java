package de.jensvogt.euclid.dto.eam;

import de.jensvogt.euclid.dto.eam.model.User;

/**
 * Response returned after successfully registering a user.
 *
 * @param user the newly registered user
 */
public record RegisterResponse(User user) {

    /**
     * Creates a new instance of the Builder for constructing a RegisterResponse object.
     *
     * @return a new Builder instance for constructing RegisterResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link RegisterResponse} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The newly registered user.
         */
        private User user;

        /**
         * Sets the registered user.
         *
         * @param user the newly registered user
         * @return the builder instance
         */
        public Builder user(User user) {
            this.user = user;
            return this;
        }

        /**
         * Builds and returns a new instance of RegisterResponse using the properties set on the Builder.
         *
         * @return a new RegisterResponse instance populated with the user value.
         */
        public RegisterResponse build() {
            return new RegisterResponse(user);
        }
    }
}
