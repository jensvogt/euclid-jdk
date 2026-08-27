package de.jensvogt.euclid.dto.eam;

import de.jensvogt.euclid.dto.eam.model.Namespace;

import java.util.List;

/**
 * Response returned from list-namespaces.
 *
 * @param namespaces the namespaces
 * @param total      total number of namespaces
 */
public record ListNamespacesResponse(List<Namespace> namespaces, long total) {

    /**
     * Creates a new instance of the Builder for constructing a ListNamespacesResponse object.
     *
     * @return a new Builder instance for constructing ListNamespacesResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListNamespacesResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The namespaces.
         */
        private List<Namespace> namespaces;

        /**
         * The total number of namespaces.
         */
        private long total;

        /**
         * Sets the namespaces.
         *
         * @param namespaces the namespaces
         * @return the builder instance
         */
        public Builder namespaces(List<Namespace> namespaces) {
            this.namespaces = namespaces;
            return this;
        }

        /**
         * Sets the total number of namespaces.
         *
         * @param total the total number of namespaces
         * @return the builder instance
         */
        public Builder total(long total) {
            this.total = total;
            return this;
        }

        /**
         * Builds and returns a new instance of ListNamespacesResponse using the properties set on the Builder.
         *
         * @return a new ListNamespacesResponse instance.
         */
        public ListNamespacesResponse build() {
            return new ListNamespacesResponse(namespaces, total);
        }
    }
}
