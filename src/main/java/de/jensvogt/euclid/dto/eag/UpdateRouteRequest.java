package de.jensvogt.euclid.dto.eag;

import de.jensvogt.euclid.dto.eag.model.RouteAuthentication;

import java.util.List;

/**
 * Request to change a route that already exists. Only what the request names changes, so one
 * setting can be adjusted without restating the rest.
 *
 * <p>The server asks whether a field is present at all, which is why the unset fields are
 * {@code null} rather than {@code ""}: {@link de.jensvogt.euclid.module.eag.EuclidEag} serializes
 * with a mapper that drops null fields rather than writing them as {@code null}, so the distinction
 * survives serialization.
 *
 * <p>Naming an {@link #applicationId()} moves the route to an application and clears its module
 * target; naming a {@link #moduleTarget()} does the reverse. Leaving both set would make which one
 * wins depend on the proxy, so a request should name one or neither.
 *
 * @param routeId        the route to change. Required
 * @param path           the new path prefix, or {@code null} to leave it alone. It has to start
 *                       with {@code /}, and the resulting path/method pair has to be free - a
 *                       change that collides with another route is refused with 409, and changing
 *                       only the methods can collide just as easily as changing the path
 * @param applicationId  the application to move the route to, or {@code null} to leave it alone.
 *                       It has to exist. Setting it clears {@code moduleTarget} and
 *                       {@code moduleAction}
 * @param moduleTarget   the euclid module to move the route to, or {@code null} to leave it alone.
 *                       Setting it clears {@code applicationId}
 * @param moduleAction   the action a module route answers for, or {@code null} to leave it alone.
 *                       A route left naming a module with no action is refused with 400
 * @param methods        the new method list, or {@code null} to leave it alone; an empty list
 *                       answers for every method
 * @param region         the new region, or {@code null} to leave it alone
 * @param namespace      the new namespace, or {@code null} to leave it alone
 * @param authentication what a caller must present, as a {@link RouteAuthentication#wireValue()},
 *                       or {@code null} to leave it alone. An unrecognized value is refused rather
 *                       than defaulted: somebody asking for EUCLID and silently getting NONE would
 *                       be handed a public route they believe is protected
 * @param active         whether the gateway serves the route, or {@code null} to leave it alone.
 *                       This is how a route is taken out of service in a hurry and put back
 *                       afterwards knowing it returns exactly as it was
 */
public record UpdateRouteRequest(String routeId, String path, String applicationId, String moduleTarget,
                                 String moduleAction, List<String> methods, String region, String namespace,
                                 String authentication, Boolean active) {

    /**
     * Creates a new instance of the Builder for constructing an UpdateRouteRequest object.
     *
     * @return a new Builder instance for constructing UpdateRouteRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link UpdateRouteRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The route to change.
         */
        private String routeId;

        /**
         * The new path prefix, null to leave it alone.
         */
        private String path;

        /**
         * The application to move the route to, null to leave it alone.
         */
        private String applicationId;

        /**
         * The euclid module to move the route to, null to leave it alone.
         */
        private String moduleTarget;

        /**
         * The action a module route answers for, null to leave it alone.
         */
        private String moduleAction;

        /**
         * The new method list, null to leave it alone.
         */
        private List<String> methods;

        /**
         * The new region, null to leave it alone.
         */
        private String region;

        /**
         * The new namespace, null to leave it alone.
         */
        private String namespace;

        /**
         * What a caller must present, null to leave it alone.
         */
        private String authentication;

        /**
         * Whether the gateway serves the route, null to leave it alone.
         */
        private Boolean active;

        /**
         * Sets the route to change.
         *
         * @param routeId the route ID
         * @return the builder instance
         */
        public Builder routeId(String routeId) {
            this.routeId = routeId;
            return this;
        }

        /**
         * Sets the new path prefix.
         *
         * @param path the path prefix, which has to start with {@code /}
         * @return the builder instance
         */
        public Builder path(String path) {
            this.path = path;
            return this;
        }

        /**
         * Moves the route to an application, clearing its module target and action.
         *
         * @param applicationId the application ID, which has to exist already
         * @return the builder instance
         */
        public Builder applicationId(String applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        /**
         * Moves the route to a euclid module, clearing its application.
         *
         * @param moduleTarget the module, e.g. {@code "eam"}
         * @return the builder instance
         */
        public Builder moduleTarget(String moduleTarget) {
            this.moduleTarget = moduleTarget;
            return this;
        }

        /**
         * Sets the action a module route answers for.
         *
         * @param moduleAction the action, e.g. {@code "login"}
         * @return the builder instance
         */
        public Builder moduleAction(String moduleAction) {
            this.moduleAction = moduleAction;
            return this;
        }

        /**
         * Sets the methods this route answers for.
         *
         * @param methods the methods; an empty list answers for all of them
         * @return the builder instance
         */
        public Builder methods(List<String> methods) {
            this.methods = methods;
            return this;
        }

        /**
         * Sets the region requests carried by this route act in.
         *
         * @param region the region
         * @return the builder instance
         */
        public Builder region(String region) {
            this.region = region;
            return this;
        }

        /**
         * Sets the namespace requests carried by this route act in.
         *
         * @param namespace the namespace
         * @return the builder instance
         */
        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        /**
         * Sets what a caller must present before a request is forwarded.
         *
         * @param authentication the kind of credential required
         * @return the builder instance
         */
        public Builder authentication(RouteAuthentication authentication) {
            this.authentication = authentication == null ? null : authentication.wireValue();
            return this;
        }

        /**
         * Sets whether the gateway serves the route.
         *
         * @param active {@code false} to take it out of service
         * @return the builder instance
         */
        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        /**
         * Builds and returns a new instance of UpdateRouteRequest using the properties set on the Builder.
         *
         * @return a new UpdateRouteRequest instance.
         */
        public UpdateRouteRequest build() {
            return new UpdateRouteRequest(routeId, path, applicationId, moduleTarget, moduleAction, methods, region,
                    namespace, authentication, active);
        }
    }
}
