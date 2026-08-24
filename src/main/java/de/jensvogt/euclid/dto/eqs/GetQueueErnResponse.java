package de.jensvogt.euclid.dto.eqs;

public record GetQueueErnResponse(String ern) {

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

        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        public GetQueueErnResponse build() {
            return new GetQueueErnResponse(ern);
        }
    }
}
