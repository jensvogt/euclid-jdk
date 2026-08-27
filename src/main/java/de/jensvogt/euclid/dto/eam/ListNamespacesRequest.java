package de.jensvogt.euclid.dto.eam;

/**
 * Request to list namespaces under an account, optionally filtered by name prefix and paginated.
 *
 * @param accountId  only namespaces belonging to this account are returned
 * @param prefix     namespace name prefix
 * @param pageSize   page size
 * @param pageIndex  page index
 * @param sortColumn sorting column
 */
public record ListNamespacesRequest(String accountId, String prefix, long pageSize, long pageIndex, String sortColumn) {

    /**
     * Creates a new instance of the Builder for constructing a ListNamespacesRequest object.
     *
     * @return a new Builder instance for constructing ListNamespacesRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListNamespacesRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Only namespaces belonging to this account are returned.
         */
        private String accountId;

        /**
         * The namespace name prefix.
         */
        private String prefix = "";

        /**
         * The page size.
         */
        private long pageSize = 10;

        /**
         * The page index.
         */
        private long pageIndex = 0;

        /**
         * The sorting column.
         */
        private String sortColumn = "name";

        /**
         * Sets the account namespaces are listed for.
         *
         * @param accountId the account ID
         * @return the builder instance
         */
        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        /**
         * Sets the namespace name prefix.
         *
         * @param prefix the namespace name prefix
         * @return the builder instance
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Sets the page size.
         *
         * @param pageSize the page size
         * @return the builder instance
         */
        public Builder pageSize(long pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /**
         * Sets the page index.
         *
         * @param pageIndex the page index
         * @return the builder instance
         */
        public Builder pageIndex(long pageIndex) {
            this.pageIndex = pageIndex;
            return this;
        }

        /**
         * Sets the sorting column.
         *
         * @param sortColumn the sorting column
         * @return the builder instance
         */
        public Builder sortColumn(String sortColumn) {
            this.sortColumn = sortColumn;
            return this;
        }

        /**
         * Builds and returns a new instance of ListNamespacesRequest using the properties set on the Builder.
         *
         * @return a new ListNamespacesRequest instance.
         */
        public ListNamespacesRequest build() {
            return new ListNamespacesRequest(accountId, prefix, pageSize, pageIndex, sortColumn);
        }
    }
}
