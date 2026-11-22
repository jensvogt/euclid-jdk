package de.jensvogt.euclid.module.eds;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jensvogt.euclid.auth.CredentialsFileTokens;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.auth.SigningScheme;
import de.jensvogt.euclid.auth.SigningSchemeSelectable;
import de.jensvogt.euclid.auth.TokenRefreshable;
import de.jensvogt.euclid.dto.eds.CreateZoneRequest;
import de.jensvogt.euclid.dto.eds.GetAnswerRequest;
import de.jensvogt.euclid.dto.eds.ListRecordsRequest;
import de.jensvogt.euclid.dto.eds.ListRecordsResponse;
import de.jensvogt.euclid.dto.eds.ListZonesRequest;
import de.jensvogt.euclid.dto.eds.ListZonesResponse;
import de.jensvogt.euclid.dto.eds.PutRecordRequest;
import de.jensvogt.euclid.dto.eds.RecordRequest;
import de.jensvogt.euclid.dto.eds.ZoneRequest;
import de.jensvogt.euclid.dto.eds.model.Answer;
import de.jensvogt.euclid.dto.eds.model.AnswerRecord;
import de.jensvogt.euclid.dto.eds.model.HostedZone;
import de.jensvogt.euclid.dto.eds.model.RecordSet;
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
 * EDS (domain name service) operations for an authenticated
 * {@link de.jensvogt.euclid.module.eam.EuclidSession}: the zones the name server answers for
 * authoritatively, the record sets in them, and what it would answer for a question.
 * <p>
 * EDS has two ports. This client manages zones and records through euclid's gateway, with the
 * session's credentials and grants; DNS queries themselves are served on UDP and TCP (port 1053 by
 * default) to anybody who can reach it, which this client does not talk to -
 * {@link #getAnswer(String, String)} shows the same answer without needing a DNS client.
 * <p>
 * A zone's name is unique across the installation, not just the account, because a DNS question
 * names no account. Asking for another account's zone is answered with HTTP 404, the same as one
 * that does not exist. Each action needs the matching {@code eds:*} permission on the zone's ERN
 * ({@code ern:eds:<region>:<account>:<namespace>:zone:<name>}); a caller without it gets HTTP 403.
 */
public final class EuclidEds implements TokenRefreshable, SigningSchemeSelectable {

    /**
     * A singleton instance of {@code ObjectMapper} from the Jackson library used for
     * serializing Java objects to JSON and deserializing JSON to Java objects.
     * <p>
     * Configured to leave null fields out of the serialized request entirely rather than writing
     * them as {@code null}. EDS reads an absent filter as "no filter", so an unset prefix, name or
     * type must not reach it at all.
     */
    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);

    /**
     * The Euclid service every request from this class is addressed to, sent as the
     * {@code x-euclid-target} header.
     */
    private static final String TARGET = "eds";

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
     * The namespace requests are scoped to, sent as the {@code x-euclid-namespace} header. A zone
     * created here is owned by this namespace, and {@code list-zones} lists this namespace's zones.
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
     * Constructs an EDS client. Normally obtained from
     * {@link de.jensvogt.euclid.module.eam.EuclidSession#eds()} rather than built directly.
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
    public EuclidEds(String baseUrl, String token, String region, String accountId, String userId,
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

    // ── Zones ────────────────────────────────────────────────────────────────

    /**
     * Creates a zone with the default TTL and no description.
     *
     * @param name the zone's apex, e.g. {@code "corp.example"}
     * @return the zone as stored
     * @throws EuclidServiceException with HTTP 400 if the name is not a valid zone name or lies in
     *                                the service domain, 401 if the session is not authenticated,
     *                                403 if the caller lacks {@code eds:create-zone}, or 409 if a zone
     *                                of that name exists already - in any account
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public HostedZone createZone(String name) throws IOException, InterruptedException {
        return createZone(CreateZoneRequest.builder().name(name).build());
    }

    /**
     * Creates a zone, which the name server answers for from then on: with the records put into
     * it, and with NXDOMAIN for every other name below it. The zone is owned by the session's
     * account and namespace, and gets an SOA and NS record without being asked.
     *
     * @param request the zone's apex, TTL and description
     * @return the zone as stored
     * @throws EuclidServiceException with HTTP 400 if the name is not a valid zone name, lies in the
     *                                service domain or the TTL is out of range, 401 if the session is
     *                                not authenticated, 403 if the caller lacks
     *                                {@code eds:create-zone}, or 409 if a zone of that name exists
     *                                already - in any account
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public HostedZone createZone(CreateZoneRequest request) throws IOException, InterruptedException {
        return toZone(post("create-zone", OBJECT_MAPPER.writeValueAsString(request)));
    }

    /**
     * Retrieves a zone: its TTL, its SOA serial and how many record sets it holds.
     *
     * @param name the zone's apex
     * @return the zone
     * @throws EuclidServiceException with HTTP 401 if the session is not authenticated, 403 if the
     *                                caller lacks {@code eds:get-zone}, or 404 if no zone of that name
     *                                belongs to the account
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public HostedZone getZone(String name) throws IOException, InterruptedException {
        return toZone(post("get-zone", OBJECT_MAPPER.writeValueAsString(new ZoneRequest(name))));
    }

    /**
     * Lists every zone of the session's account and namespace.
     *
     * @return the zones and how many there are
     * @throws EuclidServiceException with HTTP 401 if the session is not authenticated, or 403 if
     *                                the caller lacks {@code eds:list-zones}
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ListZonesResponse listZones() throws IOException, InterruptedException {
        return listZones(ListZonesRequest.builder().build());
    }

    /**
     * Lists a page of the zones of the session's account and namespace, optionally only those whose
     * name starts with a prefix. A zone the caller's grants do not let it read is left off the page.
     *
     * @param request the prefix and the page to return
     * @return the zones and how many match
     * @throws EuclidServiceException with HTTP 401 if the session is not authenticated, or 403 if
     *                                the caller lacks {@code eds:list-zones}
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ListZonesResponse listZones(ListZonesRequest request) throws IOException, InterruptedException {
        JsonNode root = post("list-zones", OBJECT_MAPPER.writeValueAsString(request));
        List<HostedZone> zones = new ArrayList<>();
        JsonNode array = root.get("zones");
        if (array != null && array.isArray()) {
            for (JsonNode zone : array) {
                zones.add(toZone(zone));
            }
        }
        return new ListZonesResponse(zones, root.path("total").asLong(0));
    }

    /**
     * Deletes a zone and every record in it. Names in it stop resolving at once; resolvers that
     * cached them keep them for up to their TTL.
     *
     * @param name the zone's apex
     * @return how many record sets were deleted with it
     * @throws EuclidServiceException with HTTP 401 if the session is not authenticated, 403 if the
     *                                caller lacks {@code eds:delete-zone}, or 404 if no zone of that
     *                                name belongs to the account
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public long deleteZone(String name) throws IOException, InterruptedException {
        return post("delete-zone", OBJECT_MAPPER.writeValueAsString(new ZoneRequest(name)))
                .path("deletedRecords").asLong(0);
    }

    // ── Records ──────────────────────────────────────────────────────────────

    /**
     * Writes every record of one type at one name with the zone's TTL, replacing those there.
     *
     * @param zone   the zone's apex
     * @param name   the owner name - see {@link PutRecordRequest#name()}
     * @param type   the type mnemonic: A, AAAA, CNAME, MX, NS, PTR, SRV or TXT
     * @param values the values, in zone-file presentation form
     * @return the record set as stored
     * @throws EuclidServiceException as {@link #putRecord(PutRecordRequest)} does
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public RecordSet putRecord(String zone, String name, String type, String... values)
            throws IOException, InterruptedException {
        return putRecord(PutRecordRequest.builder().zone(zone).name(name).type(type).values(values).build());
    }

    /**
     * Writes every record of one type at one name, replacing those there in one step - there is
     * never a moment when half the old values and half the new ones are served.
     *
     * @param request the zone, owner name, type, TTL and values
     * @return the record set as stored
     * @throws EuclidServiceException with HTTP 400 if the name is not in the zone, the type is not
     *                                storable, a value cannot be encoded, there are no values, a
     *                                CNAME has more than one value or is put at the apex; 401 if the
     *                                session is not authenticated; 403 if the caller lacks
     *                                {@code eds:put-record}; 404 if the zone does not belong to the
     *                                account; or 409 if a CNAME would sit beside other records
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public RecordSet putRecord(PutRecordRequest request) throws IOException, InterruptedException {
        return toRecordSet(post("put-record", OBJECT_MAPPER.writeValueAsString(request)));
    }

    /**
     * Removes every record of one type at one name.
     *
     * @param zone the zone's apex
     * @param name the owner name - see {@link PutRecordRequest#name()}; {@code null} or {@code "@"} is the apex
     * @param type the type mnemonic
     * @throws EuclidServiceException with HTTP 400 if the name is not in the zone or no type is
     *                                given, 401 if the session is not authenticated, 403 if the caller
     *                                lacks {@code eds:delete-record}, or 404 if the zone does not
     *                                belong to the account or holds no such records
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public void deleteRecord(String zone, String name, String type) throws IOException, InterruptedException {
        post("delete-record", OBJECT_MAPPER.writeValueAsString(new RecordRequest(zone, name == null ? "@" : name, type)));
    }

    /**
     * Lists every record set of a zone.
     *
     * @param zone the zone's apex
     * @return the record sets and how many there are
     * @throws EuclidServiceException as {@link #listRecords(ListRecordsRequest)} does
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ListRecordsResponse listRecords(String zone) throws IOException, InterruptedException {
        return listRecords(ListRecordsRequest.builder().zone(zone).build());
    }

    /**
     * Lists a page of a zone's record sets, by name and type, with their values as they were put.
     * The SOA and NS records the server makes for every zone are not stored, so they are not listed.
     *
     * @param request the zone, the optional name and type filters, and the page to return
     * @return the record sets and how many match
     * @throws EuclidServiceException with HTTP 400 if the name is not in the zone or the type is
     *                                unknown, 401 if the session is not authenticated, 403 if the
     *                                caller lacks {@code eds:list-records}, or 404 if the zone does
     *                                not belong to the account
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ListRecordsResponse listRecords(ListRecordsRequest request) throws IOException, InterruptedException {
        JsonNode root = post("list-records", OBJECT_MAPPER.writeValueAsString(request));
        List<RecordSet> records = new ArrayList<>();
        JsonNode array = root.get("records");
        if (array != null && array.isArray()) {
            for (JsonNode record : array) {
                records.add(toRecordSet(record));
            }
        }
        return new ListRecordsResponse(records, root.path("total").asLong(0));
    }

    // ── Lookups ──────────────────────────────────────────────────────────────

    /**
     * Shows what the name server answers for the A records of a name.
     *
     * @param name the name to look up
     * @return the answer
     * @throws EuclidServiceException as {@link #getAnswer(String, String)} does
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public Answer getAnswer(String name) throws IOException, InterruptedException {
        return getAnswer(name, "A");
    }

    /**
     * Shows what the name server answers for a name, from the zones it holds - the same answer a DNS
     * client gets, without needing one. A name in none of its zones comes back {@code REFUSED}; the
     * DNS port forwards those instead, when forwarders are configured. Not checked against the
     * caller's grants on any zone, since anybody who can reach the DNS port gets the same answer.
     *
     * @param name the name to look up
     * @param type the type mnemonic; {@code null} means {@code A}
     * @return the answer
     * @throws EuclidServiceException with HTTP 400 if no name is given or the type is unknown, 401 if
     *                                the session is not authenticated, or 403 if the caller lacks
     *                                {@code eds:get-answer}
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public Answer getAnswer(String name, String type) throws IOException, InterruptedException {
        JsonNode root = post("get-answer", OBJECT_MAPPER.writeValueAsString(new GetAnswerRequest(name, type)));
        String zone = textOrNull(root, "zone");
        return new Answer(
                textOrNull(root, "name"),
                textOrNull(root, "type"),
                root.path("authoritative").asBoolean(false),
                zone == null || zone.isEmpty() ? null : zone,
                textOrNull(root, "rcode"),
                toAnswerRecords(root.get("answers")),
                toAnswerRecords(root.get("authority")));
    }

    /**
     * Posts one of EDS's actions and parses the response body, since every one of them takes a JSON
     * request and answers with JSON.
     *
     * @param action the EDS action to post
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
     * Builds a {@link HostedZone} from a zone as EDS describes it.
     *
     * @param node the JSON object describing the zone
     * @return the parsed zone
     */
    private static HostedZone toZone(JsonNode node) {
        return new HostedZone(
                textOrNull(node, "name"),
                textOrNull(node, "ern"),
                textOrNull(node, "namespace"),
                node.path("ttl").asLong(0),
                textOrNull(node, "description"),
                node.path("recordCount").asLong(0),
                node.path("serial").asLong(0),
                textOrNull(node, "created"),
                textOrNull(node, "modified"));
    }

    /**
     * Builds a {@link RecordSet} from a record set as EDS describes it.
     *
     * @param node the JSON object describing the record set
     * @return the parsed record set
     */
    private static RecordSet toRecordSet(JsonNode node) {
        return new RecordSet(
                textOrNull(node, "zone"),
                textOrNull(node, "name"),
                textOrNull(node, "type"),
                node.path("ttl").asLong(0),
                textList(node, "values"),
                textOrNull(node, "created"),
                textOrNull(node, "modified"));
    }

    /**
     * Converts one section of a get-answer response into {@link AnswerRecord}s.
     *
     * @param array the JSON array holding the section
     * @return the records, or an empty list if the section is absent
     */
    private static List<AnswerRecord> toAnswerRecords(JsonNode array) {
        List<AnswerRecord> records = new ArrayList<>();
        if (array != null && array.isArray()) {
            for (JsonNode node : array) {
                records.add(new AnswerRecord(textOrNull(node, "name"), textOrNull(node, "type"),
                        node.path("ttl").asLong(0), textOrNull(node, "value")));
            }
        }
        return records;
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
     * Reads an array of strings from a JSON object.
     *
     * @param node the JSON object to read from
     * @param field the field name
     * @return the strings, or an empty list if the field is absent or not an array
     */
    private static List<String> textList(JsonNode node, String field) {
        List<String> values = new ArrayList<>();
        JsonNode array = node.get(field);
        if (array != null && array.isArray()) {
            for (JsonNode value : array) {
                values.add(value.asText());
            }
        }
        return values;
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
