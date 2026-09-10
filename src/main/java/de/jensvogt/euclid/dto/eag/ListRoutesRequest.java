package de.jensvogt.euclid.dto.eag;

/**
 * Request to list the gateway's routes.
 *
 * <p>The prefix filters on the route's <em>path</em>, not on its ID - which is what somebody asking
 * "what is published under {@code /api}" wants. It is matched literally rather than as a pattern,
 * so {@code /api/v1.0} does not also match {@code /api/v1X0}.
 *
 * <p>There is no paging here: the route table is a piece of configuration somebody wrote, not a
 * data set, and an installation with enough routes for a page boundary to matter would be one
 * nobody could reason about anyway.
 *
 * @param prefix only routes whose path starts with this are returned; empty returns all of them
 */
public record ListRoutesRequest(String prefix) {

    /**
     * Creates a new instance of the Builder for constructing a ListRoutesRequest object.
     *
     * @return a new Builder instance for constructing ListRoutesRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListRoutesRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Only routes whose path starts with this are returned.
         */
        private String prefix = "";

        /**
         * Sets the path prefix to filter by.
         *
         * @param prefix the path prefix
         * @return the builder instance
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Builds and returns a new instance of ListRoutesRequest using the properties set on the Builder.
         *
         * @return a new ListRoutesRequest instance.
         */
        public ListRoutesRequest build() {
            return new ListRoutesRequest(prefix);
        }
    }
}
