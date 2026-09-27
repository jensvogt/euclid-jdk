package de.jensvogt.euclid.dto.eap;

import java.util.List;

/**
 * Response returned after an application's infrastructure declaration has been applied.
 * <p>
 * {@code declared} false means the application has no declaration stored - not that applying one
 * failed. Nothing was created, deleted or granted, and the four lists are empty; an application
 * that provisions its resources by hand reads this way every time and is not in error.
 * <p>
 * {@code deleted} is the half worth reading before trusting a declaration: a reconcile is full, so
 * a resource this application created and the declaration no longer names is removed, taking a
 * queue's messages or a bucket's objects with it. It is named here rather than counted so that a
 * removal nobody intended is visible in the answer.
 *
 * @param applicationId the ID of the application
 * @param declared      whether the application had a declaration to apply at all
 * @param created       ERNs of the resources that did not exist and were created; one that already
 *                      existed is not listed, so a re-apply that changes nothing reports none
 * @param deleted       ERNs of the resources this application created and the declaration no
 *                      longer names, which have been removed with everything they held
 * @param granted       the {@code access-} roles the declaration's {@code uses} entries asked for
 * @param revoked       the {@code access-} roles that were dropped first; they are replaced
 *                      wholesale rather than diffed, so a re-apply that changes nothing still
 *                      reports every role in both lists
 */
public record ApplyInfrastructureResponse(String applicationId, boolean declared, List<String> created,
                                          List<String> deleted, List<String> granted, List<String> revoked) {

    /**
     * Creates a new instance of the Builder for constructing an ApplyInfrastructureResponse object.
     *
     * @return a new Builder instance for constructing ApplyInfrastructureResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ApplyInfrastructureResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ID of the application.
         */
        private String applicationId;

        /**
         * Whether the application had a declaration to apply.
         */
        private boolean declared;

        /**
         * ERNs of the resources that were created.
         */
        private List<String> created = List.of();

        /**
         * ERNs of the resources that were removed.
         */
        private List<String> deleted = List.of();

        /**
         * The roles that were granted.
         */
        private List<String> granted = List.of();

        /**
         * The roles that were dropped first.
         */
        private List<String> revoked = List.of();

        /**
         * Sets the ID of the application.
         *
         * @param applicationId the ID of the application
         * @return the builder instance
         */
        public Builder applicationId(String applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        /**
         * Sets whether the application had a declaration to apply.
         *
         * @param declared true when a declaration was found and applied
         * @return the builder instance
         */
        public Builder declared(boolean declared) {
            this.declared = declared;
            return this;
        }

        /**
         * Sets the ERNs of the resources that were created.
         *
         * @param created the created resources
         * @return the builder instance
         */
        public Builder created(List<String> created) {
            this.created = created;
            return this;
        }

        /**
         * Sets the ERNs of the resources that were removed.
         *
         * @param deleted the removed resources
         * @return the builder instance
         */
        public Builder deleted(List<String> deleted) {
            this.deleted = deleted;
            return this;
        }

        /**
         * Sets the roles that were granted.
         *
         * @param granted the granted roles
         * @return the builder instance
         */
        public Builder granted(List<String> granted) {
            this.granted = granted;
            return this;
        }

        /**
         * Sets the roles that were dropped first.
         *
         * @param revoked the revoked roles
         * @return the builder instance
         */
        public Builder revoked(List<String> revoked) {
            this.revoked = revoked;
            return this;
        }

        /**
         * Builds and returns a new instance of ApplyInfrastructureResponse using the properties set on the Builder.
         *
         * @return a new ApplyInfrastructureResponse instance.
         */
        public ApplyInfrastructureResponse build() {
            return new ApplyInfrastructureResponse(applicationId, declared, created, deleted, granted, revoked);
        }
    }
}
