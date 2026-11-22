package de.jensvogt.euclid.dto.esm;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Request to list objects in a bucket, optionally filtered and paged.
 *
 * @param bucketErn          the ERN (Entity Resource Name) of the bucket whose objects are listed
 * @param prefix             the text keys are matched against - by default, only objects whose key
 *                           starts with it are returned. With a {@code delimiter}, the level to list
 *                           instead, which has to end in {@code "/"} (or be empty for the top level)
 * @param matchType          how {@code prefix} is matched: {@code "prefix"}, {@code "endsWith"},
 *                           {@code "contains"} or {@code "regex"}, spelled exactly so; null for
 *                           {@code "prefix"}. Every type but {@code "regex"} takes the text literally,
 *                           so {@code "a.b"} does not also find {@code "aXb"}. Only {@code "prefix"}
 *                           goes with a delimiter - a level is a path, not a search - and anything
 *                           else is refused with HTTP 400 rather than quietly answered with a level
 * @param pageSize           the maximum number of objects to return in a single page
 * @param pageIndex          the zero-based index of the page to return
 * @param sortColumn         the name of the column results are sorted by
 * @param sortDirection      the sort direction, {@code "asc"} or {@code "desc"}
 * @param includeDirectories whether directory keys are listed alongside real objects
 * @param delimiter          {@code "/"} to list one level rather than a whole subtree, or null.
 *                           Without it a prefix of {@code "in/"} answers with every object anywhere
 *                           beneath it, a page at a time, with no way to tell which sit in
 *                           {@code "in/"} and which further down. With it the answer is the objects
 *                           whose parent is exactly {@code "in/"}, and the directories one step below
 *                           come back separately as {@link ListObjectsResponse#commonPrefixes()} - the
 *                           S3 convention. Only {@code "/"} is accepted, and a level is always sorted
 *                           by key - {@code sortColumn} is ignored, {@code sortDirection} is not
 */
// The two newer fields are left out when they say nothing, so a listing that uses neither sends
// exactly what it always sent and an older installation is not handed fields it has no meaning for.
public record ListObjectsRequest(String bucketErn, String prefix,
                                 @JsonInclude(JsonInclude.Include.NON_NULL) String matchType, long pageSize,
                                 long pageIndex, String sortColumn, String sortDirection, boolean includeDirectories,
                                 @JsonInclude(JsonInclude.Include.NON_NULL) String delimiter) {

    /**
     * Creates a new instance of the Builder for constructing a ListObjectsRequest object.
     *
     * @return a new Builder instance for constructing ListObjectsRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListObjectsRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN (Entity Resource Name) of the bucket whose objects are listed.
         */
        private String bucketErn = "";

        /**
         * Only objects whose key starts with this prefix are returned.
         */
        private String prefix = "";

        /**
         * How the prefix is matched; null for a plain prefix match.
         */
        private String matchType;

        /**
         * The maximum number of objects to return in a single page.
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
         * Whether directory keys are listed alongside real objects.
         */
        private boolean includeDirectories = false;

        /**
         * {@code "/"} to list one level rather than a whole subtree, or null.
         */
        private String delimiter;

        /**
         * Sets the ERN (Entity Resource Name) of the bucket whose objects are listed.
         *
         * @param bucketErn the ERN (Entity Resource Name) of the bucket whose objects are listed
         * @return the builder instance
         */
        public Builder bucketErn(String bucketErn) {
            this.bucketErn = bucketErn;
            return this;
        }

        /**
         * Sets only objects whose key starts with this prefix are returned.
         *
         * @param prefix only objects whose key starts with this prefix are returned
         * @return the builder instance
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Sets how the prefix is matched against object keys.
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
         * Sets the maximum number of objects to return in a single page.
         *
         * @param pageSize the maximum number of objects to return in a single page
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
         * Sets whether directory keys are listed alongside real objects.
         *
         * @param includeDirectories whether directory keys are listed alongside real objects
         * @return the builder instance
         */
        public Builder includeDirectories(boolean includeDirectories) {
            this.includeDirectories = includeDirectories;
            return this;
        }

        /**
         * Sets the delimiter that turns the listing into one level of the bucket.
         *
         * @param delimiter {@code "/"} to list one level, or null for the whole subtree
         * @return the builder instance
         */
        public Builder delimiter(String delimiter) {
            this.delimiter = delimiter;
            return this;
        }

        /**
         * Builds and returns a new instance of ListObjectsRequest using the properties set on the Builder.
         *
         * @return a new ListObjectsRequest instance.
         */
        public ListObjectsRequest build() {
            return new ListObjectsRequest(bucketErn, prefix, matchType, pageSize, pageIndex, sortColumn, sortDirection,
                    includeDirectories, delimiter);
        }
    }
}
