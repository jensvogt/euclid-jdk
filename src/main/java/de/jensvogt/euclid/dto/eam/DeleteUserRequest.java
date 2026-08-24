package de.jensvogt.euclid.dto.eam;

/**
 * Request to delete a user.
 *
 * @param userId the id of the user to delete
 */
public record DeleteUserRequest(String userId) {

    /**
     * Creates a new instance of the Builder for constructing a DeleteUserRequest object.
     *
     * @return a new Builder instance for constructing DeleteUserRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link DeleteUserRequest} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The id of the user to delete.
         */
        private String userId;

        /**
         * Sets the id of the user to delete.
         *
         * @param userId the user id
         * @return the builder instance
         */
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * Builds and returns a new instance of DeleteUserRequest using the properties set on the Builder.
         *
         * @return a new DeleteUserRequest instance populated with the user id value.
         */
        public DeleteUserRequest build() {
            return new DeleteUserRequest(userId);
        }
    }
}
