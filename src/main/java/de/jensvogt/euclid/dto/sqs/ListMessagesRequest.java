package de.jensvogt.euclid.dto.sqs;

public record ListMessagesRequest(String queueErn, long pageSize, long pageIndex, String sortColumn) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String queueErn;
        private long pageSize = 10;
        private long pageIndex = 0;
        private String sortColumn = "created";

        public Builder queueErn(String queueErn) {
            this.queueErn = queueErn;
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

        public ListMessagesRequest build() {
            return new ListMessagesRequest(queueErn, pageSize, pageIndex, sortColumn);
        }
    }
}
