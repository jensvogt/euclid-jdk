package de.jensvogt.euclid.dto.esm;

public record PurgeBucketResponse(String ern, long count) {

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
        private long count;

        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        public Builder count(long count) {
            this.count = count;
            return this;
        }

        public PurgeBucketResponse build() {
            return new PurgeBucketResponse(ern, count);
        }
    }
}
