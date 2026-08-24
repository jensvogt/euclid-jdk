package de.jensvogt.euclid.dto.esm;

public record CreateUploadRequest(String bucketErn, String key) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String bucketErn;
        private String key;

        public Builder bucketErn(String bucketErn) {
            this.bucketErn = bucketErn;
            return this;
        }

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public CreateUploadRequest build() {
            return new CreateUploadRequest(bucketErn, key);
        }
    }
}
