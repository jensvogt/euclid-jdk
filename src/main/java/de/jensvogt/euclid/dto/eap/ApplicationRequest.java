package de.jensvogt.euclid.dto.eap;

/**
 * Request naming a single application, shared by get-application, delete-application,
 * start-application and stop-application - all four take nothing but the ID.
 *
 * @param applicationId the ID of the application the action applies to
 */
public record ApplicationRequest(String applicationId) {

    /**
     * Creates a new instance of the Builder for constructing an ApplicationRequest object.
     *
     * @return a new Builder instance for constructing ApplicationRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ApplicationRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ID of the application the action applies to.
         */
        private String applicationId;

        /**
         * Sets the ID of the application the action applies to.
         *
         * @param applicationId the ID of the application the action applies to
         * @return the builder instance
         */
        public Builder applicationId(String applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        /**
         * Builds and returns a new instance of ApplicationRequest using the properties set on the Builder.
         *
         * @return a new ApplicationRequest instance.
         */
        public ApplicationRequest build() {
            return new ApplicationRequest(applicationId);
        }
    }
}
