package de.jensvogt.euclid.dto.eam;

/**
 * Request to delete an existing user group.
 *
 * @param name group name to delete
 */
public record DeleteUserGroupRequest(String name) {

    /**
     * Creates a new instance of the Builder for constructing a DeleteUserGroupRequest object.
     *
     * @return a new Builder instance for constructing DeleteUserGroupRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link DeleteUserGroupRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The group name to delete.
         */
        private String name;

        /**
         * Sets the group name to delete.
         *
         * @param name the group name
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Builds and returns a new instance of DeleteUserGroupRequest using the properties set on the Builder.
         *
         * @return a new DeleteUserGroupRequest instance.
         */
        public DeleteUserGroupRequest build() {
            return new DeleteUserGroupRequest(name);
        }
    }
}
