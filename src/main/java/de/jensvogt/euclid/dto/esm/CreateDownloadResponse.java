package de.jensvogt.euclid.dto.esm;

/**
 * The download session a create-download action started, along with the metadata of the object
 * being downloaded - {@code size} is what tells the caller how many parts to ask for.
 *
 * @param downloadId  the download ID the part requests carry
 * @param bucketErn   the ERN of the bucket holding the object
 * @param key         the key of the object being downloaded
 * @param ern         the ERN of the object being downloaded
 * @param size        the object's total size in bytes
 * @param contentType the object's MIME content type
 */
public record CreateDownloadResponse(String downloadId, String bucketErn, String key, String ern, long size,
                                     String contentType) {

    /**
     * Creates a new instance of the Builder for constructing a CreateDownloadResponse object.
     *
     * @return a new Builder instance for constructing CreateDownloadResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CreateDownloadResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The download ID the part requests carry.
         */
        private String downloadId;

        /**
         * The ERN of the bucket holding the object.
         */
        private String bucketErn;

        /**
         * The key of the object being downloaded.
         */
        private String key;

        /**
         * The ERN of the object being downloaded.
         */
        private String ern;

        /**
         * The object's total size in bytes.
         */
        private long size = 0;

        /**
         * The object's MIME content type.
         */
        private String contentType;

        /**
         * Sets the download ID the part requests carry.
         *
         * @param downloadId the download ID the part requests carry
         * @return the builder instance
         */
        public Builder downloadId(String downloadId) {
            this.downloadId = downloadId;
            return this;
        }

        /**
         * Sets the ERN of the bucket holding the object.
         *
         * @param bucketErn the ERN of the bucket holding the object
         * @return the builder instance
         */
        public Builder bucketErn(String bucketErn) {
            this.bucketErn = bucketErn;
            return this;
        }

        /**
         * Sets the key of the object being downloaded.
         *
         * @param key the key of the object being downloaded
         * @return the builder instance
         */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /**
         * Sets the ERN of the object being downloaded.
         *
         * @param ern the ERN of the object being downloaded
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the object's total size in bytes.
         *
         * @param size the object's total size in bytes
         * @return the builder instance
         */
        public Builder size(long size) {
            this.size = size;
            return this;
        }

        /**
         * Sets the object's MIME content type.
         *
         * @param contentType the object's MIME content type
         * @return the builder instance
         */
        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        /**
         * Builds and returns a new instance of CreateDownloadResponse using the properties set on the Builder.
         *
         * @return a new CreateDownloadResponse instance.
         */
        public CreateDownloadResponse build() {
            return new CreateDownloadResponse(downloadId, bucketErn, key, ern, size, contentType);
        }
    }
}
