package de.jensvogt.euclid.dto.access;

public record ListUserRequest(String prefix, long pageSize, long pageIndex, String sortColumn) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
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

        public ListUserRequest build() {
            return new ListUserRequest(prefix, pageSize, pageIndex, sortColumn);
        }
    }
}
