package de.jensvogt.euclid.dto.esm;

public record CreateBucketRequest(String name) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public CreateBucketRequest build() {
            return new CreateBucketRequest(name);
        }
    }
}
