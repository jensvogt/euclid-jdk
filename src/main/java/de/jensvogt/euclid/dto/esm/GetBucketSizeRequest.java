package de.jensvogt.euclid.dto.esm;

public record GetBucketSizeRequest(String ern) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String ern;

        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        public GetBucketSizeRequest build() {
            return new GetBucketSizeRequest(ern);
        }
    }
}
