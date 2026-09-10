package de.jensvogt.euclid.dto.ekv;

import de.jensvogt.euclid.dto.ekv.model.SortOperator;

/**
 * Request to read the items of one partition, in sort-key order.
 *
 * <p>A query names one partition exactly - that is what makes it a query rather than a
 * {@link ScanRequest} - and may then narrow it by sort key. Narrowing at all needs a table that
 * declares a sort key; asking a table without one for anything but
 * {@link SortOperator#NONE} is refused.
 *
 * @param table         the table to query
 * @param partitionKey  the partition key's value, a {@link String} or a number to match the type
 *                      the table declared for it
 * @param sortOperator  how to narrow by sort key, as a {@link SortOperator#wireValue()}; empty
 *                      takes the whole partition
 * @param sortValue     what to compare the sort key against, or the lower bound for
 *                      {@link SortOperator#BETWEEN}
 * @param sortUpper     the upper bound, inclusive, for {@link SortOperator#BETWEEN} only
 * @param forward       whether to read in ascending sort-key order
 * @param pageSize      the maximum number of items to return; 0 means no limit
 * @param pageIndex     the zero-based index of the page to return, applied when pageSize is set
 */
public record QueryRequest(String table, Object partitionKey, String sortOperator, Object sortValue,
                           Object sortUpper, boolean forward, long pageSize, long pageIndex) {

    /**
     * Creates a new instance of the Builder for constructing a QueryRequest object.
     *
     * @return a new Builder instance for constructing QueryRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link QueryRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The table to query.
         */
        private String table = "";

        /**
         * The partition key's value.
         */
        private Object partitionKey;

        /**
         * How to narrow by sort key.
         */
        private String sortOperator = SortOperator.NONE.wireValue();

        /**
         * What to compare the sort key against.
         */
        private Object sortValue;

        /**
         * The upper bound, for between only.
         */
        private Object sortUpper;

        /**
         * Whether to read in ascending sort-key order.
         */
        private boolean forward = true;

        /**
         * The maximum number of items to return; 0 means no limit.
         */
        private long pageSize = 0;

        /**
         * The zero-based index of the page to return.
         */
        private long pageIndex = 0;

        /**
         * Sets the table to query.
         *
         * @param table the table name
         * @return the builder instance
         */
        public Builder table(String table) {
            this.table = table;
            return this;
        }

        /**
         * Sets the partition key's value.
         *
         * @param partitionKey the value, of the type the table declared for the partition key
         * @return the builder instance
         */
        public Builder partitionKey(Object partitionKey) {
            this.partitionKey = partitionKey;
            return this;
        }

        /**
         * Sets how to narrow by sort key, defaulting to {@link SortOperator#NONE}.
         *
         * @param sortOperator the operator
         * @return the builder instance
         */
        public Builder sortOperator(SortOperator sortOperator) {
            this.sortOperator = sortOperator == null ? SortOperator.NONE.wireValue() : sortOperator.wireValue();
            return this;
        }

        /**
         * Sets what to compare the sort key against, or the lower bound for
         * {@link SortOperator#BETWEEN}.
         *
         * @param sortValue the value, of the type the table declared for the sort key
         * @return the builder instance
         */
        public Builder sortValue(Object sortValue) {
            this.sortValue = sortValue;
            return this;
        }

        /**
         * Sets the inclusive upper bound, for {@link SortOperator#BETWEEN} only.
         *
         * @param sortUpper the upper bound
         * @return the builder instance
         */
        public Builder sortUpper(Object sortUpper) {
            this.sortUpper = sortUpper;
            return this;
        }

        /**
         * Sets whether to read in ascending sort-key order.
         *
         * @param forward {@code true} for ascending, {@code false} for descending
         * @return the builder instance
         */
        public Builder forward(boolean forward) {
            this.forward = forward;
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
         * Builds and returns a new instance of QueryRequest using the properties set on the Builder.
         *
         * @return a new QueryRequest instance.
         */
        public QueryRequest build() {
            return new QueryRequest(table, partitionKey, sortOperator, sortValue, sortUpper, forward, pageSize,
                    pageIndex);
        }
    }
}
