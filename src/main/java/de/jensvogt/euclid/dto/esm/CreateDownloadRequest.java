package de.jensvogt.euclid.dto.esm;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Request to start a multipart download, the mirror image of a create-upload.
 *
 * @param bucketErn the ERN of the bucket holding the object
 * @param key       the key of the object to download
 * @param versionId the version ID of the object
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateDownloadRequest(String bucketErn, String key, String versionId) {

    /**
     * Creates a new instance of the Builder for constructing a CreateDownloadRequest object.
     *
     * @return a new Builder instance for constructing CreateDownloadRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CreateDownloadRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN of the bucket holding the object.
         */
        private String bucketErn;

        /**
         * The key of the object to download.
         */
        private String key;

        private String versionId;

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
         * Sets the key of the object to download.
         *
         * @param key the key of the object to download
         * @return the builder instance
         */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /**
         * Sets a non-current version of the object to download instead of the current one.
         *
         * @param versionId the version, or {@code null} for the current object
         * @return the builder instance
         */
        public Builder versionId(String versionId) {
            this.versionId = versionId;
            return this;
        }

        /**
         * Builds and returns a new instance of CreateDownloadRequest using the properties set on the Builder.
         *
         * @return a new CreateDownloadRequest instance.
         */
        public CreateDownloadRequest build() {
            return new CreateDownloadRequest(bucketErn, key, versionId);
        }
    }
}
