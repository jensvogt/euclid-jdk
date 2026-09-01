package de.jensvogt.euclid.dto.eam;

/**
 * Request to list accounts, optionally filtered by accountId prefix and paginated.
 *
 * @param prefix        account ID prefix
 * @param pageSize      page size
 * @param pageIndex     page index
 * @param sortColumn    sorting column
 * @param sortDirection sort direction, {@code "asc"} or {@code "desc"}
 */
public record ListAccountsRequest(String prefix, long pageSize, long pageIndex, String sortColumn, String sortDirection) {

    /**
     * Creates a new instance of the Builder for constructing a ListAccountsRequest object.
     *
     * @return a new Builder instance for constructing ListAccountsRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListAccountsRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Account ID prefix.
         */
        private String prefix = "";

        /**
         * Page size.
         */
        private long pageSize = 10;

        /**
         * Page index.
         */
        private long pageIndex = 0;

        /**
         * Sorting column.
         */
        private String sortColumn = "name";

        /**
         * Sort direction, {@code "asc"} or {@code "desc"}.
         */
        private String sortDirection = "asc";

        /**
         * Sets account ID prefix.
         *
         * @param prefix account ID prefix
         * @return the builder instance
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Sets page size.
         *
         * @param pageSize page size
         * @return the builder instance
         */
        public Builder pageSize(long pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /**
         * Sets page index.
         *
         * @param pageIndex page index
         * @return the builder instance
         */
        public Builder pageIndex(long pageIndex) {
            this.pageIndex = pageIndex;
            return this;
        }

        /**
         * Sets sorting column.
         *
         * @param sortColumn sorting column
         * @return the builder instance
         */
        public Builder sortColumn(String sortColumn) {
            this.sortColumn = sortColumn;
            return this;
        }

        /**
         * Sets sort direction, {@code "asc"} or {@code "desc"}.
         *
         * @param sortDirection sort direction, {@code "asc"} or {@code "desc"}
         * @return the builder instance
         */
        public Builder sortDirection(String sortDirection) {
            this.sortDirection = sortDirection;
            return this;
        }

        /**
         * Builds and returns a new instance of ListAccountsRequest using the properties set on the Builder.
         *
         * @return a new ListAccountsRequest instance.
         */
        public ListAccountsRequest build() {
            return new ListAccountsRequest(prefix, pageSize, pageIndex, sortColumn, sortDirection);
        }
    }
}
