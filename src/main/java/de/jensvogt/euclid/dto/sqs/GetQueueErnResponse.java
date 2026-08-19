package de.jensvogt.euclid.dto.sqs;

public record GetQueueErnResponse(String ern) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String ern;

        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        public GetQueueErnResponse build() {
            return new GetQueueErnResponse(ern);
        }
    }
}
