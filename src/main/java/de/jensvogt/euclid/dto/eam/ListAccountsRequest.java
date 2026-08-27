package de.jensvogt.euclid.dto.eam;

/**
 * Request to list accounts, optionally filtered by accountId prefix and paginated.
 *
 * @param prefix     account ID prefix
 * @param pageSize   page size
 * @param pageIndex  page index
 * @param sortColumn sorting column
 */
public record ListAccountsRequest(String prefix, long pageSize, long pageIndex, String sortColumn) {

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
         * The account ID prefix.
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
        private String sortColumn = "accountId";

        /**
         * Sets the account ID prefix.
         *
         * @param prefix the account ID prefix
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
         * Builds and returns a new instance of ListAccountsRequest using the properties set on the Builder.
         *
         * @return a new ListAccountsRequest instance.
         */
        public ListAccountsRequest build() {
            return new ListAccountsRequest(prefix, pageSize, pageIndex, sortColumn);
        }
    }
}
