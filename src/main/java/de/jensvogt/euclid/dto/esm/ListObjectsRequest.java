package de.jensvogt.euclid.dto.esm;

/**
 * Request to list objects in a bucket, optionally filtered and paged.
 *
 * @param bucketErn          the ERN (Entity Resource Name) of the bucket whose objects are listed
 * @param prefix             only objects whose key starts with this prefix are returned
 * @param pageSize           the maximum number of objects to return in a single page
 * @param pageIndex          the zero-based index of the page to return
 * @param sortColumn         the name of the column results are sorted by
 * @param sortDirection      the sort direction, {@code "asc"} or {@code "desc"}
 * @param includeDirectories whether directory keys are listed alongside real objects
 */
public record ListObjectsRequest(String bucketErn, String prefix, long pageSize, long pageIndex, String sortColumn,
                                 String sortDirection, boolean includeDirectories) {

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
         * Builds and returns a new instance of ListObjectsRequest using the properties set on the Builder.
         *
         * @return a new ListObjectsRequest instance.
         */
        public ListObjectsRequest build() {
            return new ListObjectsRequest(bucketErn, prefix, pageSize, pageIndex, sortColumn, sortDirection, includeDirectories);
        }
    }
}
