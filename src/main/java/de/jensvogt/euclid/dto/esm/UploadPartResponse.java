package de.jensvogt.euclid.dto.esm;

public record UploadPartResponse(String uploadId, long partNumber, long size) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String uploadId;
        private long partNumber;
        private long size;

        public Builder uploadId(String uploadId) {
            this.uploadId = uploadId;
            return this;
        }

        public Builder partNumber(long partNumber) {
            this.partNumber = partNumber;
            return this;
        }

        public Builder size(long size) {
            this.size = size;
            return this;
        }

        public UploadPartResponse build() {
            return new UploadPartResponse(uploadId, partNumber, size);
        }
    }
}
