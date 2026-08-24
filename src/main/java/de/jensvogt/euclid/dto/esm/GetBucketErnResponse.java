package de.jensvogt.euclid.dto.esm;

public record GetBucketErnResponse(String ern) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String ern;

        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        public GetBucketErnResponse build() {
            return new GetBucketErnResponse(ern);
        }
    }
}
