package de.jensvogt.euclid.dto.eam;

/**
 * Request to list an account's roles.
 *
 * @param prefix         only roles whose name starts with this; empty matches all
 * @param pageSize       the maximum number of stored roles to return in a single page
 * @param pageIndex      the zero-based index of the page to return
 * @param sortColumn     the column results are sorted by
 * @param sortDirection  the direction to sort in, {@code "asc"} or {@code "desc"}
 * @param includeBuiltin whether the roles euclid ships are listed alongside the account's own.
 *                       They are not stored, so they sit outside the paging and arrive first - a
 *                       caller listing roles almost always wants to see what it can bind
 */
public record ListRolesRequest(String prefix, long pageSize, long pageIndex, String sortColumn, String sortDirection,
                               boolean includeBuiltin) {

    /**
     * Creates a builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ListRolesRequest}. Built-in roles are included unless asked otherwise.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        private String prefix = "";
        private long pageSize = 10;
        private long pageIndex = 0;
        private String sortColumn = "name";
        private String sortDirection = "asc";
        private boolean includeBuiltin = true;

        /**
         * Sets the name prefix to filter by.
         *
         * @param prefix the name prefix
         * @return the builder
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Sets the maximum number of stored roles per page.
         *
         * @param pageSize the page size
         * @return the builder
         */
        public Builder pageSize(long pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /**
         * Sets the zero-based index of the page to return.
         *
         * @param pageIndex the page index
         * @return the builder
         */
        public Builder pageIndex(long pageIndex) {
            this.pageIndex = pageIndex;
            return this;
        }

        /**
         * Sets the column results are sorted by.
         *
         * @param sortColumn the sort column
         * @return the builder
         */
        public Builder sortColumn(String sortColumn) {
            this.sortColumn = sortColumn;
            return this;
        }

        /**
         * Sets the direction to sort in.
         *
         * @param sortDirection {@code "asc"} or {@code "desc"}
         * @return the builder
         */
        public Builder sortDirection(String sortDirection) {
            this.sortDirection = sortDirection;
            return this;
        }

        /**
         * Sets whether the roles euclid ships are listed too.
         *
         * @param includeBuiltin {@code false} for the account's own roles alone
         * @return the builder
         */
        public Builder includeBuiltin(boolean includeBuiltin) {
            this.includeBuiltin = includeBuiltin;
            return this;
        }

        /**
         * Builds the request.
         *
         * @return the request
         */
        public ListRolesRequest build() {
            return new ListRolesRequest(prefix, pageSize, pageIndex, sortColumn, sortDirection, includeBuiltin);
        }
    }
}
