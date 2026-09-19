package de.jensvogt.euclid.dto.eap;

/**
 * Response returned after an application's instances have been asked to start again.
 *
 * @param applicationId the ID of the application
 * @param restarting    whether the restart was recorded; the instances themselves are stopped and
 *                      started by euclid-mgr on its next reconcile, so this says the request was
 *                      taken rather than that anything has happened yet
 * @param instances     how many instances the manager is about to cycle - what was running when
 *                      the request was answered, not what came back from it
 */
public record RestartApplicationResponse(String applicationId, boolean restarting, long instances) {

    /**
     * Creates a new instance of the Builder for constructing a RestartApplicationResponse object.
     *
     * @return a new Builder instance for constructing RestartApplicationResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link RestartApplicationResponse} instances.
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
         * Whether the restart was recorded.
         */
        private boolean restarting;

        /**
         * How many instances the manager is about to cycle.
         */
        private long instances;

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
         * Sets whether the restart was recorded.
         *
         * @param restarting true when the request was taken
         * @return the builder instance
         */
        public Builder restarting(boolean restarting) {
            this.restarting = restarting;
            return this;
        }

        /**
         * Sets how many instances the manager is about to cycle.
         *
         * @param instances the instance count at the time of the request
         * @return the builder instance
         */
        public Builder instances(long instances) {
            this.instances = instances;
            return this;
        }

        /**
         * Builds and returns a new instance of RestartApplicationResponse using the properties set on the Builder.
         *
         * @return a new RestartApplicationResponse instance.
         */
        public RestartApplicationResponse build() {
            return new RestartApplicationResponse(applicationId, restarting, instances);
        }
    }
}
