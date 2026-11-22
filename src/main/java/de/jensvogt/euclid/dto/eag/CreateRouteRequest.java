package de.jensvogt.euclid.dto.eag;

import de.jensvogt.euclid.dto.eag.model.RouteAuthentication;
import de.jensvogt.euclid.dto.eag.model.RouteType;

import java.util.List;

/**
 * Request to publish a path on the API gateway.
 *
 * <p>A route names either an application euclid runs or a euclid module, never both and never
 * neither - those are reached in entirely different ways. A module route also has to name the one
 * action it answers for. An {@link RouteType#UPLOAD} route is the exception: the gateway is the
 * endpoint rather than a way to one, so it names a bucket instead of either - and naming a backend
 * as well is refused, since it says the author expected the request to be forwarded and it will
 * not be.
 *
 * <p>The unset fields are {@code null} rather than {@code ""}, and
 * {@link de.jensvogt.euclid.module.eag.EuclidEag} serializes with a mapper that drops them, so an
 * omitted {@code region} or {@code namespace} is defaulted by the server - to the creating
 * principal's region, and to the namespace the request is made in - rather than being set to
 * nothing.
 *
 * @param routeId        the name to manage this route under, unique within its account and namespace.
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
 * @param type           what the gateway does with a matching request, as a
 *                       {@link RouteType#wireValue()}, or {@code null} for {@link RouteType#PROXY}.
 *                       An upload route names a bucket and neither an application nor a module,
 *                       and must require a credential - one with {@link RouteAuthentication#NONE}
 *                       would be a public write endpoint into somebody's bucket, so it is refused
 *                       with 400 rather than defaulted
 * @param bucket         upload routes only: ERN of the bucket bodies are written to. Required for an
 *                       upload route and refused on any other; a bucket that does not exist is
 *                       refused with 404 now, rather than after a caller has sent a whole upload
 * @param keyPrefix      upload routes only: prefix every key is confined to, or {@code null} for the
 *                       whole bucket - which lets anybody who may upload at all overwrite anything
 *                       the bucket holds
 * @param maxBytes       upload routes only: largest body accepted, in bytes, or {@code null} or 0 for
 *                       no limit beyond the listener's. Must not be negative
 * @param partSize       upload routes only: bytes per part streamed to ESM, and the size under which
 *                       a body is stored in one piece, or {@code null} for the server's 5 MB. Must
 *                       be greater than zero
 * @param contentTypes   upload routes only: content types accepted, or {@code null} for any
 * @param timeout        seconds the backend has to answer, from 0 to 3600, or {@code null} or 0 for
 *                       the gateway's default. Anything else is refused with 400 rather than
 *                       clamped, since a route cut off sooner than its author was told is worse
 *                       than one that was never created
 */
public record CreateRouteRequest(String routeId, String path, String applicationId, String moduleTarget,
                                 String moduleAction, List<String> methods, String region, String namespace,
                                 String authentication, Boolean active, String type, String bucket,
                                 String keyPrefix, Long maxBytes, Long partSize, List<String> contentTypes,
                                 Long timeout) {

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
         * What the gateway does with a matching request; null for PROXY.
         */
        private String type;

        /**
         * ERN of the bucket an upload route writes to.
         */
        private String bucket;

        /**
         * Prefix every key an upload route writes is confined to.
         */
        private String keyPrefix;

        /**
         * Largest body an upload route accepts, in bytes.
         */
        private Long maxBytes;

        /**
         * Bytes per part an upload route streams to ESM.
         */
        private Long partSize;

        /**
         * Content types an upload route accepts.
         */
        private List<String> contentTypes;

        /**
         * Seconds the backend has to answer.
         */
        private Long timeout;

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
         * Sets what the gateway does with a request this route matches.
         *
         * @param type {@link RouteType#PROXY} to forward it, {@link RouteType#UPLOAD} to write its
         *             body into {@link #bucket(String)}
         * @return the builder instance
         */
        public Builder type(RouteType type) {
            this.type = type == null ? null : type.wireValue();
            return this;
        }

        /**
         * Sets the bucket an upload route writes to.
         *
         * @param bucket the bucket's ERN - not its name
         * @return the builder instance
         */
        public Builder bucket(String bucket) {
            this.bucket = bucket;
            return this;
        }

        /**
         * Sets the prefix every key an upload route writes is confined to.
         *
         * @param keyPrefix the prefix, e.g. {@code "incoming/"}
         * @return the builder instance
         */
        public Builder keyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
            return this;
        }

        /**
         * Sets the largest body an upload route accepts.
         *
         * @param maxBytes the limit in bytes, or 0 for none beyond the listener's
         * @return the builder instance
         */
        public Builder maxBytes(long maxBytes) {
            this.maxBytes = maxBytes;
            return this;
        }

        /**
         * Sets the bytes per part an upload route streams to ESM.
         *
         * @param partSize the part size in bytes, which also decides when a body is small enough
         *                 to be stored in one piece
         * @return the builder instance
         */
        public Builder partSize(long partSize) {
            this.partSize = partSize;
            return this;
        }

        /**
         * Sets the content types an upload route accepts.
         *
         * @param contentTypes the accepted types, e.g. {@code List.of("application/xml")}; empty
         *                     accepts any
         * @return the builder instance
         */
        public Builder contentTypes(List<String> contentTypes) {
            this.contentTypes = contentTypes;
            return this;
        }

        /**
         * Sets how long the backend has to answer a request on this route.
         *
         * @param timeout seconds, from 0 to 3600; 0 follows the gateway's default
         * @return the builder instance
         */
        public Builder timeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Builds and returns a new instance of CreateRouteRequest using the properties set on the Builder.
         *
         * @return a new CreateRouteRequest instance.
         */
        public CreateRouteRequest build() {
            return new CreateRouteRequest(routeId, path, applicationId, moduleTarget, moduleAction, methods, region,
                    namespace, authentication, active, type, bucket, keyPrefix, maxBytes, partSize, contentTypes,
                    timeout);
        }
    }
}
