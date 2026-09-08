package de.jensvogt.euclid.dto.ekm;

/**
 * Request naming a single certificate, shared by get-certificate and delete-certificate - both take
 * nothing but the name.
 *
 * @param name the name of the certificate the action applies to
 */
public record CertificateNameRequest(String name) {

    /**
     * Creates a new instance of the Builder for constructing a CertificateNameRequest object.
     *
     * @return a new Builder instance for constructing CertificateNameRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CertificateNameRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The name of the certificate the action applies to.
         */
        private String name;

        /**
         * Sets the name of the certificate the action applies to.
         *
         * @param name the name of the certificate
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Builds and returns a new instance of CertificateNameRequest using the properties set on the Builder.
         *
         * @return a new CertificateNameRequest instance populated with the name.
         */
        public CertificateNameRequest build() {
            return new CertificateNameRequest(name);
        }
    }
}
