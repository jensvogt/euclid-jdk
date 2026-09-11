package de.jensvogt.euclid.module.eag;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jensvogt.euclid.auth.CredentialsFileTokens;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.auth.SigningScheme;
import de.jensvogt.euclid.auth.SigningSchemeSelectable;
import de.jensvogt.euclid.auth.TokenRefreshable;
import de.jensvogt.euclid.dto.eag.CreateRouteRequest;
import de.jensvogt.euclid.dto.eag.ListListenersResponse;
import de.jensvogt.euclid.dto.eag.ListRoutesRequest;
import de.jensvogt.euclid.dto.eag.RouteIdRequest;
import de.jensvogt.euclid.dto.eag.UpdateRouteRequest;
import de.jensvogt.euclid.dto.eag.model.Listener;
import de.jensvogt.euclid.dto.eag.model.ListenerCertificate;
import de.jensvogt.euclid.dto.eag.model.Protocol;
import de.jensvogt.euclid.dto.eag.model.Route;
import de.jensvogt.euclid.dto.eag.model.RouteAuthentication;
import de.jensvogt.euclid.exception.EuclidServiceException;
import de.jensvogt.euclid.http.EuclidHttpClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * EAG (API gateway) operations for an authenticated
 * {@link de.jensvogt.euclid.module.eam.EuclidSession}: the routes the gateway publishes to the
 * outside world, and the listeners it publishes them on.
 * <p>
 * The API gateway is a second, quite different listener from the one this client otherwise talks
 * to. Where euclid's own gateway speaks euclid's protocol - a target, an action and a JSON body -
 * the API gateway speaks whatever the application behind it speaks, and decides per route who may
 * reach it. A caller asks for {@code /resource/searchById?id=123} and has no idea which application
 * answers; that is what a route says, and it is why the application's name never appears in the
 * URL.
 * <p>
 * A route names either an application euclid runs or a euclid module, never both. The second kind
 * is the way in for something outside euclid that needs euclid itself - a browser that has to log
 * in before it can call anything, most of all - and it names the one action it answers for, so that
 * what is exposed is exactly what somebody wrote down rather than every action the module has.
 * <p>
 * Matching is by longest path prefix on whole segments: a route on {@code /resource} carries
 * everything beneath it but never claims {@code /resource-intern}, and a more specific
 * {@code /resource/export} can be carved out of it later without either being reordered. Two routes
 * may share a path only if their methods do not overlap, which is how reads and writes of one
 * resource go to different applications; an overlap is refused with 409 rather than resolved by
 * whichever the gateway happened to consider first.
 * <p>
 * Every action here decides what is exposed to the outside world and on what terms, so every one of
 * them is administrator-only server-side: a session whose {@link
 * de.jensvogt.euclid.module.eam.EuclidSession#isAdmin()} is false gets HTTP 403 from all of them.
 */
public final class EuclidEag implements TokenRefreshable, SigningSchemeSelectable {

    /**
     * A singleton instance of {@code ObjectMapper} from the Jackson library used for
     * serializing Java objects to JSON and deserializing JSON to Java objects.
     * <p>
     * Configured to leave null fields out of the serialized request entirely rather than writing
     * them as {@code null}. EAG distinguishes an absent field from a present one: update-route only
     * touches the fields it receives, and on create an omitted region or namespace is defaulted by
     * the server rather than set to nothing.
     */
    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    /**
     * The Euclid service every request from this class is addressed to, sent as the
     * {@code x-euclid-target} header.
     */
    private static final String TARGET = "eag";

    /**
     * The base URL of the Euclid server this instance talks to.
     */
    private final String baseUrl;

    /**
     * Supplies the bearer token for each request, used when no SigV4 access key is configured.
     *
     * <p>A supplier rather than a string so that a token which expires can be replaced without
     * rebuilding the client - see {@link TokenRefreshable#token(Supplier)}. A client built inside an
     * application euclid deployed follows the credentials file euclid rewrites; anywhere else it
     * holds a supplier returning the token it was given - see
     * {@link CredentialsFileTokens#forClient(String, String)}.
     */
    private volatile Supplier<String> token;

    /**
     * The region requests are made in.
     */
    private final String region;

    /**
     * The account requests are made on behalf of.
     */
    private final String accountId;

    /**
     * The user requests are made on behalf of.
     */
    private final String userId;

    /**
     * Public identifier of the SigV4 access key, or {@code null} to authenticate with the token.
     */
    private final String accessKeyId;

    /**
     * Secret paired with {@link #accessKeyId}, or {@code null} to authenticate with the token.
     */
    private final String secretAccessKey;

    /**
     * The namespace requests are scoped to, sent as the {@code x-euclid-namespace} header. A route
     * created without a namespace of its own takes this one, which decides both which listener
     * carries it and what scope the requests it carries act in.
     */
    private final String nameSpace;

    /**
     * The scheme requests are signed with when an access key is configured.
     *
     * <p>Defaults to SigV4, which is what euclid has always accepted; a caller pointed at a server
     * that understands RFC 9421 switches it with {@link #signingScheme(SigningScheme)}. Volatile
     * because that call can come from a different thread than the requests it affects.
     */
    private volatile SigningScheme signingScheme = SigningScheme.SIGV4;

    /**
     * The HTTP client used for every request, pre-configured with this session's TLS trust.
     */
    private final EuclidHttpClient httpClient;

    /**
     * Constructs an EAG client. Normally obtained from
     * {@link de.jensvogt.euclid.module.eam.EuclidSession#eag()} rather than built directly.
     *
     * @param baseUrl         the base URL of the Euclid server
     * @param token           the bearer token issued at login
     * @param region          the region requests are made in
     * @param accountId       the account requests are made on behalf of
     * @param userId          the user requests are made on behalf of
     * @param accessKeyId     public identifier of the SigV4 access key, or {@code null} for token auth
     * @param secretAccessKey secret paired with {@code accessKeyId}, or {@code null} for token auth
     * @param caCertPath      path to an additional PEM CA certificate to trust, or {@code null}
     * @param nameSpace       the namespace requests are scoped to, or {@code null} if unscoped
     */
    public EuclidEag(String baseUrl, String token, String region, String accountId, String userId,
                     String accessKeyId, String secretAccessKey, String caCertPath, String nameSpace) {
        this.baseUrl = baseUrl;
        this.token = CredentialsFileTokens.forClient(token, userId);
        this.region = region;
        this.accountId = accountId;
        this.userId = userId;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.nameSpace = nameSpace;
        // The header factory is what lets a request whose token or signature expired in flight be
        // built again and sent once more - see EuclidHttpClient#headerFactory.
        this.httpClient = new EuclidHttpClient(caCertPath).headerFactory(this::requestHeaders);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void token(Supplier<String> token) {
        this.token = Objects.requireNonNull(token, "token supplier must not be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void signingScheme(SigningScheme signingScheme) {
        this.signingScheme = Objects.requireNonNull(signingScheme, "signing scheme must not be null");
    }

    /**
     * Publishes a path on the gateway and sends everything beneath it to an application, for every
     * HTTP method and without requiring a credential.
     *
     * @param routeId       the name to manage this route under, unique within its account and namespace
     * @param path          the path prefix to publish, e.g. {@code /resource}
     * @param applicationId the application requests are sent to, which has to exist already
     * @return the route as it was stored
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public Route createRoute(String routeId, String path, String applicationId)
            throws IOException, InterruptedException {
        return createRoute(CreateRouteRequest.builder().routeId(routeId).path(path)
                .applicationId(applicationId).build());
    }

    /**
     * Publishes a path that reaches one action of a euclid module rather than an application.
     * <p>
     * This is the way in for something outside euclid that needs euclid itself - a browser that has
     * to log in before it can call anything. Without it a front end would talk to the API gateway
     * for the application and to euclid's own gateway for its credentials: two ports, two origins,
     * and CORS between them.
     *
     * @param routeId      the name to manage this route under
     * @param path         the path prefix to publish, e.g. {@code /euclid/login}
     * @param moduleTarget the euclid module to reach, e.g. {@code "eam"}
     * @param moduleAction the one action it answers for on this route, e.g. {@code "login"}
     * @return the route as it was stored
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public Route createModuleRoute(String routeId, String path, String moduleTarget, String moduleAction)
            throws IOException, InterruptedException {
        return createRoute(CreateRouteRequest.builder().routeId(routeId).path(path)
                .moduleTarget(moduleTarget).moduleAction(moduleAction).build());
    }

    /**
     * Publishes a path, with the methods, the scope and what a caller must present spelled out.
     * <p>
     * Refused with HTTP 409 if the route ID is taken, or if another route already answers for this
     * path and one of these methods. An application that does not exist is refused with 404 rather
     * than becoming a route that answers 503 for every request - which looks like an application
     * that is down rather than one that was never deployed.
     * <p>
     * The two conflicts are scoped differently, deliberately. A <em>route ID</em> belongs to an
     * account and a namespace, so two of them may each manage a route called {@code "suppliers"}.
     * A <em>path</em> is claimed from whoever asks for it next, installation-wide: a listener is
     * bound to a namespace and knows nothing about accounts, so two accounts publishing the same
     * path in one namespace would leave the gateway picking between them by sort order. That is
     * what the 409 refuses, and it means a path can be taken by an account whose routes this
     * client cannot see.
     * <p>
     * An {@code applicationId} is looked for in the route's own account and namespace - the pair
     * the gateway will later look in for the application's instances - so an application of that
     * name deployed elsewhere is not the one this route would reach.
     *
     * @param request the route to publish
     * @return the route as it was stored
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public Route createRoute(CreateRouteRequest request) throws IOException, InterruptedException {
        return toRoute(post("create-route", OBJECT_MAPPER.writeValueAsString(request)));
    }

    /**
     * Changes a route that already exists. Only what the request names changes, so one setting can
     * be adjusted without restating the rest.
     * <p>
     * The distinction the server draws is between a field being absent and being present, not
     * between values - so leaving {@link UpdateRouteRequest#path()} null leaves the stored path
     * alone. Moving a route to an application clears its module target and the other way round,
     * since leaving both set would make which one wins depend on the proxy.
     *
     * @param request what to change about the route
     * @return the route as it stands afterwards
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public Route updateRoute(UpdateRouteRequest request) throws IOException, InterruptedException {
        return toRoute(post("update-route", OBJECT_MAPPER.writeValueAsString(request)));
    }

    /**
     * Takes a route out of service, or puts it back, without changing anything else about it.
     * <p>
     * This is how something stops being exposed in a hurry: the route stays exactly as it was and
     * comes back the same when it is reactivated, which deleting and recreating it would not
     * guarantee.
     *
     * @param routeId the route to change
     * @param active  {@code false} to stop serving it, {@code true} to serve it again
     * @return the route as it stands afterwards
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public Route setRouteActive(String routeId, boolean active) throws IOException, InterruptedException {
        return updateRoute(UpdateRouteRequest.builder().routeId(routeId).active(active).build());
    }

    /**
     * Lists the routes of this client's account and namespace.
     * <p>
     * Not every route the gateway serves: a listener carries the routes of its own namespace, and
     * every other action here resolves a route ID in the namespace the request was made in, so a
     * listing that crossed namespaces would show routes this client cannot address.
     *
     * @return the routes, in no particular order
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public List<Route> listRoutes() throws IOException, InterruptedException {
        return listRoutes("");
    }

    /**
     * Lists the routes whose path starts with a prefix - what somebody asking "what is published
     * under {@code /api}" wants.
     * <p>
     * The prefix filters on the path rather than on the route ID, and is matched literally rather
     * than as a pattern, so {@code /api/v1.0} does not also match {@code /api/v1X0}.
     *
     * @param pathPrefix only routes whose path starts with this are returned; empty returns all
     * @return the matching routes
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public List<Route> listRoutes(String pathPrefix) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(ListRoutesRequest.builder().prefix(pathPrefix).build());
        return toRouteList(post("list-routes", body).get("routes"));
    }

    /**
     * Reads one route by its ID.
     *
     * @param routeId the route to read
     * @return the route
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public Route getRoute(String routeId) throws IOException, InterruptedException {
        return toRoute(post("get-route", routeIdBody(routeId)));
    }

    /**
     * Deletes a route, which stops the gateway serving its path.
     * <p>
     * Deleting is not how something is taken out of service temporarily - see
     * {@link #setRouteActive}, which leaves the route as it was so it returns exactly the same.
     *
     * @param routeId the route to delete
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public void deleteRoute(String routeId) throws IOException, InterruptedException {
        post("delete-route", routeIdBody(routeId));
    }

    /**
     * Reports what the gateway was configured to serve, and whether it is serving it.
     * <p>
     * A listener whose port was taken, or whose certificate could not be loaded, is still listed -
     * it is the one somebody is looking for - but
     * {@link ListListenersResponse#serving()} is what says whether anything is actually bound. For
     * an HTTPS listener the certificate comes with it, including whether euclid minted it itself:
     * callers reject a self-signed certificate until they are given it, so that decides whether the
     * port works for anybody who has not been told about it.
     *
     * @return the configured listeners
     * @throws IOException          if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ListListenersResponse listListeners() throws IOException, InterruptedException {
        JsonNode root = post("list-listeners", "{}");
        List<Listener> listeners = toListenerList(root.get("listeners"));
        return ListListenersResponse.builder().listeners(listeners)
                .total(root.path("total").asLong(listeners.size()))
                .serving(root.path("serving").asBoolean(false)).build();
    }

    /**
     * Builds the request body for the two actions that name nothing but a route.
     *
     * @param routeId the route the action applies to
     * @return the serialized request body
     * @throws IOException if the request cannot be serialized
     */
    private static String routeIdBody(String routeId) throws IOException {
        return OBJECT_MAPPER.writeValueAsString(RouteIdRequest.builder().routeId(routeId).build());
    }

    /**
     * Posts one of EAG's actions and parses the response body, since every one of them takes a JSON
     * request and answers with JSON - or, for delete-route, with nothing, which reads back as a
     * missing node rather than failing.
     *
     * @param action the EAG action to post
     * @param body the JSON request body
     * @return the parsed response body
     * @throws IOException if an I/O error occurs during the request
     * @throws InterruptedException if the operation is interrupted while waiting for the response
     */
    private JsonNode post(String action, String body) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, TARGET, action,
                requestHeaders(action, body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException(TARGET, action, response.statusCode(), response.body());
        }

        return OBJECT_MAPPER.readTree(response.body());
    }

    /**
     * Converts a JsonNode holding an array of routes into a list of them.
     *
     * @param routesNode the JsonNode representing the array of routes
     * @return the parsed routes, or an empty list if the node is null or not an array
     */
    private static List<Route> toRouteList(JsonNode routesNode) {
        List<Route> routes = new ArrayList<>();
        if (routesNode != null && routesNode.isArray()) {
            for (JsonNode routeNode : routesNode) {
                routes.add(toRoute(routeNode));
            }
        }
        return routes;
    }

    /**
     * Builds a {@link Route} from the route JSON every route action but list-routes answers with
     * directly.
     *
     * @param node the JSON object describing the route
     * @return the parsed route, or {@code null} if the node is null or not an object
     */
    private static Route toRoute(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        return new Route(
                textOrNull(node, "routeId"),
                textOrNull(node, "ern"),
                textOrNull(node, "accountId"),
                textOrNull(node, "region"),
                textOrNull(node, "namespace"),
                textOrNull(node, "path"),
                textOrNull(node, "applicationId"),
                textOrNull(node, "moduleTarget"),
                textOrNull(node, "moduleAction"),
                toStringList(node.get("methods")),
                RouteAuthentication.fromWireValue(textOrNull(node, "authentication")),
                node.path("active").asBoolean(true),
                textOrNull(node, "created"),
                textOrNull(node, "modified"));
    }

    /**
     * Converts a JsonNode holding an array of listeners into a list of them.
     *
     * @param listenersNode the JsonNode representing the array of listeners
     * @return the parsed listeners, or an empty list if the node is null or not an array
     */
    private static List<Listener> toListenerList(JsonNode listenersNode) {
        List<Listener> listeners = new ArrayList<>();
        if (listenersNode != null && listenersNode.isArray()) {
            for (JsonNode listenerNode : listenersNode) {
                listeners.add(toListener(listenerNode));
            }
        }
        return listeners;
    }

    /**
     * Builds a {@link Listener} from one entry of the list-listeners response, gathering the
     * certificate's flat {@code certificate*} fields into a {@link ListenerCertificate}.
     *
     * @param node the JSON object describing the listener
     * @return the parsed listener, or {@code null} if the node is null or not an object
     */
    private static Listener toListener(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        return new Listener(
                textOrNull(node, "namespace"),
                node.path("port").asInt(0),
                Protocol.fromWireValue(textOrNull(node, "protocol")),
                node.path("serving").asBoolean(false),
                textOrNull(node, "certificate"),
                textOrNull(node, "certificateConfigured"),
                toListenerCertificate(node));
    }

    /**
     * Gathers a listener's certificate fields, which arrive flat alongside the listener's own.
     * <p>
     * Answers {@code null} unless the server said it found one: a plain HTTP listener has no
     * certificate to be missing, and an HTTPS one whose certificate is absent is what a port that
     * never came up looks like - neither is a certificate this can describe.
     *
     * @param node the JSON object describing the listener
     * @return the certificate, or {@code null} if there is none to report
     */
    private static ListenerCertificate toListenerCertificate(JsonNode node) {
        if (!node.path("certificateFound").asBoolean(false)) {
            return null;
        }
        return new ListenerCertificate(
                textOrNull(node, "certificateErn"),
                textOrNull(node, "certificateSubject"),
                textOrNull(node, "certificateIssuer"),
                textOrNull(node, "certificateSerialNumber"),
                textOrNull(node, "certificateFingerprint"),
                toStringList(node.get("certificateSubjectAltNames")),
                node.path("certificateGenerated").asBoolean(false),
                textOrNull(node, "certificateNotBefore"),
                textOrNull(node, "certificateNotAfter"),
                node.path("certificateExpired").asBoolean(false));
    }

    /**
     * Converts a JsonNode holding an array of strings into a list.
     *
     * @param node the JsonNode to convert
     * @return the strings, or an empty list if the node is null or not an array
     */
    private static List<String> toStringList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode value : node) {
                values.add(value.asText());
            }
        }
        return values;
    }

    /**
     * Generates a map of HTTP request headers for a specified action and request body.
     * The headers include content type, region, account ID, user ID, and
     * authentication information. If AWS credentials are available, the headers
     * are signed with the client's configured {@link SigningScheme} - SigV4 unless
     * {@link #signingScheme(SigningScheme)} says otherwise; without an access key, a Bearer token is
     * used and nothing is signed.
     *
     * @param action the action being performed by the request.
     * @param body the body of the request to be included for signing.
     * @return a map of HTTP headers constructed for the request.
     */
    private Map<String, String> requestHeaders(String action, String body) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        if (region != null) {
            headers.put("x-euclid-region", region);
        }
        if (accountId != null) {
            headers.put("x-euclid-account-id", accountId);
        }
        if (userId != null) {
            headers.put("x-euclid-user-id", userId);
        }
        if (nameSpace != null && !nameSpace.isEmpty()) {
            headers.put("x-euclid-namespace", nameSpace);
        }

        if (accessKeyId != null && !accessKeyId.isEmpty() && secretAccessKey != null && !secretAccessKey.isEmpty()) {
            SignableRequest signable = new SignableRequest("POST", "/");
            headers.forEach(signable::header);
            signable.header("host", hostHeader());
            signable.header("x-euclid-target", TARGET);
            signable.header("x-euclid-action", action);
            signable.body(body);
            signSignatureHeaders(signable, TARGET, headers);
        } else {
            headers.put("Authorization", "Bearer " + token.get());
        }
        return headers;
    }

    /**
     * Signs {@code signable} with the configured scheme and copies the headers it produced onto the
     * outgoing request.
     * <p>
     * Which headers those are is the scheme's business rather than this method's: SigV4 signs into
     * Authorization alongside two {@code x-amz-*} headers, RFC 9421 into Signature and
     * Signature-Input alongside Content-Digest. The scheme is read once into a local so that a
     * {@link #signingScheme(SigningScheme)} call arriving mid-request cannot sign with one scheme
     * and then copy the header names of the other.
     *
     * @param signable the request to sign, with every header the signature covers and the body
     *                 already set on it
     * @param service  the service to scope the signature to
     * @param headers  the outgoing headers, which the signature headers are added to in place
     */
    private void signSignatureHeaders(SignableRequest signable, String service, Map<String, String> headers) {
        signable.scheme(URI.create(baseUrl).getScheme());
        SigningScheme scheme = signingScheme;
        scheme.sign(signable, accessKeyId, secretAccessKey, region, service);
        for (String header : scheme.signatureHeaderNames()) {
            headers.put(header, signable.header(header));
        }
    }

    /**
     * Builds the {@code host} header value the request signature is computed over, including the port
     * when the base URL names one.
     *
     * @return the host header value
     */
    private String hostHeader() {
        URI uri = URI.create(baseUrl);
        int port = uri.getPort();
        return port == -1 ? uri.getHost() : uri.getHost() + ":" + port;
    }

    /**
     * Reads a string field from a JSON object, mapping both an absent field and a JSON null to
     * {@code null}.
     *
     * @param node the JSON object to read from
     * @param field the field name
     * @return the field's value, or {@code null} if it is absent or null
     */
    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
