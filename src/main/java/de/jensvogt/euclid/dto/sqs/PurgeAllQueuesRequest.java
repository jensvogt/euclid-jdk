package de.jensvogt.euclid.dto.sqs;

public record PurgeAllQueuesRequest(String region, String accountId) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String region;
        private String accountId;

        public Builder region(String region) {
            this.region = region;
            return this;
        }

        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public PurgeAllQueuesRequest build() {
            return new PurgeAllQueuesRequest(region, accountId);
        }
    }
}
