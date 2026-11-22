package de.jensvogt.euclid.dto.ekm;

/**
 * Request to list the certificates of an account and namespace.
 *
 * @param prefix        only certificates whose name matches this text, the way {@code matchType} says;
 *                      empty matches every one
 * @param matchType     how {@code prefix} is matched against each name: {@code "prefix"}, which
 *                      is also what an empty value means and what every listing did before the
 *                      field existed, {@code "endsWith"}, {@code "contains"} or {@code "regex"}.
 *                      Every type but {@code "regex"} treats the text as a literal, so
 *                      {@code "a.b"} does not also find {@code "aXb"}. An unknown type, or a
 *                      regex that does not compile, is refused with HTTP 400 rather than quietly
 *                      matching nothing
 * @param pageSize      the maximum number of certificates to return in a single page
 * @param pageIndex     the zero-based index of the page to return
 * @param sortColumn    the column results are sorted by
 * @param sortDirection the direction to sort in, {@code "asc"} or {@code "desc"}
 */
public record ListCertificatesRequest(String prefix, String matchType, long pageSize, long pageIndex,
                                      String sortColumn, String sortDirection) {

    /**
     * Creates a request that matches names by prefix - the shape this record had before
     * {@code matchType} existed, so code written against it still compiles.
     *
     * @param prefix        only certificates whose name starts with this prefix are returned
     * @param pageSize      the maximum number of certificates to return in a single page
     * @param pageIndex     the zero-based index of the page to return
     * @param sortColumn    the column results are sorted by
     * @param sortDirection the direction to sort in, {@code "asc"} or {@code "desc"}
     */
    public ListCertificatesRequest(String prefix, long pageSize, long pageIndex, String sortColumn, String sortDirection) {
        this(prefix, "", pageSize, pageIndex, sortColumn, sortDirection);
    }

    /**
     * Creates a new instance of the Builder for constructing a ListCertificatesRequest object.
     *
     * @return a new Builder instance for constructing ListCertificatesRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListCertificatesRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Only certificates whose name starts with this prefix are returned.
         */
        private String prefix = "";

        /**
         * How the prefix is matched against each name; empty is a prefix match.
         */
        private String matchType = "";

        /**
         * The maximum number of certificates to return in a single page.
         */
        private long pageSize = 10;

        /**
         * The zero-based index of the page to return.
         */
        private long pageIndex = 0;

        /**
         * The column results are sorted by.
         */
        private String sortColumn = "name";

        /**
         * The direction to sort in.
         */
        private String sortDirection = "asc";

        /**
         * Sets the name prefix to filter by.
         *
         * @param prefix the name prefix
         * @return the builder instance
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Sets how the prefix is matched against each name.
         *
         * @param matchType {@code "prefix"}, {@code "endsWith"}, {@code "contains"} or
         *                  {@code "regex"}; empty, the default, is a prefix match
         * @return the builder instance
         */
        public Builder matchType(String matchType) {
            this.matchType = matchType;
            return this;
        }

        /**
         * Sets the maximum number of certificates per page.
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
         * Sets the column results are sorted by.
         *
         * @param sortColumn the sort column
         * @return the builder instance
         */
        public Builder sortColumn(String sortColumn) {
            this.sortColumn = sortColumn;
            return this;
        }

        /**
         * Sets the direction to sort in.
         *
         * @param sortDirection {@code "asc"} or {@code "desc"}
         * @return the builder instance
         */
        public Builder sortDirection(String sortDirection) {
            this.sortDirection = sortDirection;
            return this;
        }

        /**
         * Builds and returns a new instance of ListCertificatesRequest using the properties set on the Builder.
         *
         * @return a new ListCertificatesRequest instance.
         */
        public ListCertificatesRequest build() {
            return new ListCertificatesRequest(prefix, matchType, pageSize, pageIndex, sortColumn, sortDirection);
        }
    }
}
