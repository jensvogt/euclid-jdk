package de.jensvogt.euclid.dto.sqs;

public record CreateQueueResponse(String name, String ern) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private String ern;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        public CreateQueueResponse build() {
            return new CreateQueueResponse(name, ern);
        }
    }
}
