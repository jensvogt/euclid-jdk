package de.jensvogt.euclid.dto.eqs;

/**
 * Request to list queues, optionally filtered by name prefix and paginated.
 *
 * @param prefix        only queues whose name matches this are returned, the way {@code matchType} says
 * @param matchType     how {@code prefix} is matched: {@code "prefix"}, {@code "endsWith"},
 *                      {@code "contains"} or {@code "regex"}; empty means {@code "prefix"}, and every type
 *                      but {@code "regex"} takes the text literally
 * @param pageSize      the maximum number of queues to return in a single page
 * @param pageIndex     the zero-based index of the page to return
 * @param sortColumn    the name of the column results are sorted by
 * @param sortDirection the sort direction, {@code "asc"} or {@code "desc"}
 * @param includeInternal whether euclid's own queues are listed as well; they are hidden by default
 */
public record ListQueueRequest(String prefix, String matchType, long pageSize, long pageIndex, String sortColumn, String sortDirection,
                               boolean includeInternal) {

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
         * How {@code prefix} is matched; empty is the server's default, {@code "prefix"}.
         */
        private String matchType = "";

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
        private String sortColumn = "name";

        /**
         * The sort direction, {@code "asc"} or {@code "desc"}.
         */
        private String sortDirection = "asc";

        /**
         * Whether euclid's own queues are listed as well.
         */
        private boolean includeInternal = false;

        /**
         * Sets only queues whose name starts with this prefix are returned.
         *
         * @param prefix only queues whose name starts with this prefix are returned
         * @return the builder instance
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Sets how {@code prefix} is matched against queue names.
         *
         * <p>Named by the server rather than by an enum here, so a match type a newer server adds
         * can be asked for without waiting for this client to learn it; one the server does not
         * know is refused with HTTP 400 rather than silently treated as a prefix.
         *
         * @param matchType {@code "prefix"}, {@code "endsWith"}, {@code "contains"} or {@code "regex"};
         *                  {@code null} or empty means {@code "prefix"}
         * @return the builder instance
         */
        public Builder matchType(String matchType) {
            this.matchType = matchType == null ? "" : matchType;
            return this;
        }

        /**
         * Sets the maximum number of queues to return in a single page.
         *
         * @param pageSize the maximum number of queues to return in a single page
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
         * Sets whether euclid's own queues are listed as well.
         *
         * <p>An internal queue is one a component made for itself rather than one somebody asked
         * for - the delivery queue behind a bucket listener, for instance. They are left out of a
         * listing by default, so that a listing shows what a person would recognise. A component
         * looking for its own queues has to ask for them.
         *
         * @param includeInternal whether euclid's own queues are listed as well
         * @return the builder instance
         */
        public Builder includeInternal(boolean includeInternal) {
            this.includeInternal = includeInternal;
            return this;
        }

        /**
         * Builds and returns a new instance of ListQueueRequest using the properties set on the Builder.
         *
         * @return a new ListQueueRequest instance.
         */
        public ListQueueRequest build() {
            return new ListQueueRequest(prefix, matchType, pageSize, pageIndex, sortColumn, sortDirection, includeInternal);
        }
    }
}
