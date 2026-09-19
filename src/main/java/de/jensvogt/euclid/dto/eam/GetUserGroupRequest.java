package de.jensvogt.euclid.dto.eam;

/**
 * Request for one user group, by name or by ERN.
 * <p>
 * Exactly one of the two is sent. Groups are installation-wide rather than scoped to an account, so
 * a name identifies one without further qualification - which is why a name is enough here, and an
 * ERN is offered only because that is what a grant's principal carries.
 *
 * @param ern  the ERN uniquely identifying the group, or null when it is named instead
 * @param name the group's name, used when no ERN is given
 */
public record GetUserGroupRequest(String ern, String name) {

    /**
     * Creates a new instance of the Builder for constructing a GetUserGroupRequest object.
     *
     * @return a new Builder instance for constructing GetUserGroupRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetUserGroupRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN uniquely identifying the group.
         */
        private String ern;

        /**
         * The group's name.
         */
        private String name;

        /**
         * Sets the ERN of the group.
         *
         * @param ern the ERN uniquely identifying the group
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the name of the group.
         *
         * @param name the group's name
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Builds and returns a new instance of GetUserGroupRequest using the properties set on the Builder.
         *
         * @return a new GetUserGroupRequest instance.
         */
        public GetUserGroupRequest build() {
            return new GetUserGroupRequest(ern, name);
        }
    }
}
