package de.jensvogt.euclid.dto.eqs;

/**
 * Request to list messages in a queue, paged.
 *
 * @param queueErn   the ERN (Entity Resource Name) of the queue whose messages are listed
 * @param pageSize   the maximum number of messages to return in a single page
 * @param pageIndex  the zero-based index of the page to return
 * @param sortColumn the name of the column results are sorted by
 */
public record ListMessagesRequest(String queueErn, long pageSize, long pageIndex, String sortColumn) {

    /**
     * Creates a new instance of the Builder for constructing a ListMessagesRequest object.
     *
     * @return a new Builder instance for constructing ListMessagesRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListMessagesRequest} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN (Entity Resource Name) of the queue whose messages are listed.
         */
        private String queueErn;

        /**
         * The maximum number of messages to return in a single page.
         */
        private long pageSize = 10;

        /**
         * The zero-based index of the page to return.
         */
        private long pageIndex = 0;

        /**
         * The name of the column results are sorted by.
         */
        private String sortColumn = "created";

        /**
         * Sets the ERN of the queue whose messages are listed.
         *
         * @param queueErn the queue ERN
         * @return the builder instance
         */
        public Builder queueErn(String queueErn) {
            this.queueErn = queueErn;
            return this;
        }

        /**
         * Sets the maximum number of messages to return in a single page.
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
         * Builds and returns a new instance of ListMessagesRequest using the properties set on the Builder.
         *
         * @return a new ListMessagesRequest instance populated with the queue ERN, page size, page index and sort column values.
         */
        public ListMessagesRequest build() {
            return new ListMessagesRequest(queueErn, pageSize, pageIndex, sortColumn);
        }
    }
}
