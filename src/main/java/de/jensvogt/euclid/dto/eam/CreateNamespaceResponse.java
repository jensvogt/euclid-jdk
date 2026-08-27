package de.jensvogt.euclid.dto.eam;

import de.jensvogt.euclid.dto.eam.model.Namespace;

/**
 * Response returned after successfully creating a namespace.
 *
 * @param namespace the newly created namespace
 */
public record CreateNamespaceResponse(Namespace namespace) {

    /**
     * Creates a new instance of the Builder for constructing a CreateNamespaceResponse object.
     *
     * @return a new Builder instance for constructing CreateNamespaceResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CreateNamespaceResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The newly created namespace.
         */
        private Namespace namespace;

        /**
         * Sets the newly created namespace.
         *
         * @param namespace the namespace
         * @return the builder instance
         */
        public Builder namespace(Namespace namespace) {
            this.namespace = namespace;
            return this;
        }

        /**
         * Builds and returns a new instance of CreateNamespaceResponse using the properties set on the Builder.
         *
         * @return a new CreateNamespaceResponse instance.
         */
        public CreateNamespaceResponse build() {
            return new CreateNamespaceResponse(namespace);
        }
    }
}
