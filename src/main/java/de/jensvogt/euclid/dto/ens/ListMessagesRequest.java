package de.jensvogt.euclid.dto.ens;

/**
 * Request to list a topic's messages without receiving them, optionally paginated.
 *
 * @param topicErn   ERN of the topic whose messages are listed
 * @param pageSize   page size
 * @param pageIndex  page index
 * @param sortColumn sorting column
 */
public record ListMessagesRequest(String topicErn, long pageSize, long pageIndex, String sortColumn) {

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
         * The ERN of the topic whose messages are listed.
         */
        private String topicErn;

        /**
         * The page size.
         */
        private long pageSize = 10;

        /**
         * The page index.
         */
        private long pageIndex = 0;

        /**
         * The sorting column.
         */
        private String sortColumn = "created";

        /**
         * Sets the ERN of the topic whose messages are listed.
         *
         * @param topicErn the topic ERN
         * @return the builder instance
         */
        public Builder topicErn(String topicErn) {
            this.topicErn = topicErn;
            return this;
        }

        /**
         * Sets the page size.
         *
         * @param pageSize the page size
         * @return the builder instance
         */
        public Builder pageSize(long pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /**
         * Sets the page index.
         *
         * @param pageIndex the page index
         * @return the builder instance
         */
        public Builder pageIndex(long pageIndex) {
            this.pageIndex = pageIndex;
            return this;
        }

        /**
         * Sets the sorting column.
         *
         * @param sortColumn the sorting column
         * @return the builder instance
         */
        public Builder sortColumn(String sortColumn) {
            this.sortColumn = sortColumn;
            return this;
        }

        /**
         * Builds and returns a new instance of ListMessagesRequest using the properties set on the Builder.
         *
         * @return a new ListMessagesRequest instance.
         */
        public ListMessagesRequest build() {
            return new ListMessagesRequest(topicErn, pageSize, pageIndex, sortColumn);
        }
    }
}
