package de.jensvogt.euclid.dto.emm;

import java.util.List;

/**
 * What to export: which modules' MongoDB collections, how much of them, and whether to seal the
 * result with a passphrase.
 *
 * <p>Exactly one of {@link #modules()} and {@link #all()} is required. By default only the
 * top-level resource collections are included - queues, topics, buckets, users and so on;
 * {@link #full()} adds the bulk child data each resource owns (EQS/ENS messages, ESM objects).
 *
 * <p>An export holds everything those collections hold - access-key secrets among it, and with
 * {@code ekm} the key material itself - so a passphrase seals it: the contents are encrypted
 * (AES-256-GCM under a key derived from the passphrase with PBKDF2) and only which modules it holds
 * and when it was taken stay readable. Exporting {@code ekm}, alone or through {@code all}, requires
 * one. There is no recovery: an archive whose passphrase is lost is lost with it.
 *
 * @param modules    the modules to export, e.g. {@code ["esm", "eqs"]}; empty or {@code null} when
 *                   {@code all} is set
 * @param all        export every module instead of naming them
 * @param full       also export the bulk child data, not just the top-level resources
 * @param passphrase seal the archive with this passphrase, or {@code null} or empty for a plain one
 */
public record ExportRequest(List<String> modules, boolean all, boolean full, String passphrase) {

    /**
     * Creates a new instance of the Builder for constructing an ExportRequest object.
     *
     * @return a new Builder instance for constructing ExportRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ExportRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The modules to export.
         */
        private List<String> modules = List.of();

        /**
         * Whether to export every module.
         */
        private boolean all;

        /**
         * Whether to include the bulk child data.
         */
        private boolean full;

        /**
         * The passphrase to seal the archive with.
         */
        private String passphrase;

        /**
         * Sets the modules to export.
         *
         * @param modules the module names
         * @return the builder instance
         */
        public Builder modules(List<String> modules) {
            this.modules = modules;
            return this;
        }

        /**
         * Sets the modules to export.
         *
         * @param modules the module names
         * @return the builder instance
         */
        public Builder modules(String... modules) {
            this.modules = List.of(modules);
            return this;
        }

        /**
         * Sets whether to export every module instead of naming them.
         *
         * @param all {@code true} to export every module
         * @return the builder instance
         */
        public Builder all(boolean all) {
            this.all = all;
            return this;
        }

        /**
         * Sets whether to include the bulk child data.
         *
         * @param full {@code true} to include messages and object records
         * @return the builder instance
         */
        public Builder full(boolean full) {
            this.full = full;
            return this;
        }

        /**
         * Sets the passphrase to seal the archive with.
         *
         * @param passphrase the passphrase, or {@code null} for a plain archive
         * @return the builder instance
         */
        public Builder passphrase(String passphrase) {
            this.passphrase = passphrase;
            return this;
        }

        /**
         * Builds and returns a new instance of ExportRequest using the properties set on the Builder.
         *
         * @return a new ExportRequest instance.
         */
        public ExportRequest build() {
            return new ExportRequest(modules, all, full, passphrase);
        }
    }
}
