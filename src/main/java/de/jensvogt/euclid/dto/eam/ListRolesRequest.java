package de.jensvogt.euclid.dto.eam;

/**
 * Request to list an account's roles.
 *
 * @param prefix         the text to filter role names by, matched as {@code matchType} says;
 *                       empty matches all
 * @param matchType     how {@code prefix} is matched against the name: {@code "prefix"},
 *                      {@code "endsWith"}, {@code "contains"} or {@code "regex"}; empty means
 *                      {@code "prefix"}, which is what every listing did before the choice existed.
 *                      Every type but {@code "regex"} takes the text literally, so a search for
 *                      {@code "a.b"} does not also find {@code "aXb"}; a pattern the server cannot
 *                      use is refused with HTTP 400
 * @param pageSize       the maximum number of stored roles to return in a single page
 * @param pageIndex      the zero-based index of the page to return
 * @param sortColumn     the column results are sorted by
 * @param sortDirection  the direction to sort in, {@code "asc"} or {@code "desc"}
 * @param includeBuiltin whether the roles euclid ships are listed alongside the account's own.
 *                       They are not stored, so they sit outside the paging and arrive first - a
 *                       caller listing roles almost always wants to see what it can bind
 */
public record ListRolesRequest(String prefix, String matchType, long pageSize, long pageIndex, String sortColumn, String sortDirection,
                               boolean includeBuiltin) {

    /**
     * Creates a request that matches {@code prefix} as a prefix - the shape this request had
     * before {@code matchType} existed, kept so code written against it goes on compiling.
     *
     * @param prefix see the record component of the same name
     * @param pageSize see the record component of the same name
     * @param pageIndex see the record component of the same name
     * @param sortColumn see the record component of the same name
     * @param sortDirection see the record component of the same name
     * @param includeBuiltin see the record component of the same name
     */
    public ListRolesRequest(String prefix, long pageSize, long pageIndex, String sortColumn, String sortDirection, boolean includeBuiltin) {
        this(prefix, "", pageSize, pageIndex, sortColumn, sortDirection, includeBuiltin);
    }

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

        /**
         * How the prefix is matched; empty means as a prefix.
         */
        private String matchType = "";
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
         * Sets how the prefix is matched: {@code "prefix"}, {@code "endsWith"}, {@code "contains"}
         * or {@code "regex"}.
         *
         * @param matchType the match type; empty means {@code "prefix"}
         * @return the builder instance
         */
        public Builder matchType(String matchType) {
            this.matchType = matchType;
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
            return new ListRolesRequest(prefix, matchType, pageSize, pageIndex, sortColumn, sortDirection, includeBuiltin);
        }
    }
}
