package de.jensvogt.euclid.module.ets;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jensvogt.euclid.auth.CredentialsFileTokens;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.auth.SigningScheme;
import de.jensvogt.euclid.auth.SigningSchemeSelectable;
import de.jensvogt.euclid.auth.TokenRefreshable;
import de.jensvogt.euclid.dto.ets.CreateServerRequest;
import de.jensvogt.euclid.dto.ets.ListServersRequest;
import de.jensvogt.euclid.dto.ets.ServerRequest;
import de.jensvogt.euclid.dto.ets.UpdateServerRequest;
import de.jensvogt.euclid.dto.ets.model.TransferServer;
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
 * ETS (transfer server) operations for an authenticated
 * {@link de.jensvogt.euclid.module.eam.EuclidSession}. Mirrors euclid-cli's {@code EtsCli}.
 * <p>
 * A transfer server is an FTP or SFTP listener fronting an ESM bucket: clients authenticate with
 * their EAM credentials and whatever they upload becomes an object in that bucket. Every action
 * here changes - or reveals - which network listeners exist and who may reach which bucket through
 * them, so the server requires administrator privileges for all of them and answers HTTP 403
 * otherwise.
 * <p>
 * Starting and stopping only record intent. {@link #startServer} and {@link #stopServer} set the
 * server's {@code desiredState}; euclid-mgr's reconciler is what actually launches or tears down
 * the process, so {@link TransferServer#state()} can lag {@link TransferServer#desiredState()}
 * briefly after either call.
 */
public final class EuclidEts implements TokenRefreshable, SigningSchemeSelectable {

    /**
     * A singleton instance of {@code ObjectMapper} from the Jackson library used for
     * serializing Java objects to JSON and deserializing JSON to Java objects.
     * <p>
     * Configured to leave null fields out of the serialized request entirely rather than writing
     * them as {@code null}. ETS distinguishes an absent field from a present one: update-server
     * only touches the fields it receives, and sending {@code "userIds": null} would be read as an
     * empty list and silently clear the server's access list rather than leave it alone.
     */
    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    /**
     * The Euclid service every request from this class is addressed to, sent as the
     * {@code x-euclid-target} header.
     */
    private static final String TARGET = "ets";

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
     * The namespace requests are scoped to, sent as the {@code x-euclid-namespace} header.
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
     * Constructs an ETS client. Normally obtained from
     * {@link de.jensvogt.euclid.module.eam.EuclidSession#ets()} rather than built directly.
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
    public EuclidEts(String baseUrl, String token, String region, String accountId, String userId,
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
     * Defines a new transfer server. It is created stopped - call {@link #startServer} to run it.
     * <p>
     * The bucket is resolved now rather than at start-up, so a name that matches nothing is
     * reported here as HTTP 404 instead of surfacing later as a server that runs but stores
     * nothing. A port already claimed by another transfer server is refused with HTTP 409.
     * <p>
     * At least one of {@code userIds} or {@code userGroups} must be set. The server would accept a
     * definition with neither, but nobody could then log in to the resulting listener, so - as
     * euclid-cli does - that is refused here rather than sent.
     *
     * @param request the server definition; unset fields take the server's defaults
     * @return the stored definition, including the ERN and bucket ERN the server resolved
     * @throws IllegalArgumentException if the request names neither users nor user groups
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public TransferServer createServer(CreateServerRequest request) throws IOException, InterruptedException {
        if (isEmpty(request.userIds()) && isEmpty(request.userGroups())) {
            throw new IllegalArgumentException(
                    "at least one of userIds or userGroups is required, otherwise nobody can log in");
        }
        return toTransferServer(post("create-server", OBJECT_MAPPER.writeValueAsString(request)));
    }

    /**
     * Changes an existing transfer server. Only the fields set on the request are sent, and the
     * server only touches the fields it receives, so one setting can be flipped without resending
     * the whole definition.
     * <p>
     * A field that is set replaces the current value outright - passing {@code userIds} substitutes
     * the whole access list rather than adding to it.
     *
     * @param request the change to apply; only {@code serverId} is required
     * @return the stored definition after the change
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public TransferServer updateServer(UpdateServerRequest request) throws IOException, InterruptedException {
        return toTransferServer(post("update-server", OBJECT_MAPPER.writeValueAsString(request)));
    }

    /**
     * Lists every transfer server.
     *
     * @return the defined transfer servers
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public List<TransferServer> listServers() throws IOException, InterruptedException {
        return listServers("");
    }

    /**
     * Lists the transfer servers whose ID starts with the given prefix.
     *
     * @param prefix only servers whose ID starts with this prefix are returned; empty lists them all
     * @return the matching transfer servers
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public List<TransferServer> listServers(String prefix) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(ListServersRequest.builder().prefix(prefix).build());
        JsonNode root = post("list-servers", body);

        List<TransferServer> servers = new ArrayList<>();
        JsonNode serversNode = root.get("servers");
        if (serversNode != null && serversNode.isArray()) {
            for (JsonNode serverNode : serversNode) {
                servers.add(toTransferServer(serverNode));
            }
        }
        return servers;
    }

    /**
     * Retrieves a single transfer server by ID.
     *
     * @param serverId the ID of the transfer server
     * @return the server's definition
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public TransferServer getServer(String serverId) throws IOException, InterruptedException {
        return toTransferServer(post("get-server", serverBody(serverId)));
    }

    /**
     * Deletes a transfer server's definition, which is also what stops it: the reconciler runs
     * whatever is defined and RUNNING, so a definition that no longer exists is torn down on its
     * next tick.
     *
     * @param serverId the ID of the transfer server to delete
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public void deleteServer(String serverId) throws IOException, InterruptedException {
        post("delete-server", serverBody(serverId));
    }

    /**
     * Records that a transfer server should be running. euclid-mgr's reconciler is what launches
     * the process, so the returned {@link TransferServer#state()} may still read {@code "STOPPED"}
     * even though {@link TransferServer#desiredState()} is now {@code "RUNNING"}.
     *
     * @param serverId the ID of the transfer server to start
     * @return the stored definition, with its new desired state
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public TransferServer startServer(String serverId) throws IOException, InterruptedException {
        return toTransferServer(post("start-server", serverBody(serverId)));
    }

    /**
     * Records that a transfer server should be stopped, leaving its definition in place. The
     * reconciler tears the process down; see {@link #startServer} on the lag between desired and
     * observed state.
     *
     * @param serverId the ID of the transfer server to stop
     * @return the stored definition, with its new desired state
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public TransferServer stopServer(String serverId) throws IOException, InterruptedException {
        return toTransferServer(post("stop-server", serverBody(serverId)));
    }

    /**
     * Reports whether an optional list on a request was left unset or set to nothing - the two are
     * the same thing as far as "can anybody log in to this server" goes.
     *
     * @param values the list to check
     * @return {@code true} if the list is null or empty
     */
    private static boolean isEmpty(List<String> values) {
        return values == null || values.isEmpty();
    }

    /**
     * Builds the request body for the four actions that name nothing but a server.
     *
     * @param serverId the ID of the transfer server the action applies to
     * @return the serialized request body
     * @throws IOException if the request cannot be serialized
     */
    private static String serverBody(String serverId) throws IOException {
        return OBJECT_MAPPER.writeValueAsString(ServerRequest.builder().serverId(serverId).build());
    }

    /**
     * Posts one of ETS's actions and parses the response body, since every one of them takes a JSON
     * request and answers with JSON.
     *
     * @param action the ETS action to post
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
     * Builds a {@link TransferServer} from the server definition JSON every ETS action but
     * list-servers and delete-server answers with.
     *
     * @param node the JSON object describing the server
     * @return the parsed transfer server
     */
    private static TransferServer toTransferServer(JsonNode node) {
        return new TransferServer(
                textOrNull(node, "serverId"),
                textOrNull(node, "ern"),
                textOrNull(node, "accountId"),
                textOrNull(node, "region"),
                textOrNull(node, "protocol"),
                textOrNull(node, "address"),
                node.path("port").asLong(0),
                textOrNull(node, "bucketName"),
                textOrNull(node, "bucketErn"),
                toStringList(node.get("userIds")),
                toStringList(node.get("userGroups")),
                textOrNull(node, "desiredState"),
                textOrNull(node, "state"),
                textOrNull(node, "hostKey"),
                node.path("pasvMin").asLong(0),
                node.path("pasvMax").asLong(0),
                textOrNull(node, "created"),
                textOrNull(node, "modified"));
    }

    /**
     * Converts a JsonNode holding an array of strings into a list.
     *
     * @param node the JsonNode to convert
     * @return the list of strings, or an empty list if the node is null or not an array
     */
    private static List<String> toStringList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode element : node) {
                values.add(element.asText());
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
     * Reads a text field from a JSON node, tolerating both an absent field and an explicit null.
     *
     * @param node the node to read from
     * @param field the field name
     * @return the field's text value, or {@code null} if it is absent or null
     */
    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
