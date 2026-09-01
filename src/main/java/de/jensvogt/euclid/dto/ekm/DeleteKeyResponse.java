package de.jensvogt.euclid.dto.ekm;

/**
 * The key a delete-key action scheduled for removal, and when that removal is due.
 *
 * @param ern          the key's ERN
 * @param name         the key ID
 * @param deletionDate ISO8601 timestamp the key becomes unrecoverable
 * @param status       the key's lifecycle status, {@code "PENDING_DELETION"} after this call
 */
public record DeleteKeyResponse(String ern, String name, String deletionDate, String status) {

    /**
     * Creates a new instance of the Builder for constructing a DeleteKeyResponse object.
     *
     * @return a new Builder instance for constructing DeleteKeyResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link DeleteKeyResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The key's ERN.
         */
        private String ern;

        /**
         * The key ID.
         */
        private String name;

        /**
         * ISO8601 timestamp the key becomes unrecoverable.
         */
        private String deletionDate;

        /**
         * The key's lifecycle status, {@code "PENDING_DELETION"} after this call.
         */
        private String status;

        /**
         * Sets the key's ERN.
         *
         * @param ern the key's ERN
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the key ID.
         *
         * @param name the key ID
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets ISO8601 timestamp the key becomes unrecoverable.
         *
         * @param deletionDate ISO8601 timestamp the key becomes unrecoverable
         * @return the builder instance
         */
        public Builder deletionDate(String deletionDate) {
            this.deletionDate = deletionDate;
            return this;
        }

        /**
         * Sets the key's lifecycle status, {@code "PENDING_DELETION"} after this call.
         *
         * @param status the key's lifecycle status, {@code "PENDING_DELETION"} after this call
         * @return the builder instance
         */
        public Builder status(String status) {
            this.status = status;
            return this;
        }

        /**
         * Builds and returns a new instance of DeleteKeyResponse using the properties set on the Builder.
         *
         * @return a new DeleteKeyResponse instance.
         */
        public DeleteKeyResponse build() {
            return new DeleteKeyResponse(ern, name, deletionDate, status);
        }
    }
}
