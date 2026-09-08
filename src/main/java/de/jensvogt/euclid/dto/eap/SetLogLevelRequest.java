package de.jensvogt.euclid.dto.eap;

/**
 * Request to override the log level an application runs at, or to take that override back.
 *
 * @param applicationId the ID of the application whose log level is set
 * @param level         one of {@code "trace"}, {@code "debug"}, {@code "info"}, {@code "warning"},
 *                      {@code "error"}, {@code "fatal"} or {@code "off"}; empty puts the
 *                      application back under the level its channel is configured with. An
 *                      unrecognised level is refused rather than defaulted - a typo quietly meaning
 *                      "info" is an application logging more than somebody asked for, and quietly
 *                      meaning "off" is silence nobody asked for at all
 */
public record SetLogLevelRequest(String applicationId, String level) {

    /**
     * Creates a new instance of the Builder for constructing a SetLogLevelRequest object.
     *
     * @return a new Builder instance for constructing SetLogLevelRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SetLogLevelRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ID of the application whose log level is set.
         */
        private String applicationId;

        /**
         * The level to run at, empty to fall back to the configured default.
         */
        private String level = "";

        /**
         * Sets the ID of the application whose log level is set.
         *
         * @param applicationId the ID of the application
         * @return the builder instance
         */
        public Builder applicationId(String applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        /**
         * Sets the level the application is to log at.
         *
         * @param level the level name, or {@code null}/empty to fall back to the configured default
         * @return the builder instance
         */
        public Builder level(String level) {
            this.level = level;
            return this;
        }

        /**
         * Builds and returns a new instance of SetLogLevelRequest using the properties set on the Builder.
         *
         * @return a new SetLogLevelRequest instance populated with the application ID and level.
         */
        public SetLogLevelRequest build() {
            return new SetLogLevelRequest(applicationId, level);
        }
    }
}
