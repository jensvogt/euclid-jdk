package de.jensvogt.euclid.dto.esm;

public record CompleteUploadRequest(String uploadId) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String uploadId;

        public Builder uploadId(String uploadId) {
            this.uploadId = uploadId;
            return this;
        }

        public CompleteUploadRequest build() {
            return new CompleteUploadRequest(uploadId);
        }
    }
}
