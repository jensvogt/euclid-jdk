package de.jensvogt.euclid.dto.eam;

import java.util.List;

/**
 * Request to define a role, or to redefine one that exists.
 *
 * <p>The same shape serves {@code create-role} and {@code update-role}, because the server reads the
 * same three fields for both. Update <em>replaces</em> rather than merges: a permission left out is
 * taken away, which is the only way to narrow a role at all.
 *
 * <p>A role with no permissions is refused with HTTP 400 - it would grant nothing, and an empty
 * list is far more often a caller who meant to send something. A name euclid ships as a built-in is
 * refused too: creating one is HTTP 409, changing or deleting one is HTTP 403.
 *
 * @param name        the role's name, unique within the account
 * @param description what the role is for
 * @param permissions the permissions it carries; at least one, and each has to be one a module
 *                    answers - see
 *                    {@link de.jensvogt.euclid.module.eam.EuclidSession#listPermissions()}
 */
public record RoleRequest(String name, String description, List<String> permissions) {

    /**
     * Creates a builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link RoleRequest}.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        private String name = "";
        private String description = "";
        private List<String> permissions = List.of();

        /**
         * Sets the role's name.
         *
         * @param name the role name
         * @return the builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets what the role is for.
         *
         * @param description the description
         * @return the builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the permissions it carries, replacing whatever it had.
         *
         * @param permissions the permissions; at least one
         * @return the builder
         */
        public Builder permissions(List<String> permissions) {
            this.permissions = permissions;
            return this;
        }

        /**
         * Builds the request.
         *
         * @return the request
         */
        public RoleRequest build() {
            return new RoleRequest(name, description, permissions);
        }
    }
}
