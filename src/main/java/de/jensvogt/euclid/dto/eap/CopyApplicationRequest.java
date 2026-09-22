package de.jensvogt.euclid.dto.eap;

/**
 * Request to define an application again in another namespace, leaving the original alone.
 * <p>
 * The sibling of moving one with {@link UpdateApplicationRequest}'s namespace: a move takes the
 * definition with it, so what ran in the old namespace stops running there. A copy is how the same
 * build is promoted - development to integration, integration to production - while the namespace
 * it came from goes on serving.
 * <p>
 * The copy runs the same artifact, down to the checksum, so it is the same bytes rather than a
 * rebuild that happens to share a version. It is given a runtime name and a technical principal of
 * its own, because both are installation-wide and cannot be shared, and it is created stopped
 * whatever the original is doing.
 * <p>
 * What the application may reach is re-resolved rather than copied: a bucket or queue ERN carries
 * the namespace it was resolved in, so copying the list would point the new application at the old
 * namespace's data. The same names are looked up in the target namespace, and a name with no
 * counterpart there fails the copy rather than leaving the application with less access than the
 * one it was copied from.
 *
 * @param applicationId       the application to copy, in the namespace the session works in
 * @param targetNamespace     the namespace to copy it into; it has to exist already
 * @param targetApplicationId the name the copy is defined under, or null for the original's name.
 *                            Naming it is how an application is copied beside itself within one
 *                            namespace
 */
public record CopyApplicationRequest(String applicationId, String targetNamespace, String targetApplicationId) {

    /**
     * Creates a new instance of the Builder for constructing a CopyApplicationRequest object.
     *
     * @return a new Builder instance for constructing CopyApplicationRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CopyApplicationRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The application to copy.
         */
        private String applicationId;

        /**
         * The namespace to copy it into.
         */
        private String targetNamespace;

        /**
         * The name the copy is defined under; null leaves it the original's.
         */
        private String targetApplicationId;

        /**
         * Sets the application to copy.
         *
         * @param applicationId the ID of the application to copy
         * @return the builder instance
         */
        public Builder applicationId(String applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        /**
         * Sets the namespace to copy the application into.
         *
         * @param targetNamespace the namespace, which has to exist already
         * @return the builder instance
         */
        public Builder targetNamespace(String targetNamespace) {
            this.targetNamespace = targetNamespace;
            return this;
        }

        /**
         * Sets the name the copy is defined under.
         *
         * @param targetApplicationId the copy's name, or {@code null} to keep the original's
         * @return the builder instance
         */
        public Builder targetApplicationId(String targetApplicationId) {
            this.targetApplicationId = targetApplicationId;
            return this;
        }

        /**
         * Builds and returns a new instance of CopyApplicationRequest using the properties set on the Builder.
         *
         * @return a new CopyApplicationRequest instance.
         */
        public CopyApplicationRequest build() {
            return new CopyApplicationRequest(applicationId, targetNamespace, targetApplicationId);
        }
    }
}
