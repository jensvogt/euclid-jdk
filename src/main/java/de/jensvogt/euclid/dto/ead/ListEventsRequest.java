package de.jensvogt.euclid.dto.ead;

/**
 * Request for a page of the audit trail, newest first.
 *
 * <p>The three filters are sent only when set: EAD reads an absent filter as "no filter", and a
 * {@code null} here is left out of the request entirely rather than matched as an empty string.
 * Whatever the filters say, the result is confined to the caller's own account - the server never
 * reads an account from the body.
 *
 * @param userId    only entries of this user, or {@code null} for every user
 * @param module    only entries addressed to this module, e.g. {@code "esm"}, or {@code null} for all
 * @param command   only entries of this command, e.g. {@code "delete-bucket"}, or {@code null} for all
 * @param pageSize  the maximum number of entries to return in a single page
 * @param pageIndex the zero-based index of the page to return
 */
public record ListEventsRequest(String userId, String module, String command, long pageSize, long pageIndex) {

    /**
     * Creates a new instance of the Builder for constructing a ListEventsRequest object.
     *
     * @return a new Builder instance for constructing ListEventsRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListEventsRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Only entries of this user.
         */
        private String userId;

        /**
         * Only entries addressed to this module.
         */
        private String module;

        /**
         * Only entries of this command.
         */
        private String command;

        /**
         * The maximum number of entries to return in a single page.
         */
        private long pageSize = 10;

        /**
         * The zero-based index of the page to return.
         */
        private long pageIndex = 0;

        /**
         * Sets the user to filter by.
         *
         * @param userId the user ID
         * @return the builder instance
         */
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * Sets the module to filter by.
         *
         * @param module the module name, e.g. {@code "esm"}
         * @return the builder instance
         */
        public Builder module(String module) {
            this.module = module;
            return this;
        }

        /**
         * Sets the command to filter by.
         *
         * @param command the command, e.g. {@code "delete-bucket"}
         * @return the builder instance
         */
        public Builder command(String command) {
            this.command = command;
            return this;
        }

        /**
         * Sets the maximum number of entries per page.
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
         * Builds and returns a new instance of ListEventsRequest using the properties set on the Builder.
         *
         * @return a new ListEventsRequest instance.
         */
        public ListEventsRequest build() {
            return new ListEventsRequest(userId, module, command, pageSize, pageIndex);
        }
    }
}
