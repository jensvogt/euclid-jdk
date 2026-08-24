package de.jensvogt.euclid.dto.eqs;

public record ListQueueRequest(String prefix, long pageSize, long pageIndex, String sortColumn) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        private String prefix = "";
        private long pageSize = 10;
        private long pageIndex = 0;
        private String sortColumn = "userId";

        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder pageSize(long pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder pageIndex(long pageIndex) {
            this.pageIndex = pageIndex;
            return this;
        }

        public Builder sortColumn(String sortColumn) {
            this.sortColumn = sortColumn;
            return this;
        }

        public ListQueueRequest build() {
            return new ListQueueRequest(prefix, pageSize, pageIndex, sortColumn);
        }
    }
}
