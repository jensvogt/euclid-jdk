package de.jensvogt.euclid.dto.sqs;

public record GetQueueErnRequest(String name) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public GetQueueErnRequest build() {
            return new GetQueueErnRequest(name);
        }
    }
}
