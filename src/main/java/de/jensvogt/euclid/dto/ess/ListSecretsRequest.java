package de.jensvogt.euclid.dto.ess;

/**
 * Request to list the secrets of an account and namespace.
 *
 * <p>A principal restricted to particular secrets sees those and no others. The list is filtered
 * rather than refused, so an application listing what it may read gets an answer instead of a 403 -
 * which does mean {@code total} can exceed the number of secrets actually returned.
 *
 * @param prefix        only secrets whose name starts with this prefix are returned
 * @param pageSize      the maximum number of secrets to return in a single page
 * @param pageIndex     the zero-based index of the page to return
 * @param sortColumn    the column results are sorted by
 * @param sortDirection the direction to sort in, {@code "asc"} or {@code "desc"}
 */
public record ListSecretsRequest(String prefix, long pageSize, long pageIndex, String sortColumn,
                                 String sortDirection) {

    /**
     * Creates a new instance of the Builder for constructing a ListSecretsRequest object.
     *
     * @return a new Builder instance for constructing ListSecretsRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListSecretsRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Only secrets whose name starts with this prefix are returned.
         */
        private String prefix = "";

        /**
         * The maximum number of secrets to return in a single page.
         */
        private long pageSize = 10;

        /**
         * The zero-based index of the page to return.
         */
        private long pageIndex = 0;

        /**
         * The column results are sorted by.
         */
        private String sortColumn = "name";

        /**
         * The direction to sort in.
         */
        private String sortDirection = "asc";

        /**
         * Sets the name prefix to filter by.
         *
         * @param prefix the name prefix
         * @return the builder instance
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Sets the maximum number of secrets per page.
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
         * Sets the column results are sorted by.
         *
         * @param sortColumn the sort column
         * @return the builder instance
         */
        public Builder sortColumn(String sortColumn) {
            this.sortColumn = sortColumn;
            return this;
        }

        /**
         * Sets the direction to sort in.
         *
         * @param sortDirection {@code "asc"} or {@code "desc"}
         * @return the builder instance
         */
        public Builder sortDirection(String sortDirection) {
            this.sortDirection = sortDirection;
            return this;
        }

        /**
         * Builds and returns a new instance of ListSecretsRequest using the properties set on the Builder.
         *
         * @return a new ListSecretsRequest instance.
         */
        public ListSecretsRequest build() {
            return new ListSecretsRequest(prefix, pageSize, pageIndex, sortColumn, sortDirection);
        }
    }
}
