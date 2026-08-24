package de.jensvogt.euclid.dto.eqs;

public record CreateQueueRequest(String name, long visibility, long maxRetries, long maxMessageLength,
                                  String dlqName, long delay) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private long visibility = 30;
        private long maxRetries = 3;
        private long maxMessageLength = 1024 * 1024;
        private String dlqName = "";
        private long delay = 0;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder visibility(long visibility) {
            this.visibility = visibility;
            return this;
        }

        public Builder maxRetries(long maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder maxMessageLength(long maxMessageLength) {
            this.maxMessageLength = maxMessageLength;
            return this;
        }

        public Builder dlqName(String dlqName) {
            this.dlqName = dlqName;
            return this;
        }

        public Builder delay(long delay) {
            this.delay = delay;
            return this;
        }

        public CreateQueueRequest build() {
            return new CreateQueueRequest(name, visibility, maxRetries, maxMessageLength, dlqName, delay);
        }
    }
}
