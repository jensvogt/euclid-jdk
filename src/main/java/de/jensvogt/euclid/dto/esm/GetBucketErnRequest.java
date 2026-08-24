package de.jensvogt.euclid.dto.esm;

public record GetBucketErnRequest(String name) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public GetBucketErnRequest build() {
            return new GetBucketErnRequest(name);
        }
    }
}
