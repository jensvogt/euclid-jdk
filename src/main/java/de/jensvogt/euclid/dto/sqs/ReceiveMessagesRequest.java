package de.jensvogt.euclid.dto.sqs;

public record ReceiveMessagesRequest(String ern, long maxCount, long waitTime) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String ern;
        private long maxCount;
        private long waitTime;

        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        public Builder maxCount(long maxCount) {
            this.maxCount = maxCount;
            return this;
        }

        public Builder waitTime(long waitTime) {
            this.waitTime = waitTime;
            return this;
        }

        public ReceiveMessagesRequest build() {
            return new ReceiveMessagesRequest(ern, maxCount, waitTime);
        }
    }
}
