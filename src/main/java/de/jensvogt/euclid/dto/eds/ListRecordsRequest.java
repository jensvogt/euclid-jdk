package de.jensvogt.euclid.dto.eds;

/**
 * Request for a zone's record sets, optionally narrowed to one name and one type.
 *
 * <p>The name and type filters are sent only when set; EDS reads an absent filter as "no filter".
 *
 * @param zone      the zone's apex
 * @param name      only sets at this owner name, in any form {@link PutRecordRequest#name()} accepts, or {@code null} for all
 * @param type      only sets of this type, or {@code null} for all
 * @param pageSize  the most sets to return; 0 means no limit
 * @param pageIndex the zero-based page, applied when {@code pageSize} is set
 */
public record ListRecordsRequest(String zone, String name, String type, long pageSize, long pageIndex) {

    /**
     * Creates a new Builder for constructing a ListRecordsRequest.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListRecordsRequest} instances.
     */
    public static final class Builder {

        /**
         * The zone's apex.
         */
        private String zone;

        /**
         * Only sets at this owner name.
         */
        private String name;

        /**
         * Only sets of this type.
         */
        private String type;

        /**
         * The most sets to return; 0 means no limit.
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
         * Sets the zone to list.
         *
         * @param zone the zone's apex
         * @return this builder
         */
        public Builder zone(String zone) {
            this.zone = zone;
            return this;
        }

        /**
         * Lists only sets at this owner name.
         *
         * @param name the owner name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Lists only sets of this type.
         *
         * @param type the type mnemonic
         * @return this builder
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the most sets to return.
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
         * @return a new ListRecordsRequest
         */
        public ListRecordsRequest build() {
            return new ListRecordsRequest(zone, name, type, pageSize, pageIndex);
        }
    }
}
