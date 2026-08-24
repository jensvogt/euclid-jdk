package de.jensvogt.euclid.dto.esm;

public record GetBucketSizeResponse(String ern, long size) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        private String ern;
        private long size;

        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        public Builder size(long size) {
            this.size = size;
            return this;
        }

        public GetBucketSizeResponse build() {
            return new GetBucketSizeResponse(ern, size);
        }
    }
}
