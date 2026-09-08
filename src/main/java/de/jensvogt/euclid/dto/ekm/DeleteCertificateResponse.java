package de.jensvogt.euclid.dto.ekm;

/**
 * Response returned after a certificate has been deleted.
 *
 * <p>Unlike a key, a certificate is removed outright with no grace period, because nothing becomes
 * unreadable: a listener already serving it keeps the copy it loaded until it restarts, which is
 * what makes this recoverable - import a replacement under the same name.
 *
 * @param ern  the ERN of the deleted certificate
 * @param name the name of the deleted certificate
 */
public record DeleteCertificateResponse(String ern, String name) {

    /**
     * Creates a new instance of the Builder for constructing a DeleteCertificateResponse object.
     *
     * @return a new Builder instance for constructing DeleteCertificateResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link DeleteCertificateResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN of the deleted certificate.
         */
        private String ern;

        /**
         * The name of the deleted certificate.
         */
        private String name;

        /**
         * Sets the ERN of the deleted certificate.
         *
         * @param ern the certificate ERN
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the name of the deleted certificate.
         *
         * @param name the certificate name
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Builds and returns a new instance of DeleteCertificateResponse using the properties set on the Builder.
         *
         * @return a new DeleteCertificateResponse instance.
         */
        public DeleteCertificateResponse build() {
            return new DeleteCertificateResponse(ern, name);
        }
    }
}
