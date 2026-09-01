package de.jensvogt.euclid.dto.ens;

/**
 * Request to list a topic's messages without receiving them, paginated.
 *
 * @param topicErn      the ERN of the topic whose messages are listed
 * @param pageSize      the maximum number of messages to return in a single page
 * @param pageIndex     the zero-based index of the page to return
 * @param sortColumn    the name of the column results are sorted by
 * @param sortDirection the sort direction, {@code "asc"} or {@code "desc"}
 */
public record ListMessagesRequest(String topicErn, long pageSize, long pageIndex, String sortColumn, String sortDirection) {

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
        private String topicErn = "";

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
         * The sort direction, {@code "asc"} or {@code "desc"}.
         */
        private String sortDirection = "asc";

        /**
         * Sets the ERN of the topic whose messages are listed.
         *
         * @param topicErn the ERN of the topic whose messages are listed
         * @return the builder instance
         */
        public Builder topicErn(String topicErn) {
            this.topicErn = topicErn;
            return this;
        }

        /**
         * Sets the maximum number of messages to return in a single page.
         *
         * @param pageSize the maximum number of messages to return in a single page
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
         * Builds and returns a new instance of ListMessagesRequest using the properties set on the Builder.
         *
         * @return a new ListMessagesRequest instance.
         */
        public ListMessagesRequest build() {
            return new ListMessagesRequest(topicErn, pageSize, pageIndex, sortColumn, sortDirection);
        }
    }
}
