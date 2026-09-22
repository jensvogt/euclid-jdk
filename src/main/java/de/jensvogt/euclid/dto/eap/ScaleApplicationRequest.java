package de.jensvogt.euclid.dto.eap;

/**
 * Request to change how many instances an application runs, without restarting the ones it has.
 * <p>
 * {@link UpdateApplicationRequest} can set the same two fields, but it writes the whole definition
 * and stamps the modification date - and the manager restarts a pool whose application has changed
 * since it started it. Scaling that way stops every running instance and starts it again, which is
 * the opposite of what asking for more capacity means.
 * <p>
 * What is set is the range the autoscaler works within, not a count: the manager scales toward it
 * on its next reconcile, adding instances one at a time and stopping idle ones as the load allows.
 * Nothing is started or stopped by the call itself. Setting both to the same number pins the pool
 * at that size and leaves the autoscaler nothing to decide.
 * <p>
 * A bound left null is left as it stands, so a ceiling can be raised without touching the floor.
 * The two are checked against each other as they <em>will</em> stand rather than as they are, so
 * raising only the floor is refused when it would pass the stored ceiling. A floor of zero is
 * refused outright: an application desired RUNNING with no instances reads everywhere as a pool
 * that failed to start, and stopping one is a different thing.
 *
 * @param applicationId the application to scale; the only required field
 * @param minInstances  smallest number of instances to keep running, or null to leave it
 * @param maxInstances  largest number the autoscaler may run, or null to leave it
 */
public record ScaleApplicationRequest(String applicationId, Long minInstances, Long maxInstances) {

    /**
     * Creates a new instance of the Builder for constructing a ScaleApplicationRequest object.
     *
     * @return a new Builder instance for constructing ScaleApplicationRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ScaleApplicationRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The application to scale.
         */
        private String applicationId;

        /**
         * Smallest number of instances to keep running; null leaves it alone.
         */
        private Long minInstances;

        /**
         * Largest number the autoscaler may run; null leaves it alone.
         */
        private Long maxInstances;

        /**
         * Sets the application to scale.
         *
         * @param applicationId the ID of the application
         * @return the builder instance
         */
        public Builder applicationId(String applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        /**
         * Sets the smallest number of instances to keep running.
         *
         * @param minInstances the floor, or {@code null} to leave the stored one
         * @return the builder instance
         */
        public Builder minInstances(Long minInstances) {
            this.minInstances = minInstances;
            return this;
        }

        /**
         * Sets the largest number of instances the autoscaler may run.
         *
         * @param maxInstances the ceiling, or {@code null} to leave the stored one
         * @return the builder instance
         */
        public Builder maxInstances(Long maxInstances) {
            this.maxInstances = maxInstances;
            return this;
        }

        /**
         * Sets both bounds to the same number, pinning the pool at that size.
         *
         * @param instances the size to hold the pool at
         * @return the builder instance
         */
        public Builder instances(long instances) {
            this.minInstances = instances;
            this.maxInstances = instances;
            return this;
        }

        /**
         * Builds and returns a new instance of ScaleApplicationRequest using the properties set on the Builder.
         *
         * @return a new ScaleApplicationRequest instance.
         */
        public ScaleApplicationRequest build() {
            return new ScaleApplicationRequest(applicationId, minInstances, maxInstances);
        }
    }
}
