package de.jensvogt.euclid.dto.ekm;

import java.util.List;

/**
 * Request to have EKM generate a self-signed certificate, for an installation that has to serve
 * HTTPS before anybody has bought it a real one.
 *
 * <p>Nobody has vouched for the result. The stored certificate says so through
 * {@link de.jensvogt.euclid.dto.ekm.model.Certificate#generated()}, and a client still has to be
 * told to trust it.
 *
 * @param name            the name to store the certificate under
 * @param description     what the certificate is for, or {@code null}/empty
 * @param commonName      the subject common name; empty uses {@code name}, since for a listener
 *                        certificate those are usually the same word and a certificate with an
 *                        empty subject is refused by everything that reads it
 * @param subjectAltNames additional names the certificate is to be valid for
 * @param validDays       how many days the certificate is valid for
 * @param keyBits         the RSA key size in bits
 */
public record CreateCertificateRequest(String name, String description, String commonName,
                                       List<String> subjectAltNames, long validDays, long keyBits) {

    /**
     * Creates a new instance of the Builder for constructing a CreateCertificateRequest object.
     *
     * @return a new Builder instance for constructing CreateCertificateRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CreateCertificateRequest} instances.
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
         * The subject common name, empty to use the certificate's name.
         */
        private String commonName = "";

        /**
         * Additional names the certificate is to be valid for.
         */
        private List<String> subjectAltNames = List.of();

        /**
         * How many days the certificate is valid for. Defaults to the server's own 825, the longest
         * lifetime browsers accept for a newly issued certificate.
         */
        private long validDays = 825;

        /**
         * The RSA key size in bits, defaulting to the server's 2048.
         */
        private long keyBits = 2048;

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
         * Sets the subject common name.
         *
         * @param commonName the common name, or empty to use the certificate's name
         * @return the builder instance
         */
        public Builder commonName(String commonName) {
            this.commonName = commonName;
            return this;
        }

        /**
         * Sets the additional names the certificate is to be valid for.
         *
         * @param subjectAltNames the subject alternative names
         * @return the builder instance
         */
        public Builder subjectAltNames(List<String> subjectAltNames) {
            this.subjectAltNames = subjectAltNames;
            return this;
        }

        /**
         * Sets how many days the certificate is valid for.
         *
         * @param validDays the validity in days
         * @return the builder instance
         */
        public Builder validDays(long validDays) {
            this.validDays = validDays;
            return this;
        }

        /**
         * Sets the RSA key size in bits.
         *
         * @param keyBits the key size in bits
         * @return the builder instance
         */
        public Builder keyBits(long keyBits) {
            this.keyBits = keyBits;
            return this;
        }

        /**
         * Builds and returns a new instance of CreateCertificateRequest using the properties set on the Builder.
         *
         * @return a new CreateCertificateRequest instance.
         */
        public CreateCertificateRequest build() {
            return new CreateCertificateRequest(name, description, commonName, subjectAltNames, validDays, keyBits);
        }
    }
}
