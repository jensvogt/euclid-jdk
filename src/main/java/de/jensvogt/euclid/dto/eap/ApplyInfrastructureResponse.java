package de.jensvogt.euclid.dto.eap;

import de.jensvogt.euclid.dto.eap.model.KeptResource;
import de.jensvogt.euclid.dto.eap.model.UpdatedResource;

import java.util.List;

/**
 * Response returned after an infrastructure declaration has been applied, or planned.
 * <p>
 * The same shape answers all four actions: apply-infrastructure and plan-infrastructure for an
 * application, apply-platform and plan-platform for a platform. Exactly one of
 * {@code applicationId} and {@code platform} is set. With {@code dryRun} true nothing was changed,
 * and every list says what <em>would</em> have been done.
 * <p>
 * {@code declared} false means the application has no declaration stored - not that applying one
 * failed. Nothing was created, deleted or granted, and the lists are empty; an application that
 * provisions its resources by hand reads this way every time and is not in error.
 * <p>
 * {@code deleted} is the list worth reading before trusting a declaration: what this owner created
 * and the declaration no longer names is removed. Only when empty, though - a queue with messages,
 * a bucket with objects or a topic with subscriptions is left in place and listed in {@code kept}
 * instead, as is everything when pruning is turned off.
 *
 * @param applicationId the ID of the application, or {@code null} for a platform
 * @param platform      the platform's name, or {@code null} for an application
 * @param declared      whether there was a declaration to apply at all
 * @param dryRun        whether this was a plan, which changed nothing
 * @param created       ERNs of the resources that did not exist and were created; one that already
 *                      existed is not listed, so a re-apply that changes nothing reports none
 * @param adopted       ERNs of the resources that existed untagged and are now recorded as this
 *                      owner's
 * @param updated       the resources that existed and had a declared setting brought into line
 * @param deleted       ERNs of the resources this owner created and the declaration no longer
 *                      names, which have been removed
 * @param kept          the resources no longer declared that were left in place, and why
 * @param granted       the {@code access-} roles the declaration's {@code uses} entries asked for
 *                      and did not already have, each as {@code "<role> on <ern>"}
 * @param revoked       the {@code access-} roles that are no longer asked for and were dropped
 */
public record ApplyInfrastructureResponse(String applicationId, String platform, boolean declared, boolean dryRun,
                                          List<String> created, List<String> adopted, List<UpdatedResource> updated,
                                          List<String> deleted, List<KeptResource> kept, List<String> granted,
                                          List<String> revoked) {

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
         * The platform's name.
         */
        private String platform;

        /**
         * Whether there was a declaration to apply.
         */
        private boolean declared;

        /**
         * Whether this was a plan.
         */
        private boolean dryRun;

        /**
         * ERNs of the resources that were created.
         */
        private List<String> created = List.of();

        /**
         * ERNs of the resources that were adopted.
         */
        private List<String> adopted = List.of();

        /**
         * The resources whose settings were brought into line.
         */
        private List<UpdatedResource> updated = List.of();

        /**
         * ERNs of the resources that were removed.
         */
        private List<String> deleted = List.of();

        /**
         * The resources that were left in place.
         */
        private List<KeptResource> kept = List.of();

        /**
         * The roles that were granted.
         */
        private List<String> granted = List.of();

        /**
         * The roles that were dropped.
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
         * Sets the platform's name.
         *
         * @param platform the platform's name
         * @return the builder instance
         */
        public Builder platform(String platform) {
            this.platform = platform;
            return this;
        }

        /**
         * Sets whether there was a declaration to apply.
         *
         * @param declared true when a declaration was found and applied
         * @return the builder instance
         */
        public Builder declared(boolean declared) {
            this.declared = declared;
            return this;
        }

        /**
         * Sets whether this was a plan.
         *
         * @param dryRun true when nothing was changed
         * @return the builder instance
         */
        public Builder dryRun(boolean dryRun) {
            this.dryRun = dryRun;
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
         * Sets the ERNs of the resources that were adopted.
         *
         * @param adopted the adopted resources
         * @return the builder instance
         */
        public Builder adopted(List<String> adopted) {
            this.adopted = adopted;
            return this;
        }

        /**
         * Sets the resources whose settings were brought into line.
         *
         * @param updated the updated resources
         * @return the builder instance
         */
        public Builder updated(List<UpdatedResource> updated) {
            this.updated = updated;
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
         * Sets the resources that were left in place.
         *
         * @param kept the kept resources
         * @return the builder instance
         */
        public Builder kept(List<KeptResource> kept) {
            this.kept = kept;
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
         * Sets the roles that were dropped.
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
            return new ApplyInfrastructureResponse(applicationId, platform, declared, dryRun, created, adopted, updated,
                    deleted, kept, granted, revoked);
        }
    }
}
