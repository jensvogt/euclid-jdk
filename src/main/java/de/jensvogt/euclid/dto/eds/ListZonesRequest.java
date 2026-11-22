package de.jensvogt.euclid.dto.eds;

/**
 * Request for the zones of the account and namespace the session works in.
 *
 * <p>The prefix is sent only when set; EDS reads an absent prefix as "every zone".
 *
 * @param prefix    only zones whose name starts with this, or {@code null} for all
 * @param pageSize  the most zones to return; 0 means no limit
 * @param pageIndex the zero-based page, applied when {@code pageSize} is set
 */
public record ListZonesRequest(String prefix, long pageSize, long pageIndex) {

    /**
     * Creates a new Builder for constructing a ListZonesRequest.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListZonesRequest} instances.
     */
    public static final class Builder {

        /**
         * Only zones whose name starts with this.
         */
        private String prefix;

        /**
         * The most zones to return; 0 means no limit.
         */
        private long pageSize;

        /**
         * The zero-based page.
         */
        private long pageIndex;

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Lists only zones whose name starts with this.
         *
         * @param prefix the name prefix
         * @return this builder
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Sets the most zones to return.
         *
         * @param pageSize the page size; 0 means no limit
         * @return this builder
         */
        public Builder pageSize(long pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /**
         * Sets the zero-based page to return.
         *
         * @param pageIndex the page index
         * @return this builder
         */
        public Builder pageIndex(long pageIndex) {
            this.pageIndex = pageIndex;
            return this;
        }

        /**
         * Builds the request.
         *
         * @return a new ListZonesRequest
         */
        public ListZonesRequest build() {
            return new ListZonesRequest(prefix, pageSize, pageIndex);
        }
    }
}
