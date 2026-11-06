package de.jensvogt.euclid.dto.emm;

import java.util.List;

/**
 * What to import: an archive previously produced by
 * {@link de.jensvogt.euclid.module.emm.EuclidEmm#exportModules} (or written by
 * {@code euclid-cli emm export}, or by the nightly backup), optionally narrowed to some of the
 * modules it holds.
 *
 * @param document   the archive, as the JSON text the export produced
 * @param modules    the subset of the archive's modules to import, or {@code null} or empty for
 *                   everything it holds
 * @param passphrase the passphrase a sealed archive was exported with; must be {@code null} or
 *                   empty for a plain one
 */
public record ImportRequest(String document, List<String> modules, String passphrase) {

    /**
     * Creates a new instance of the Builder for constructing an ImportRequest object.
     *
     * @return a new Builder instance for constructing ImportRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ImportRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The archive to import.
         */
        private String document;

        /**
         * The subset of the archive's modules to import.
         */
        private List<String> modules = List.of();

        /**
         * The passphrase a sealed archive was exported with.
         */
        private String passphrase;

        /**
         * Sets the archive to import.
         *
         * @param document the archive's JSON text
         * @return the builder instance
         */
        public Builder document(String document) {
            this.document = document;
            return this;
        }

        /**
         * Sets the subset of the archive's modules to import.
         *
         * @param modules the module names
         * @return the builder instance
         */
        public Builder modules(List<String> modules) {
            this.modules = modules;
            return this;
        }

        /**
         * Sets the subset of the archive's modules to import.
         *
         * @param modules the module names
         * @return the builder instance
         */
        public Builder modules(String... modules) {
            this.modules = List.of(modules);
            return this;
        }

        /**
         * Sets the passphrase a sealed archive was exported with.
         *
         * @param passphrase the passphrase
         * @return the builder instance
         */
        public Builder passphrase(String passphrase) {
            this.passphrase = passphrase;
            return this;
        }

        /**
         * Builds and returns a new instance of ImportRequest using the properties set on the Builder.
         *
         * @return a new ImportRequest instance.
         */
        public ImportRequest build() {
            return new ImportRequest(document, modules, passphrase);
        }
    }
}
