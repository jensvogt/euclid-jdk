package de.jensvogt.euclid.dto.eqs;

/**
 * Request to list queues, optionally filtered and paged.
 *
 * @param prefix     only queues whose name starts with this prefix are returned
 * @param pageSize   the maximum number of queues to return in a single page
 * @param pageIndex  the zero-based index of the page to return
 * @param sortColumn the name of the column results are sorted by
 */
public record ListQueueRequest(String prefix, long pageSize, long pageIndex, String sortColumn) {

    /**
     * Creates a new instance of the Builder for constructing a ListQueueRequest object.
     *
     * @return a new Builder instance for constructing ListQueueRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListQueueRequest} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Only queues whose name starts with this prefix are returned.
         */
        private String prefix = "";

        /**
         * The maximum number of queues to return in a single page.
         */
        private long pageSize = 10;

        /**
         * The zero-based index of the page to return.
         */
        private long pageIndex = 0;

        /**
         * The name of the column results are sorted by.
         */
        private String sortColumn = "userId";

        /**
         * Sets the prefix used to filter queues by name.
         *
         * @param prefix only queues whose name starts with this prefix are returned
         * @return the builder instance
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Sets the maximum number of queues to return in a single page.
         *
         * @param pageSize the page size
         * @return the builder instance
         */
        public Builder pageSize(long pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /**
         * Sets the zero-based index of the page to return.
         *
         * @param pageIndex the page index
         * @return the builder instance
         */
        public Builder pageIndex(long pageIndex) {
            this.pageIndex = pageIndex;
            return this;
        }

        /**
         * Sets the name of the column results are sorted by.
         *
         * @param sortColumn the sort column name
         * @return the builder instance
         */
        public Builder sortColumn(String sortColumn) {
            this.sortColumn = sortColumn;
            return this;
        }

        /**
         * Builds and returns a new instance of ListQueueRequest using the properties set on the Builder.
         *
         * @return a new ListQueueRequest instance populated with the prefix, page size, page index and sort column values.
         */
        public ListQueueRequest build() {
            return new ListQueueRequest(prefix, pageSize, pageIndex, sortColumn);
        }
    }
}
