package de.jensvogt.euclid.dto.eag;

import de.jensvogt.euclid.dto.eag.model.RouteAuthentication;

import java.util.List;

/**
 * Request to publish a path on the API gateway.
 *
 * <p>A route names either an application euclid runs or a euclid module, never both and never
 * neither - those are reached in entirely different ways. A module route also has to name the one
 * action it answers for.
 *
 * <p>The unset fields are {@code null} rather than {@code ""}, and
 * {@link de.jensvogt.euclid.module.eag.EuclidEag} serializes with a mapper that drops them, so an
 * omitted {@code region} or {@code namespace} is defaulted by the server - to the creating
 * principal's region, and to the namespace the request is made in - rather than being set to
 * nothing.
 *
 * @param routeId        the name to manage this route under, unique across the installation.
 *                       Required, and it never appears in a URL
 * @param path           the path prefix to publish, e.g. {@code /resource}. Required, and it has to
 *                       start with {@code /} - one that does not could never match a request target
 *                       and would become a route that is configured, listed and silently dead
 * @param applicationId  the application requests are sent to, as deployed through EAP. It has to
 *                       exist already: a route pointing at nothing answers 503 for every request,
 *                       which looks like an application that is down rather than one that was never
 *                       deployed, so this is refused with 404 instead
 * @param moduleTarget   the euclid module to reach instead of an application, e.g. {@code "eam"}
 * @param moduleAction   the one action {@code moduleTarget} answers for, e.g. {@code "login"}.
 *                       Required when a module is named
 * @param methods        the HTTP methods this route answers for; {@code null} or empty answers for
 *                       all of them. Two routes may share a path only if their methods do not
 *                       overlap, which is how reads and writes of one resource go to different
 *                       applications; an overlap is refused with 409
 * @param region         the region requests carried by this route act in, or {@code null} for the
 *                       creating principal's
 * @param namespace      the namespace requests carried by this route act in, or {@code null} for
 *                       the one the request is made in
 * @param authentication what a caller must present, as a {@link RouteAuthentication#wireValue()},
 *                       or {@code null} for {@link RouteAuthentication#NONE}
 * @param active         whether the gateway serves the route at once, or {@code null} for yes
 */
public record CreateRouteRequest(String routeId, String path, String applicationId, String moduleTarget,
                                 String moduleAction, List<String> methods, String region, String namespace,
                                 String authentication, Boolean active) {

    /**
     * Creates a new instance of the Builder for constructing a CreateRouteRequest object.
     *
     * @return a new Builder instance for constructing CreateRouteRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CreateRouteRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The name to manage this route under.
         */
        private String routeId;

        /**
         * The path prefix to publish.
         */
        private String path;

        /**
         * The application requests are sent to.
         */
        private String applicationId;

        /**
         * The euclid module to reach instead of an application.
         */
        private String moduleTarget;

        /**
         * The one action the module answers for.
         */
        private String moduleAction;

        /**
         * The HTTP methods this route answers for; null answers for all.
         */
        private List<String> methods;

        /**
         * The region requests carried by this route act in; null for the creating principal's.
         */
        private String region;

        /**
         * The namespace requests carried by this route act in; null for the request's own.
         */
        private String namespace;

        /**
         * What a caller must present; null for NONE.
         */
        private String authentication;

        /**
         * Whether the gateway serves the route at once; null for yes.
         */
        private Boolean active;

        /**
         * Sets the name to manage this route under.
         *
         * @param routeId the route ID
         * @return the builder instance
         */
        public Builder routeId(String routeId) {
            this.routeId = routeId;
            return this;
        }

        /**
         * Sets the path prefix to publish.
         *
         * @param path the path prefix, which has to start with {@code /}
         * @return the builder instance
         */
        public Builder path(String path) {
            this.path = path;
            return this;
        }

        /**
         * Sets the application requests are sent to. Exclusive with
         * {@link #moduleTarget(String)}.
         *
         * @param applicationId the application ID, which has to exist already
         * @return the builder instance
         */
        public Builder applicationId(String applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        /**
         * Sets the euclid module to reach instead of an application. Exclusive with
         * {@link #applicationId(String)}, and needs a {@link #moduleAction(String)}.
         *
         * @param moduleTarget the module, e.g. {@code "eam"}
         * @return the builder instance
         */
        public Builder moduleTarget(String moduleTarget) {
            this.moduleTarget = moduleTarget;
            return this;
        }

        /**
         * Sets the one action the module answers for on this route.
         *
         * @param moduleAction the action, e.g. {@code "login"}
         * @return the builder instance
         */
        public Builder moduleAction(String moduleAction) {
            this.moduleAction = moduleAction;
            return this;
        }

        /**
         * Sets the HTTP methods this route answers for.
         *
         * @param methods the methods; empty or unset answers for all of them
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
         * Sets whether the gateway serves the route at once.
         *
         * @param active {@code false} to create it out of service
         * @return the builder instance
         */
        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        /**
         * Builds and returns a new instance of CreateRouteRequest using the properties set on the Builder.
         *
         * @return a new CreateRouteRequest instance.
         */
        public CreateRouteRequest build() {
            return new CreateRouteRequest(routeId, path, applicationId, moduleTarget, moduleAction, methods, region,
                    namespace, authentication, active);
        }
    }
}
