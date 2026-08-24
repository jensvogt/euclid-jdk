package de.jensvogt.euclid.dto.esm;

public record CompleteUploadResponse(String ern, String bucketErn, String key, long size, String status,
                                      String contentType, String md5Sum) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String ern;
        private String bucketErn;
        private String key;
        private long size;
        private String status;
        private String contentType;
        private String md5Sum;

        public Builder ern(String ern) {
            this.ern = ern;
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

        public Builder size(long size) {
            this.size = size;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder md5Sum(String md5Sum) {
            this.md5Sum = md5Sum;
            return this;
        }

        public CompleteUploadResponse build() {
            return new CompleteUploadResponse(ern, bucketErn, key, size, status, contentType, md5Sum);
        }
    }
}
