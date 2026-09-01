package de.jensvogt.euclid.dto.eam;

import de.jensvogt.euclid.dto.Metadata;
import de.jensvogt.euclid.dto.eam.model.User;

/**
 * Response returned after successfully registering a user.
 *
 * @param metadata the caller identity the server resolved the request to
 * @param user     the newly registered user
 */
public record RegisterResponse(Metadata metadata, User user) {

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
         * The caller identity the server resolved the request to.
         */
        private Metadata metadata;

        /**
         * The newly registered user.
         */
        private User user;

        /**
         * Sets the caller identity the server resolved the request to.
         *
         * @param metadata the caller identity the server resolved the request to
         * @return the builder instance
         */
        public Builder metadata(Metadata metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Sets the newly registered user.
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
         * @return a new RegisterResponse instance.
         */
        public RegisterResponse build() {
            return new RegisterResponse(metadata, user);
        }
    }
}
