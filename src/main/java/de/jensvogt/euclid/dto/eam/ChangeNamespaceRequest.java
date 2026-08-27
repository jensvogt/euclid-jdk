package de.jensvogt.euclid.dto.eam;

/**
 * Request to switch the caller's active namespace for this session.
 *
 * @param namespace namespace to switch the caller's active session to; empty clears it back to unscoped
 */
public record ChangeNamespaceRequest(String namespace) {

    /**
     * Creates a new instance of the Builder for constructing a ChangeNamespaceRequest object.
     *
     * @return a new Builder instance for constructing ChangeNamespaceRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ChangeNamespaceRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The namespace to switch to.
         */
        private String namespace = "";

        /**
         * Sets the namespace to switch to.
         *
         * @param namespace the namespace to switch to; empty clears it back to unscoped
         * @return the builder instance
         */
        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        /**
         * Builds and returns a new instance of ChangeNamespaceRequest using the properties set on the Builder.
         *
         * @return a new ChangeNamespaceRequest instance.
         */
        public ChangeNamespaceRequest build() {
            return new ChangeNamespaceRequest(namespace);
        }
    }
}
