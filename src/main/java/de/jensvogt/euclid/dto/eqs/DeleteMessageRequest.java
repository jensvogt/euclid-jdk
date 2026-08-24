package de.jensvogt.euclid.dto.eqs;

public record DeleteMessageRequest(String receiptHandle) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String receiptHandle;

        public Builder receiptHandle(String receiptHandle) {
            this.receiptHandle = receiptHandle;
            return this;
        }

        public DeleteMessageRequest build() {
            return new DeleteMessageRequest(receiptHandle);
        }
    }
}
