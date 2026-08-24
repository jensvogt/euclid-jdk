package de.jensvogt.euclid.dto.esm;

public record DeleteObjectRequest(String ern) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String ern;

        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        public DeleteObjectRequest build() {
            return new DeleteObjectRequest(ern);
        }
    }
}
