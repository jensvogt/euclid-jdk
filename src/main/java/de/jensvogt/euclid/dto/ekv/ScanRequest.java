package de.jensvogt.euclid.dto.ekv;

/**
 * Request to read a table's items without regard to their key.
 *
 * <p>Everything a {@link QueryRequest} avoids: the whole table is read rather than one partition
 * out of an index. Fine for a small table or an export, the wrong tool for a lookup.
 *
 * @param table     the table to scan
 * @param pageSize  the maximum number of items to return; 0 means no limit
 * @param pageIndex the zero-based index of the page to return, applied when pageSize is set
 */
public record ScanRequest(String table, long pageSize, long pageIndex) {

    /**
     * Creates a new instance of the Builder for constructing a ScanRequest object.
     *
     * @return a new Builder instance for constructing ScanRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ScanRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The table to scan.
         */
        private String table = "";

        /**
         * The maximum number of items to return; 0 means no limit.
         */
        private long pageSize = 0;

        /**
         * The zero-based index of the page to return.
         */
        private long pageIndex = 0;

        /**
         * Sets the table to scan.
         *
         * @param table the table name
         * @return the builder instance
         */
        public Builder table(String table) {
            this.table = table;
            return this;
        }

        /**
         * Sets the maximum number of items to return.
         *
         * @param pageSize the page size, 0 for no limit
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
         * Builds and returns a new instance of ScanRequest using the properties set on the Builder.
         *
         * @return a new ScanRequest instance.
         */
        public ScanRequest build() {
            return new ScanRequest(table, pageSize, pageIndex);
        }
    }
}
