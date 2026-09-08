package de.jensvogt.euclid.dto.ekm;

/**
 * Request to store a certificate somebody else issued, together with its private key.
 *
 * <p>Both halves are required and are checked against each other server-side. A certificate stored
 * with a key that is not its own is accepted silently by every step after this one, and only shows
 * itself as a handshake that fails for every caller - so it is refused here instead.
 *
 * @param name        the name to store the certificate under
 * @param description what the certificate is for, or {@code null}/empty
 * @param certificate the PEM-encoded X.509 certificate
 * @param privateKey  the PEM-encoded private key belonging to that certificate. EKM keeps it: no
 *                    action hands it back afterwards
 */
public record ImportCertificateRequest(String name, String description, String certificate, String privateKey) {

    /**
     * Creates a new instance of the Builder for constructing an ImportCertificateRequest object.
     *
     * @return a new Builder instance for constructing ImportCertificateRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ImportCertificateRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The name to store the certificate under.
         */
        private String name;

        /**
         * What the certificate is for.
         */
        private String description = "";

        /**
         * The PEM-encoded X.509 certificate.
         */
        private String certificate;

        /**
         * The PEM-encoded private key belonging to that certificate.
         */
        private String privateKey;

        /**
         * Sets the name to store the certificate under.
         *
         * @param name the certificate name
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets what the certificate is for.
         *
         * @param description the description
         * @return the builder instance
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the PEM-encoded X.509 certificate.
         *
         * @param certificate the certificate PEM
         * @return the builder instance
         */
        public Builder certificate(String certificate) {
            this.certificate = certificate;
            return this;
        }

        /**
         * Sets the PEM-encoded private key.
         *
         * @param privateKey the private key PEM
         * @return the builder instance
         */
        public Builder privateKey(String privateKey) {
            this.privateKey = privateKey;
            return this;
        }

        /**
         * Builds and returns a new instance of ImportCertificateRequest using the properties set on the Builder.
         *
         * @return a new ImportCertificateRequest instance.
         */
        public ImportCertificateRequest build() {
            return new ImportCertificateRequest(name, description, certificate, privateKey);
        }
    }
}
