package de.jensvogt.euclid.dto.esm;

public record CreateUploadResponse(String uploadId, String bucketErn, String key) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        private String uploadId;
        private String bucketErn;
        private String key;

        public Builder uploadId(String uploadId) {
            this.uploadId = uploadId;
            return this;
        }

        public Builder bucketErn(String bucketErn) {
            this.bucketErn = bucketErn;
            return this;
        }

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public CreateUploadResponse build() {
            return new CreateUploadResponse(uploadId, bucketErn, key);
        }
    }
}
