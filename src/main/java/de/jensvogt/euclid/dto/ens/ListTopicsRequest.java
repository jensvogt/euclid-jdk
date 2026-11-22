package de.jensvogt.euclid.dto.ens;

/**
 * Request to list topics, optionally filtered by name and paginated.
 * <p>
 * The name filter is {@code prefix}, and {@code matchType} says how it is compared. Both are carried
 * as the server reads them rather than folded into one pattern, so a literal {@code "orders.v1"}
 * means exactly that for every type but {@code "regex"} - there is nothing to escape.
 *
 * @param prefix        the text topic names are matched against; empty matches every topic
 * @param matchType     how {@code prefix} is matched: {@code "prefix"}, {@code "endsWith"},
 *                      {@code "contains"} or {@code "regex"}; empty means {@code "prefix"}
 * @param pageSize      the maximum number of topics to return in a single page
 * @param pageIndex     the zero-based index of the page to return
 * @param sortColumn    the name of the column results are sorted by
 * @param sortDirection the sort direction, {@code "asc"} or {@code "desc"}
 */
public record ListTopicsRequest(String prefix, String matchType, long pageSize, long pageIndex, String sortColumn,
                                String sortDirection) {

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
         * The text topic names are matched against.
         */
        private String prefix = "";

        /**
         * How the prefix is matched; empty leaves it to the server, which reads it as a prefix.
         */
        private String matchType = "";

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
         * Sets the text topic names are matched against - see {@link #matchType(String)}.
         *
         * @param prefix the text topic names are matched against; empty matches every topic
         * @return the builder instance
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Sets how {@code prefix} is matched against topic names.
         * <p>
         * {@code "prefix"}, {@code "endsWith"} and {@code "contains"} take the text literally;
         * {@code "regex"} reads it as an ECMAScript regular expression, which the server compiles
         * before it queries anything - an invalid one is refused with HTTP 400 rather than
         * matching nothing, so a typo cannot pass for an empty account. Anything else is refused
         * the same way.
         *
         * @param matchType {@code "prefix"}, {@code "endsWith"}, {@code "contains"} or {@code "regex"};
         *                  empty means {@code "prefix"}
         * @return the builder instance
         */
        public Builder matchType(String matchType) {
            this.matchType = matchType;
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
            return new ListTopicsRequest(prefix, matchType, pageSize, pageIndex, sortColumn, sortDirection);
        }
    }
}
