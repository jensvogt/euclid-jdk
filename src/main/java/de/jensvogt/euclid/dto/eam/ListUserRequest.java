package de.jensvogt.euclid.dto.eam;

/**
 * Request to list users, optionally filtered by user ID prefix and paginated.
 *
 * @param prefix        user ID prefix
 * @param pageSize      page size
 * @param pageIndex     page index
 * @param sortColumn    sorting column
 * @param sortDirection sort direction, {@code "asc"} or {@code "desc"}
 */
public record ListUserRequest(String prefix, long pageSize, long pageIndex, String sortColumn, String sortDirection) {

    /**
     * Creates a new instance of the Builder for constructing a ListUserRequest object.
     *
     * @return a new Builder instance for constructing ListUserRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListUserRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * User ID prefix.
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
        private String sortColumn = "userId";

        /**
         * Sort direction, {@code "asc"} or {@code "desc"}.
         */
        private String sortDirection = "asc";

        /**
         * Sets user ID prefix.
         *
         * @param prefix user ID prefix
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
         * Builds and returns a new instance of ListUserRequest using the properties set on the Builder.
         *
         * @return a new ListUserRequest instance.
         */
        public ListUserRequest build() {
            return new ListUserRequest(prefix, pageSize, pageIndex, sortColumn, sortDirection);
        }
    }
}
