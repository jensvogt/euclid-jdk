package de.jensvogt.euclid.dto.eam;

import de.jensvogt.euclid.dto.eam.model.User;

/**
 * One user, as a listing describes each of its own.
 * <p>
 * The same {@link User} a {@code listUsers} answer carries, rather than a shape of its own, so that
 * what a listing shows and what this shows cannot drift apart.
 *
 * @param user the user
 */
public record GetUserResponse(User user) {

    /**
     * Creates a new instance of the Builder for constructing a GetUserResponse object.
     *
     * @return a new Builder instance for constructing GetUserResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetUserResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The user.
         */
        private User user;

        /**
         * Sets the user.
         *
         * @param user the user
         * @return the builder instance
         */
        public Builder user(User user) {
            this.user = user;
            return this;
        }

        /**
         * Builds and returns a new instance of GetUserResponse using the properties set on the Builder.
         *
         * @return a new GetUserResponse instance.
         */
        public GetUserResponse build() {
            return new GetUserResponse(user);
        }
    }
}
