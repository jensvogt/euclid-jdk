package de.jensvogt.euclid.dto.eam;

import java.util.List;

/**
 * Request to give a role to a user or a user group, scoped.
 *
 * <p>Replaced {@code GrantNamespaceAccessRequest}: access to a namespace is now a role granted in
 * it, so the same call says <em>what</em> the principal may do there as well as <em>where</em>.
 *
 * @param role       a role of the account, or a built-in one - {@code account-administrator},
 *                   {@code operator}, {@code reader}, {@code publisher}, {@code consumer},
 *                   {@code application}
 * @param principal  a user ERN or a user-group ERN. One field for both, because the ERN says which
 * @param accountId  the account to grant in; the caller's own when empty. Naming another needs
 *                   administrator rights on it
 * @param namespaces namespaces of the account it applies in; a single "*" means every one of them
 * @param resources  ERN patterns it applies to, each exact or ending in "*"; a single "*" means
 *                   every resource
 */
public record GrantRoleRequest(String role, String principal, String accountId, List<String> namespaces,
                               List<String> resources) {

    /**
     * Creates a builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link GrantRoleRequest}. Namespaces and resources default to everything.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        private String role = "";
        private String principal = "";
        private String accountId = "";
        private List<String> namespaces = List.of("*");
        private List<String> resources = List.of("*");

        /**
         * Sets the role to grant.
         *
         * @param role the role name
         * @return the builder
         */
        public Builder role(String role) {
            this.role = role;
            return this;
        }

        /**
         * Sets who receives it.
         *
         * @param principal a user or user-group ERN
         * @return the builder
         */
        public Builder principal(String principal) {
            this.principal = principal;
            return this;
        }

        /**
         * Sets the account to grant in.
         *
         * @param accountId the account to grant in, or empty for the caller's own
         * @return the builder
         */
        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        /**
         * Sets the namespaces it applies in.
         *
         * @param namespaces namespaces it applies in
         * @return the builder
         */
        public Builder namespaces(List<String> namespaces) {
            this.namespaces = namespaces;
            return this;
        }

        /**
         * Sets the resources it applies to.
         *
         * @param resources ERN patterns it applies to
         * @return the builder
         */
        public Builder resources(List<String> resources) {
            this.resources = resources;
            return this;
        }

        /**
         * Builds the request.
         *
         * @return the request
         */
        public GrantRoleRequest build() {
            return new GrantRoleRequest(role, principal, accountId, namespaces, resources);
        }
    }
}
