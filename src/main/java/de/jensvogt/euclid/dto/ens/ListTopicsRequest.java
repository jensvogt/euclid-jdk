package de.jensvogt.euclid.dto.ens;

/**
 * Request to list topics, optionally filtered by name prefix and paginated.
 *
 * @param prefix        only topics whose name starts with this prefix are returned
 * @param pageSize      the maximum number of topics to return in a single page
 * @param pageIndex     the zero-based index of the page to return
 * @param sortColumn    the name of the column results are sorted by
 * @param sortDirection the sort direction, {@code "asc"} or {@code "desc"}
 */
public record ListTopicsRequest(String prefix, long pageSize, long pageIndex, String sortColumn, String sortDirection) {

    /**
     * Creates a new instance of the Builder for constructing a ListTopicsRequest object.
     *
     * @return a new Builder instance for constructing ListTopicsRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListTopicsRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Only topics whose name starts with this prefix are returned.
         */
        private String prefix = "";

        /**
         * The maximum number of topics to return in a single page.
         */
        private long pageSize = 10;

        /**
         * The zero-based index of the page to return.
         */
        private long pageIndex = 0;

        /**
         * The name of the column results are sorted by.
         */
        private String sortColumn = "name";

        /**
         * The sort direction, {@code "asc"} or {@code "desc"}.
         */
        private String sortDirection = "asc";

        /**
         * Sets only topics whose name starts with this prefix are returned.
         *
         * @param prefix only topics whose name starts with this prefix are returned
         * @return the builder instance
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Sets the maximum number of topics to return in a single page.
         *
         * @param pageSize the maximum number of topics to return in a single page
         * @return the builder instance
         */
        public Builder pageSize(long pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /**
         * Sets the zero-based index of the page to return.
         *
         * @param pageIndex the zero-based index of the page to return
         * @return the builder instance
         */
        public Builder pageIndex(long pageIndex) {
            this.pageIndex = pageIndex;
            return this;
        }

        /**
         * Sets the name of the column results are sorted by.
         *
         * @param sortColumn the name of the column results are sorted by
         * @return the builder instance
         */
        public Builder sortColumn(String sortColumn) {
            this.sortColumn = sortColumn;
            return this;
        }

        /**
         * Sets the sort direction, {@code "asc"} or {@code "desc"}.
         *
         * @param sortDirection the sort direction, {@code "asc"} or {@code "desc"}
         * @return the builder instance
         */
        public Builder sortDirection(String sortDirection) {
            this.sortDirection = sortDirection;
            return this;
        }

        /**
         * Builds and returns a new instance of ListTopicsRequest using the properties set on the Builder.
         *
         * @return a new ListTopicsRequest instance.
         */
        public ListTopicsRequest build() {
            return new ListTopicsRequest(prefix, pageSize, pageIndex, sortColumn, sortDirection);
        }
    }
}
