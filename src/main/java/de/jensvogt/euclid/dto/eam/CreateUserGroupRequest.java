package de.jensvogt.euclid.dto.eam;

/**
 * Request to create a new, empty user group.
 *
 * @param name        group name, unique across the deployment
 * @param description free-text description of the group's purpose
 */
public record CreateUserGroupRequest(String name, String description) {

    /**
     * Creates a new instance of the Builder for constructing a CreateUserGroupRequest object.
     *
     * @return a new Builder instance for constructing CreateUserGroupRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CreateUserGroupRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The group name.
         */
        private String name;

        /**
         * The group description.
         */
        private String description = "";

        /**
         * Sets the group name.
         *
         * @param name the group name
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the group description.
         *
         * @param description the group description
         * @return the builder instance
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Builds and returns a new instance of CreateUserGroupRequest using the properties set on the Builder.
         *
         * @return a new CreateUserGroupRequest instance.
         */
        public CreateUserGroupRequest build() {
            return new CreateUserGroupRequest(name, description);
        }
    }
}
