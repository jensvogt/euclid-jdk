package de.jensvogt.euclid.dto.emm;

/**
 * Request to set the instance limits the autoscaler works within for one module.
 *
 * <p>Either limit may be given on its own: a {@code null} is left out of the request, and EMM reads
 * an absent limit as "keep whatever is in force" - which is what lets a caller raise a ceiling
 * without restating the floor. At least one must be set.
 *
 * @param name         the module, e.g. {@code "esm"}
 * @param minInstances the floor kept running at all times (0 or more), or {@code null} to keep it
 * @param maxInstances the ceiling the autoscaler may scale up to (1 or more), or {@code null} to keep it
 */
public record SetInstancesRequest(String name, Integer minInstances, Integer maxInstances) {

    /**
     * Creates a new instance of the Builder for constructing a SetInstancesRequest object.
     *
     * @return a new Builder instance for constructing SetInstancesRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SetInstancesRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The module to set the limits for.
         */
        private String name;

        /**
         * The floor kept running, or {@code null} to keep the one in force.
         */
        private Integer minInstances;

        /**
         * The ceiling the autoscaler may scale up to, or {@code null} to keep the one in force.
         */
        private Integer maxInstances;

        /**
         * Sets the module to set the limits for.
         *
         * @param name the module name
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the floor kept running at all times.
         *
         * @param minInstances the minimum instance count, 0 or more
         * @return the builder instance
         */
        public Builder minInstances(Integer minInstances) {
            this.minInstances = minInstances;
            return this;
        }

        /**
         * Sets the ceiling the autoscaler may scale up to.
         *
         * @param maxInstances the maximum instance count, 1 or more
         * @return the builder instance
         */
        public Builder maxInstances(Integer maxInstances) {
            this.maxInstances = maxInstances;
            return this;
        }

        /**
         * Builds and returns a new instance of SetInstancesRequest using the properties set on the Builder.
         *
         * @return a new SetInstancesRequest instance.
         */
        public SetInstancesRequest build() {
            return new SetInstancesRequest(name, minInstances, maxInstances);
        }
    }
}
