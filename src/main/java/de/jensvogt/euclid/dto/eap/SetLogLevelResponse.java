package de.jensvogt.euclid.dto.eap;

/**
 * Response returned after an application's log level has been set or reset.
 *
 * @param applicationId the ID of the application
 * @param logLevel      the level the application now logs at, canonicalised by the server; empty
 *                      when the override was taken back and the configured default applies again
 * @param channel       the logging channel the level was applied to
 */
public record SetLogLevelResponse(String applicationId, String logLevel, String channel) {

    /**
     * Creates a new instance of the Builder for constructing a SetLogLevelResponse object.
     *
     * @return a new Builder instance for constructing SetLogLevelResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SetLogLevelResponse} instances.
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
         * The level the application now logs at.
         */
        private String logLevel;

        /**
         * The logging channel the level was applied to.
         */
        private String channel;

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
         * Sets the level the application now logs at.
         *
         * @param logLevel the canonical level name, empty for the configured default
         * @return the builder instance
         */
        public Builder logLevel(String logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        /**
         * Sets the logging channel the level was applied to.
         *
         * @param channel the channel name
         * @return the builder instance
         */
        public Builder channel(String channel) {
            this.channel = channel;
            return this;
        }

        /**
         * Builds and returns a new instance of SetLogLevelResponse using the properties set on the Builder.
         *
         * @return a new SetLogLevelResponse instance.
         */
        public SetLogLevelResponse build() {
            return new SetLogLevelResponse(applicationId, logLevel, channel);
        }
    }
}
