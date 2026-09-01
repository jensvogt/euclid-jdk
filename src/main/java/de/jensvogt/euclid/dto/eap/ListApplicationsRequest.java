package de.jensvogt.euclid.dto.eap;

/**
 * Request to list applications, optionally filtered by application ID prefix.
 *
 * @param prefix only applications whose ID starts with this prefix are returned
 */
public record ListApplicationsRequest(String prefix) {

    /**
     * Creates a new instance of the Builder for constructing a ListApplicationsRequest object.
     *
     * @return a new Builder instance for constructing ListApplicationsRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListApplicationsRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Only applications whose ID starts with this prefix are returned.
         */
        private String prefix = "";

        /**
         * Sets only applications whose ID starts with this prefix are returned.
         *
         * @param prefix only applications whose ID starts with this prefix are returned
         * @return the builder instance
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Builds and returns a new instance of ListApplicationsRequest using the properties set on the Builder.
         *
         * @return a new ListApplicationsRequest instance.
         */
        public ListApplicationsRequest build() {
            return new ListApplicationsRequest(prefix);
        }
    }
}
