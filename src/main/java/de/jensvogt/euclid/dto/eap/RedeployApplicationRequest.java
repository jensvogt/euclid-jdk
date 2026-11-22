package de.jensvogt.euclid.dto.eap;

/**
 * Request to deploy a new build of an application: an artifact already uploaded into the
 * application's own bucket, which the running instances are then restarted onto.
 * <p>
 * Unlike {@link UpdateApplicationRequest}, this one has a rule attached: EAP refuses it with HTTP
 * 409 when the artifact's bytes are the build already deployed, since a restart onto identical
 * bytes buys nothing. It is the artifact and not its name that decides - the version is
 * deliberately not part of the rule, so redeploying the same version with different bytes (a
 * rebuilt snapshot, a fix that keeps the number) goes through. {@code force} overrides the
 * refusal, and so does an artifact whose bytes are missing from storage, because that redeploy is
 * the one putting them back.
 * <p>
 * The upload comes first: the artifact has to be in the bucket, and settled, before this is sent,
 * since the checksum EAP compares is the stored object's. One still being uploaded is HTTP 409.
 *
 * @param applicationId the application to deploy to; the only required field
 * @param artifact      key of the artifact object; left unset, the key the application already
 *                      uses, which is the ordinary case - the build changes, its name does not
 * @param version       version this build is; left unset, EAP reads it out of the artifact's own
 *                      name, and refuses the redeploy if the name carries none
 * @param force         {@code true} to deploy even when the artifact is byte for byte the build
 *                      already deployed; left unset, such a redeploy is refused
 */
public record RedeployApplicationRequest(String applicationId, String artifact, String version, Boolean force) {

    /**
     * Creates a new instance of the Builder for constructing a RedeployApplicationRequest object.
     *
     * @return a new Builder instance for constructing RedeployApplicationRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link RedeployApplicationRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The application to deploy to.
         */
        private String applicationId;

        /**
         * Key of the artifact object; left unset, the key the application already uses.
         */
        private String artifact;

        /**
         * Version this build is; left unset, EAP reads it out of the artifact's own name.
         */
        private String version;

        /**
         * Whether to deploy even an identical build; null leaves the refusal in place.
         */
        private Boolean force;

        /**
         * Sets the application to deploy to.
         *
         * @param applicationId the application to deploy to
         * @return the builder instance
         */
        public Builder applicationId(String applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        /**
         * Sets the key of the artifact object.
         * <p>
         * Left unset, the key the application already uses. Set it to deploy under a different
         * name - a versioned one like {@code orders-1.4.0.jar} - and the application is repointed
         * at it.
         *
         * @param artifact key of the artifact object
         * @return the builder instance
         */
        public Builder artifact(String artifact) {
            this.artifact = artifact;
            return this;
        }

        /**
         * Sets the version this build is, e.g. {@code "1.4.0"}.
         * <p>
         * Left unset, EAP reads it out of the artifact's own name - {@code orders-1.4.0.jar} is
         * {@code 1.4.0} - and refuses the redeploy if the name carries no such version. It is
         * recorded, not checked: the same version as the one deployed is fine as long as the bytes
         * differ.
         *
         * @param version version this build is
         * @return the builder instance
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * Asks for the deployment to go ahead even if the artifact is the build already deployed.
         * <p>
         * For somebody who has been told what the refusal would be and means it anyway: restarting
         * a pool onto the same bytes after a host lost its copy, say. The server logs the reason it
         * overrode, so "why did this pool restart onto the same build" has an answer afterwards.
         * {@link de.jensvogt.euclid.module.eap.EuclidEap#restartApplication(String)} is the better
         * tool when nothing about the artifact needs fixing.
         *
         * @param force {@code true} to deploy an identical build
         * @return the builder instance
         */
        public Builder force(Boolean force) {
            this.force = force;
            return this;
        }

        /**
         * Builds and returns a new instance of RedeployApplicationRequest using the properties set on the Builder.
         *
         * @return a new RedeployApplicationRequest instance.
         */
        public RedeployApplicationRequest build() {
            return new RedeployApplicationRequest(applicationId, artifact, version, force);
        }
    }
}
