package de.jensvogt.euclid.dto.esm;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Request to list buckets, optionally filtered and paged.
 *
 * @param prefix          the text bucket names are matched against - by default, only buckets whose
 *                        name starts with it are returned; empty for every bucket
 * @param matchType       how {@code prefix} is matched: {@code "prefix"}, {@code "endsWith"},
 *                        {@code "contains"} or {@code "regex"}, spelled exactly so; null for
 *                        {@code "prefix"}. Every type but {@code "regex"} takes the text literally, so {@code "a.b"} does not also
 *                        find {@code "aXb"}. An unknown type or a regex that does not compile is
 *                        refused with HTTP 400 rather than read as "no filter"
 * @param pageSize        the maximum number of buckets to return in a single page
 * @param pageIndex       the zero-based index of the page to return
 * @param sortColumn      the name of the column results are sorted by
 * @param sortDirection   the sort direction, {@code "asc"} or {@code "desc"}
 * @param includeInternal whether euclid's own internal buckets are listed as well. Honoured for EAM
 *                        administrators only; anyone else asking gets the ordinary listing rather
 *                        than an error, since the buckets they asked about are none of theirs
 */
// The two newer fields are left out when they say nothing, so a listing that uses neither sends
// exactly what it always sent and an older installation is not handed fields it has no meaning for.
public record ListBucketsRequest(String prefix, @JsonInclude(JsonInclude.Include.NON_NULL) String matchType,
                                 long pageSize, long pageIndex, String sortColumn, String sortDirection,
                                 @JsonInclude(JsonInclude.Include.NON_DEFAULT) boolean includeInternal) {

    /**
     * Creates a new instance of the Builder for constructing a ListBucketsRequest object.
     *
     * @return a new Builder instance for constructing ListBucketsRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListBucketsRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Only buckets whose name starts with this prefix are returned.
         */
        private String prefix = "";

        /**
         * How the prefix is matched; null for a plain prefix match.
         */
        private String matchType;

        /**
         * The maximum number of buckets to return in a single page.
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
         * Whether euclid's own internal buckets are listed as well.
         */
        private boolean includeInternal;

        /**
         * Sets only buckets whose name starts with this prefix are returned.
         *
         * @param prefix only buckets whose name starts with this prefix are returned
         * @return the builder instance
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Sets how the prefix is matched against bucket names.
         *
         * @param matchType {@code "prefix"}, {@code "endsWith"}, {@code "contains"} or
         *                  {@code "regex"}; null for {@code "prefix"}
         * @return the builder instance
         */
        public Builder matchType(String matchType) {
            this.matchType = matchType;
            return this;
        }

        /**
         * Sets the maximum number of buckets to return in a single page.
         *
         * @param pageSize the maximum number of buckets to return in a single page
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
         * Sets whether euclid's own internal buckets are listed as well - for administrators only.
         *
         * @param includeInternal true to list internal buckets too
         * @return the builder instance
         */
        public Builder includeInternal(boolean includeInternal) {
            this.includeInternal = includeInternal;
            return this;
        }

        /**
         * Builds and returns a new instance of ListBucketsRequest using the properties set on the Builder.
         *
         * @return a new ListBucketsRequest instance.
         */
        public ListBucketsRequest build() {
            return new ListBucketsRequest(prefix, matchType, pageSize, pageIndex, sortColumn, sortDirection, includeInternal);
        }
    }
}
