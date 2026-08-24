package de.jensvogt.euclid.dto.esm;

/**
 * Request to list objects in a bucket, optionally filtered and paged.
 *
 * @param bucketErn  the ERN (Entity Resource Name) of the bucket whose objects are listed
 * @param prefix     only objects whose key starts with this prefix are returned
 * @param pageSize   the maximum number of objects to return in a single page
 * @param pageIndex  the zero-based index of the page to return
 * @param sortColumn the name of the column results are sorted by
 */
public record ListObjectsRequest(String bucketErn, String prefix, long pageSize, long pageIndex, String sortColumn) {

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
         * Sets the ERN of the bucket whose objects are listed.
         *
         * @param bucketErn the bucket ERN
         * @return the builder instance
         */
        public Builder bucketErn(String bucketErn) {
            this.bucketErn = bucketErn;
            return this;
        }

        /**
         * Sets the prefix used to filter objects by key.
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
         * Builds and returns a new instance of ListObjectsRequest using the properties set on the Builder.
         *
         * @return a new ListObjectsRequest instance populated with the bucket ERN, prefix, page size, page index and sort column values.
         */
        public ListObjectsRequest build() {
            return new ListObjectsRequest(bucketErn, prefix, pageSize, pageIndex, sortColumn);
        }
    }
}
