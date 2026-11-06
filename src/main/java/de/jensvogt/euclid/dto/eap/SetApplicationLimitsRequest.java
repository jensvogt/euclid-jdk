package de.jensvogt.euclid.dto.eap;

/**
 * Request to set an application's per-instance resource limits, without restarting the instances
 * it has.
 * <p>
 * {@link UpdateApplicationRequest} writes the whole definition and stamps the modification date,
 * and the manager restarts a pool whose application changed since it started it. This writes the
 * limits alone, and they reach instances as they start - a new one, a replacement, a restart.
 * Running instances keep the limit they started under: a JVM sizes its heap once, from the limit
 * it starts with, and a lower ceiling than it planned for is an OOM kill waiting for the next busy
 * minute. Restart the application to hand running instances the new one.
 * <p>
 * A limit left null is left as it stands, so the CPU limit can be changed without touching memory.
 * At least one has to be given. Zero removes a limit rather than setting one of nothing.
 * <p>
 * Both are per instance. The application as a whole may use up to its maximum instance count
 * times the memory limit, and that - the floor's share of it - is what the server checks against
 * the account's nodes before accepting a higher one. Over its memory limit an instance is killed
 * by the kernel and shows as {@code OOM_KILLED}; over its CPU limit it is only throttled, never
 * stopped.
 *
 * @param applicationId the application whose limits to set; the only required field
 * @param memoryLimitMb memory each instance may use, in MiB - 0 for no limit, otherwise at least
 *                      64 - or null to leave it
 * @param cpuLimit      CPU each instance may use, in cores - 0 for no limit, otherwise at least
 *                      0.01 - or null to leave it
 */
public record SetApplicationLimitsRequest(String applicationId, Long memoryLimitMb, Double cpuLimit) {

    /**
     * Creates a new instance of the Builder for constructing a SetApplicationLimitsRequest object.
     *
     * @return a new Builder instance for constructing SetApplicationLimitsRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SetApplicationLimitsRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The application whose limits to set.
         */
        private String applicationId;

        /**
         * Memory each instance may use, in MiB; null leaves it alone.
         */
        private Long memoryLimitMb;

        /**
         * CPU each instance may use, in cores; null leaves it alone.
         */
        private Double cpuLimit;

        /**
         * Sets the application whose limits to set.
         *
         * @param applicationId the ID of the application
         * @return the builder instance
         */
        public Builder applicationId(String applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        /**
         * Sets the memory each instance may use.
         *
         * @param memoryLimitMb the limit in MiB, 0 to remove it, or {@code null} to leave the stored one
         * @return the builder instance
         */
        public Builder memoryLimitMb(Long memoryLimitMb) {
            this.memoryLimitMb = memoryLimitMb;
            return this;
        }

        /**
         * Sets the CPU each instance may use.
         *
         * @param cpuLimit the limit in cores, e.g. {@code 0.5}; 0 to remove it, or {@code null} to
         *                 leave the stored one
         * @return the builder instance
         */
        public Builder cpuLimit(Double cpuLimit) {
            this.cpuLimit = cpuLimit;
            return this;
        }

        /**
         * Builds and returns a new instance of SetApplicationLimitsRequest using the properties set on the Builder.
         *
         * @return a new SetApplicationLimitsRequest instance.
         */
        public SetApplicationLimitsRequest build() {
            return new SetApplicationLimitsRequest(applicationId, memoryLimitMb, cpuLimit);
        }
    }
}
