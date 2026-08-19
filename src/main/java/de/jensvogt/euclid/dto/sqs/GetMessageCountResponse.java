package de.jensvogt.euclid.dto.sqs;

public record GetMessageCountResponse(String ern, long available, long delayed, long invisible) {

    public long total() {
        return available + delayed + invisible;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String ern;
        private long available;
        private long delayed;
        private long invisible;

        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        public Builder available(long available) {
            this.available = available;
            return this;
        }

        public Builder delayed(long delayed) {
            this.delayed = delayed;
            return this;
        }

        public Builder invisible(long invisible) {
            this.invisible = invisible;
            return this;
        }

        public GetMessageCountResponse build() {
            return new GetMessageCountResponse(ern, available, delayed, invisible);
        }
    }
}
