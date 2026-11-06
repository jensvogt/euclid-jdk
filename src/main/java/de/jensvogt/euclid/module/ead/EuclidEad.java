package de.jensvogt.euclid.module.ead;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jensvogt.euclid.auth.CredentialsFileTokens;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.auth.SigningScheme;
import de.jensvogt.euclid.auth.SigningSchemeSelectable;
import de.jensvogt.euclid.auth.TokenRefreshable;
import de.jensvogt.euclid.dto.ead.CountEventsRequest;
import de.jensvogt.euclid.dto.ead.ListEventsRequest;
import de.jensvogt.euclid.dto.ead.ListEventsResponse;
import de.jensvogt.euclid.dto.ead.PurgeEventsRequest;
import de.jensvogt.euclid.dto.ead.model.AuditEvent;
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
 * EAD (audit) operations for an authenticated {@link de.jensvogt.euclid.module.eam.EuclidSession}:
 * the trail of what was run, by whom, in which account and namespace, with the parameters it
 * carried and the status it was answered with.
 * <p>
 * What is in the trail: everything that changed something, and everything that was refused or
 * failed whatever its kind - a delete somebody was not allowed to make is the entry this exists
 * for. Successful reads are left out unless the installation sets
 * {@code euclid.modules.ead.audit-reads}, because on a working system they outnumber everything
 * else by orders of magnitude. Parameters are redacted before they are stored, so a password is
 * never in the trail to be read back here.
 * <p>
 * Reads are always confined to the caller's own account, whatever a filter says: an audit that let
 * one account read another's trail would hand over the shape of somebody else's installation. EAD
 * is role-gated like the ordinary modules, so each action needs the matching {@code ead:*}
 * permission ({@code ead:list-events}, {@code ead:count-events}, {@code ead:purge-events}) and a
 * caller without it is answered with HTTP 403.
 */
public final class EuclidEad implements TokenRefreshable, SigningSchemeSelectable {

    /**
     * A singleton instance of {@code ObjectMapper} from the Jackson library used for
     * serializing Java objects to JSON and deserializing JSON to Java objects.
     * <p>
     * Configured to leave null fields out of the serialized request entirely rather than writing
     * them as {@code null}. EAD reads an absent filter as "no filter", so an unset user, module or
     * command must not reach it at all.
     */
    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    /**
     * The Euclid service every request from this class is addressed to, sent as the
     * {@code x-euclid-target} header.
     */
    private static final String TARGET = "ead";

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
     * The namespace requests are scoped to, sent as the {@code x-euclid-namespace} header. EAD does
     * not narrow its reads by it - the trail is the account's - but the header is sent as on every
     * other module, so the request is authorized against the same scope.
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
     * Constructs an EAD client. Normally obtained from
     * {@link de.jensvogt.euclid.module.eam.EuclidSession#ead()} rather than built directly.
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
    public EuclidEad(String baseUrl, String token, String region, String accountId, String userId,
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
     * Lists the first page of the caller's account's audit trail, newest first, unfiltered and ten
     * entries to a page.
     *
     * @return a {@code ListEventsResponse} carrying the entries and how many match
     * @throws EuclidServiceException with HTTP 401 if the session is not authenticated, or 403 if
     *                                the caller lacks {@code ead:list-events}
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ListEventsResponse listEvents() throws IOException, InterruptedException {
        return listEvents(ListEventsRequest.builder().build());
    }

    /**
     * Lists a page of the audit trail, newest first, optionally narrowed to a user, a module and a
     * command.
     * <p>
     * Confined to the caller's own account whatever the filter says. The total that comes back is
     * capped rather than exact on a large trail - see {@link ListEventsResponse#totalExact()} - so
     * a caller that needs the real number asks {@link #countEvents(CountEventsRequest)}.
     *
     * @param request the filters and the page to return
     * @return a {@code ListEventsResponse} carrying the entries and how many match
     * @throws EuclidServiceException with HTTP 400 if the body cannot be parsed, 401 if the session
     *                                is not authenticated, or 403 if the caller lacks
     *                                {@code ead:list-events}
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ListEventsResponse listEvents(ListEventsRequest request) throws IOException, InterruptedException {
        JsonNode root = post("list-events", OBJECT_MAPPER.writeValueAsString(request));
        return new ListEventsResponse(toEventList(root.get("events")), root.path("total").asLong(0),
                root.path("totalExact").asBoolean(true));
    }

    /**
     * Counts every audit entry in the caller's account.
     *
     * @return the number of entries
     * @throws EuclidServiceException with HTTP 401 if the session is not authenticated, or 403 if
     *                                the caller lacks {@code ead:count-events}
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public long countEvents() throws IOException, InterruptedException {
        return countEvents(CountEventsRequest.builder().build());
    }

    /**
     * Counts audit entries without listing them, within the caller's own account and narrowed by
     * the same filters {@link #listEvents(ListEventsRequest)} takes.
     * <p>
     * Exact, unlike the total a listing comes back with - which is also why it costs more on a
     * large trail, and why a pager should not call it for every page.
     *
     * @param request the filters to count under
     * @return the number of matching entries
     * @throws EuclidServiceException with HTTP 400 if the body cannot be parsed, 401 if the session
     *                                is not authenticated, or 403 if the caller lacks
     *                                {@code ead:count-events}
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public long countEvents(CountEventsRequest request) throws IOException, InterruptedException {
        return post("count-events", OBJECT_MAPPER.writeValueAsString(request)).path("count").asLong(0);
    }

    /**
     * Removes audit entries older than the given age.
     * <p>
     * Retention is normally the database's own job - {@code euclid.modules.ead.retention} puts an
     * expiry on each entry as it is written - so this is for the installation that changed its mind
     * and needs a window gone now. Note that, unlike the reads, the purge is not confined to the
     * caller's account: it prunes the installation's trail, which is why it sits behind its own
     * permission. The purge is itself logged and audited.
     *
     * @param olderThanDays remove entries older than this many days; must be positive
     * @return how many entries were removed
     * @throws EuclidServiceException with HTTP 400 if {@code olderThanDays} is not positive - a
     *                                purge with no window would remove the whole trail - 401 if the
     *                                session is not authenticated, or 403 if the caller lacks
     *                                {@code ead:purge-events}
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public long purgeEvents(long olderThanDays) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(new PurgeEventsRequest(olderThanDays));
        return post("purge-events", body).path("removed").asLong(0);
    }

    /**
     * Posts one of EAD's actions and parses the response body, since every one of them takes a JSON
     * request and answers with JSON.
     *
     * @param action the EAD action to post
     * @param body the JSON request body
     * @return the parsed response body
     * @throws EuclidServiceException if the server answers with a non-2xx status
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
     * Converts a JsonNode holding an array of audit events into a list of AuditEvent instances.
     *
     * @param eventsNode the JsonNode representing the array of events
     * @return a list of AuditEvent instances, or an empty list if the node is null or not an array
     */
    private static List<AuditEvent> toEventList(JsonNode eventsNode) {
        List<AuditEvent> events = new ArrayList<>();
        if (eventsNode != null && eventsNode.isArray()) {
            for (JsonNode eventNode : eventsNode) {
                events.add(toEvent(eventNode));
            }
        }
        return events;
    }

    /**
     * Builds an {@link AuditEvent} from one entry of a list-events response.
     *
     * @param node the JSON object describing the event
     * @return the parsed event
     */
    private static AuditEvent toEvent(JsonNode node) {
        return new AuditEvent(
                textOrNull(node, "accountId"),
                textOrNull(node, "namespace"),
                textOrNull(node, "userId"),
                textOrNull(node, "module"),
                textOrNull(node, "command"),
                textOrNull(node, "parameters"),
                node.path("status").asLong(0),
                textOrNull(node, "created"));
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
