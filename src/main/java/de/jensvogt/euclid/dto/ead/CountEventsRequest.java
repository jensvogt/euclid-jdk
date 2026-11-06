package de.jensvogt.euclid.dto.ead;

/**
 * Request to count audit entries without listing them. Takes the same filters as
 * {@link ListEventsRequest}, each sent only when set, and is confined to the caller's own account
 * the same way.
 *
 * @param userId  only entries of this user, or {@code null} for every user
 * @param module  only entries addressed to this module, e.g. {@code "esm"}, or {@code null} for all
 * @param command only entries of this command, e.g. {@code "delete-bucket"}, or {@code null} for all
 */
public record CountEventsRequest(String userId, String module, String command) {

    /**
     * Creates a new instance of the Builder for constructing a CountEventsRequest object.
     *
     * @return a new Builder instance for constructing CountEventsRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CountEventsRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Only entries of this user.
         */
        private String userId;

        /**
         * Only entries addressed to this module.
         */
        private String module;

        /**
         * Only entries of this command.
         */
        private String command;

        /**
         * Sets the user to filter by.
         *
         * @param userId the user ID
         * @return the builder instance
         */
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * Sets the module to filter by.
         *
         * @param module the module name, e.g. {@code "esm"}
         * @return the builder instance
         */
        public Builder module(String module) {
            this.module = module;
            return this;
        }

        /**
         * Sets the command to filter by.
         *
         * @param command the command, e.g. {@code "delete-bucket"}
         * @return the builder instance
         */
        public Builder command(String command) {
            this.command = command;
            return this;
        }

        /**
         * Builds and returns a new instance of CountEventsRequest using the properties set on the Builder.
         *
         * @return a new CountEventsRequest instance.
         */
        public CountEventsRequest build() {
            return new CountEventsRequest(userId, module, command);
        }
    }
}
