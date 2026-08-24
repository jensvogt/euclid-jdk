package de.jensvogt.euclid.dto.eqs;

public record GetQueueMetadataRequest(String ern) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String ern;

        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        public GetQueueMetadataRequest build() {
            return new GetQueueMetadataRequest(ern);
        }
    }
}
