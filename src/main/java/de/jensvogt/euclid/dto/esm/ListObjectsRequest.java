package de.jensvogt.euclid.dto.esm;

public record ListObjectsRequest(String bucketErn, String prefix, long pageSize, long pageIndex, String sortColumn) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String bucketErn = "";
        private String prefix = "";
        private long pageSize = 10;
        private long pageIndex = 0;
        private String sortColumn = "name";

        public Builder bucketErn(String bucketErn) {
            this.bucketErn = bucketErn;
            return this;
        }

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

        public ListObjectsRequest build() {
            return new ListObjectsRequest(bucketErn, prefix, pageSize, pageIndex, sortColumn);
        }
    }
}
