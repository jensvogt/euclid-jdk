package de.jensvogt.euclid.dto.eqs;

public record PurgeAllQueuesRequest(String region, String accountId) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

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
