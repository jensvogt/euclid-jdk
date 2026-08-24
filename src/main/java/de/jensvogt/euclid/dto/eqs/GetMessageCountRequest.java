package de.jensvogt.euclid.dto.eqs;

public record GetMessageCountRequest(String ern) {

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

        public GetMessageCountRequest build() {
            return new GetMessageCountRequest(ern);
        }
    }
}
