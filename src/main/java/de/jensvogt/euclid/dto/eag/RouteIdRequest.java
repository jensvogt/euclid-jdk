package de.jensvogt.euclid.dto.eag;

/**
 * Request body for the two actions that name nothing but a route: get-route and delete-route.
 *
 * @param routeId the route the action applies to
 */
public record RouteIdRequest(String routeId) {

    /**
     * Creates a new instance of the Builder for constructing a RouteIdRequest object.
     *
     * @return a new Builder instance for constructing RouteIdRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link RouteIdRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The route the action applies to.
         */
        private String routeId = "";

        /**
         * Sets the route the action applies to.
         *
         * @param routeId the route ID
         * @return the builder instance
         */
        public Builder routeId(String routeId) {
            this.routeId = routeId;
            return this;
        }

        /**
         * Builds and returns a new instance of RouteIdRequest using the properties set on the Builder.
         *
         * @return a new RouteIdRequest instance.
         */
        public RouteIdRequest build() {
            return new RouteIdRequest(routeId);
        }
    }
}
