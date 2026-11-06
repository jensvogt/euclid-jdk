package de.jensvogt.euclid.dto.esm;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Request to delete an object.
 *
 * @param ern       the ERN (Entity Resource Name) of the object to delete
 * @param versionId the objects version ID
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeleteObjectRequest(String ern, String versionId) {

    /**
     * Creates a new instance of the Builder for constructing a DeleteObjectRequest object.
     *
     * @return a new Builder instance for constructing DeleteObjectRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link DeleteObjectRequest} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN (Entity Resource Name) of the object to delete.
         */
        private String ern;

        private String versionId;

        /**
         * Sets the ERN of the object to delete.
         *
         * @param ern the ERN of the object
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the one version of the object to delete.
         *
         * @param versionId the version, or {@code null} to delete the object and every version of it
         * @return the builder instance
         */
        public Builder versionId(String versionId) {
            this.versionId = versionId;
            return this;
        }

        /**
         * Builds and returns a new instance of DeleteObjectRequest using the properties set on the Builder.
         *
         * @return a new DeleteObjectRequest instance populated with the ERN value.
         */
        public DeleteObjectRequest build() {
            return new DeleteObjectRequest(ern, versionId);
        }
    }
}
