package de.jensvogt.euclid.dto.ekv;

import de.jensvogt.euclid.dto.ekv.model.SortOperator;

/**
 * Request to read a table's items without regard to their key.
 *
 * <p>Everything a {@link QueryRequest} avoids: the whole table is read rather than one partition
 * out of an index. Fine for a small table or an export, the wrong tool for a lookup.
 *
 * <p>A scan can still be narrowed - to the items touched in a time window, or to those whose
 * attribute compares a certain way against a value. That answers the questions a query cannot,
 * because they name no partition: "which items changed since Friday", "which ones have status
 * OPEN". It is still a scan, and the server still reads the table to answer it; what narrowing
 * changes is how much of that comes back over the wire and how much the caller has to sift through.
 * Every narrowing field left at its default is the whole table, which is what a scan has always
 * been.
 *
 * @param table             the table to scan
 * @param pageSize          the maximum number of items to return; 0 means no limit
 * @param pageIndex         the zero-based index of the page to return, applied when pageSize is set
 * @param sortColumn        what to order the items by: {@code "pk"} for the partition key,
 *                          {@code "sk"} for the sort key, {@code "modified"} for when an item was
 *                          last written; anything else, including the default empty string, is key
 *                          order. The key is always appended as a tie-breaker, because only the key
 *                          is unique - paging through items ordered by a column with ties would
 *                          otherwise see one item twice and another not at all
 * @param sortDirection     {@code "desc"} for descending; anything else is ascending
 * @param modifiedFrom      only items modified at or after this ISO-8601 timestamp, or empty for no
 *                          lower bound
 * @param modifiedTo        only items modified at or before this ISO-8601 timestamp, or empty for no
 *                          upper bound. Refused with HTTP 400 if it lies before {@code modifiedFrom}
 * @param attributeName     the attribute to compare, or empty to compare none. Any attribute of the
 *                          item, not only a key one - a key is what a query already reaches
 * @param attributeOperator how to compare it, as a {@link SortOperator#wireValue()} - the same
 *                          vocabulary a query uses for its sort key. Required once
 *                          {@code attributeName} is set, and refused without one
 * @param attributeValue    what to compare the attribute against, or the lower bound for
 *                          {@link SortOperator#BETWEEN}
 * @param attributeUpper    the upper bound, inclusive, for {@link SortOperator#BETWEEN} only
 */
public record ScanRequest(String table, long pageSize, long pageIndex, String sortColumn, String sortDirection,
                          String modifiedFrom, String modifiedTo, String attributeName,
                          String attributeOperator, Object attributeValue, Object attributeUpper) {

    /**
     * Creates an unnarrowed request in key order - the shape this record had before scans could be
     * sorted and narrowed, so code written against it still compiles.
     *
     * @param table     the table to scan
     * @param pageSize  the maximum number of items to return; 0 means no limit
     * @param pageIndex the zero-based index of the page to return
     */
    public ScanRequest(String table, long pageSize, long pageIndex) {
        this(table, pageSize, pageIndex, "", "", "", "", "", SortOperator.NONE.wireValue(), null, null);
    }

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
         * What to order the items by; empty is key order.
         */
        private String sortColumn = "";

        /**
         * The direction to order them in; empty is ascending.
         */
        private String sortDirection = "";

        /**
         * The earliest modification time to return, empty for no lower bound.
         */
        private String modifiedFrom = "";

        /**
         * The latest modification time to return, empty for no upper bound.
         */
        private String modifiedTo = "";

        /**
         * The attribute to compare, empty for none.
         */
        private String attributeName = "";

        /**
         * How to compare it.
         */
        private String attributeOperator = SortOperator.NONE.wireValue();

        /**
         * What to compare it against.
         */
        private Object attributeValue;

        /**
         * The upper bound, for between only.
         */
        private Object attributeUpper;

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
         * Sets what to order the items by.
         *
         * @param sortColumn {@code "pk"}, {@code "sk"} or {@code "modified"}; anything else is key
         *                   order
         * @return the builder instance
         */
        public Builder sortColumn(String sortColumn) {
            this.sortColumn = sortColumn;
            return this;
        }

        /**
         * Sets the direction to order the items in.
         *
         * @param sortDirection {@code "asc"} or {@code "desc"}
         * @return the builder instance
         */
        public Builder sortDirection(String sortDirection) {
            this.sortDirection = sortDirection;
            return this;
        }

        /**
         * Only returns items modified at or after this time.
         *
         * @param modifiedFrom an ISO-8601 timestamp, or empty for no lower bound
         * @return the builder instance
         */
        public Builder modifiedFrom(String modifiedFrom) {
            this.modifiedFrom = modifiedFrom;
            return this;
        }

        /**
         * Only returns items modified at or before this time.
         *
         * @param modifiedTo an ISO-8601 timestamp, or empty for no upper bound
         * @return the builder instance
         */
        public Builder modifiedTo(String modifiedTo) {
            this.modifiedTo = modifiedTo;
            return this;
        }

        /**
         * Sets the attribute to compare.
         *
         * @param attributeName any attribute of the item, or empty to compare none
         * @return the builder instance
         */
        public Builder attributeName(String attributeName) {
            this.attributeName = attributeName;
            return this;
        }

        /**
         * Sets how to compare the attribute. {@link SortOperator#BEGINS_WITH} only applies to a
         * string value, and is refused for anything else.
         *
         * @param attributeOperator the operator; required once an attribute is named
         * @return the builder instance
         */
        public Builder attributeOperator(SortOperator attributeOperator) {
            this.attributeOperator = attributeOperator == null
                    ? SortOperator.NONE.wireValue() : attributeOperator.wireValue();
            return this;
        }

        /**
         * Sets what to compare the attribute against, or the lower bound for
         * {@link SortOperator#BETWEEN}.
         *
         * @param attributeValue a {@link String}, number or other JSON value
         * @return the builder instance
         */
        public Builder attributeValue(Object attributeValue) {
            this.attributeValue = attributeValue;
            return this;
        }

        /**
         * Sets the inclusive upper bound, for {@link SortOperator#BETWEEN} only.
         *
         * @param attributeUpper the upper bound
         * @return the builder instance
         */
        public Builder attributeUpper(Object attributeUpper) {
            this.attributeUpper = attributeUpper;
            return this;
        }

        /**
         * Narrows the scan to items whose attribute compares a certain way against a value - the
         * three settings that only mean something together, in one call.
         *
         * @param name     the attribute to compare
         * @param operator how to compare it
         * @param value    what to compare it against, or the lower bound for
         *                 {@link SortOperator#BETWEEN}
         * @return the builder instance
         */
        public Builder where(String name, SortOperator operator, Object value) {
            return attributeName(name).attributeOperator(operator).attributeValue(value);
        }

        /**
         * Builds and returns a new instance of ScanRequest using the properties set on the Builder.
         *
         * @return a new ScanRequest instance.
         */
        public ScanRequest build() {
            return new ScanRequest(table, pageSize, pageIndex, sortColumn, sortDirection, modifiedFrom, modifiedTo,
                    attributeName, attributeOperator, attributeValue, attributeUpper);
        }
    }
}
