package de.jensvogt.euclid.dto.ekm;

import de.jensvogt.euclid.dto.ekm.model.Certificate;

import java.util.List;

/**
 * Response carrying a page of certificates and how many exist in total.
 *
 * @param certificates the certificates on the requested page
 * @param total        how many certificates match, across every page
 */
public record ListCertificatesResponse(List<Certificate> certificates, long total) {

    /**
     * Creates a new instance of the Builder for constructing a ListCertificatesResponse object.
     *
     * @return a new Builder instance for constructing ListCertificatesResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListCertificatesResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The certificates on the requested page.
         */
        private List<Certificate> certificates = List.of();

        /**
         * How many certificates match, across every page.
         */
        private long total;

        /**
         * Sets the certificates on the requested page.
         *
         * @param certificates the certificates
         * @return the builder instance
         */
        public Builder certificates(List<Certificate> certificates) {
            this.certificates = certificates;
            return this;
        }

        /**
         * Sets how many certificates match in total.
         *
         * @param total the total count
         * @return the builder instance
         */
        public Builder total(long total) {
            this.total = total;
            return this;
        }

        /**
         * Builds and returns a new instance of ListCertificatesResponse using the properties set on the Builder.
         *
         * @return a new ListCertificatesResponse instance.
         */
        public ListCertificatesResponse build() {
            return new ListCertificatesResponse(certificates, total);
        }
    }
}
