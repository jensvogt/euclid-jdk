package de.jensvogt.euclid.dto.eap;

/**
 * Request to deploy a new build of an application: an artifact already uploaded into the
 * application's own bucket, which the running instances are then restarted onto.
 * <p>
 * Unlike {@link UpdateApplicationRequest}, this one has a rule attached, and EAP refuses it unless
 * it is genuinely a new build - the version has to differ from the one deployed, and so do the
 * artifact's bytes. Two deployments under one version cannot be told apart afterwards, and a
 * version bump carrying the artifact already deployed ships nothing. Deploying either on purpose
 * is what {@code updateApplication} is for.
 * <p>
 * The upload comes first: the artifact has to be in the bucket before this is sent, since the
 * checksum EAP compares is the stored object's.
 *
 * @param applicationId the application to deploy to; the only required field
 * @param artifact      key of the artifact object; left unset, the key the application already
 *                      uses, which is the ordinary case - the build changes, its name does not
 * @param version       version this build is; left unset, EAP reads it out of the artifact's own
 *                      name, and refuses the redeploy if the name carries none
 */
public record RedeployApplicationRequest(String applicationId, String artifact, String version) {

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
         * {@code 1.4.0} - and refuses the redeploy if the name carries no such version.
         *
         * @param version version this build is
         * @return the builder instance
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * Builds and returns a new instance of RedeployApplicationRequest using the properties set on the Builder.
         *
         * @return a new RedeployApplicationRequest instance.
         */
        public RedeployApplicationRequest build() {
            return new RedeployApplicationRequest(applicationId, artifact, version);
        }
    }
}
