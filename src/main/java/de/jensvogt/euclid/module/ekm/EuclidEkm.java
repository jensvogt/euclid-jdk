package de.jensvogt.euclid.module.ekm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.auth.SigningScheme;
import de.jensvogt.euclid.auth.SigningSchemeSelectable;
import de.jensvogt.euclid.auth.TokenRefreshable;
import de.jensvogt.euclid.dto.ekm.AddKeyTagRequest;
import de.jensvogt.euclid.dto.ekm.CreateKeyRequest;
import de.jensvogt.euclid.dto.ekm.CreateKeyResponse;
import de.jensvogt.euclid.dto.ekm.DeleteKeyRequest;
import de.jensvogt.euclid.dto.ekm.DeleteKeyResponse;
import de.jensvogt.euclid.dto.ekm.DeleteKeyTagRequest;
import de.jensvogt.euclid.dto.ekm.ListKeysRequest;
import de.jensvogt.euclid.dto.ekm.ListKeysResponse;
import de.jensvogt.euclid.dto.ekm.RevokeKeyRequest;
import de.jensvogt.euclid.dto.ekm.RevokeKeyResponse;
import de.jensvogt.euclid.dto.ekm.model.Key;
import de.jensvogt.euclid.exception.EuclidServiceException;
import de.jensvogt.euclid.http.EuclidHttpClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * EKM (key management) operations for an authenticated {@link de.jensvogt.euclid.module.eam.EuclidSession}.
 * Mirrors euclid-cli's {@code EkmCli}.
 * <p>
 * A key is addressed two different ways depending on the action, which is worth knowing before
 * reaching for the wrong one: {@link #encrypt}, {@link #decrypt} and {@link #deleteKey} take the key
 * ID ({@link Key#name()}, a server-generated UUID), while {@link #revokeKey}, {@link #addKeyTag} and
 * {@link #deleteKeyTag} take the key's ERN. Both come back from {@link #createKey}.
 * <p>
 * Key material never leaves the server. There is no export action - encryption and decryption are
 * round trips, with the plaintext or ciphertext travelling as the raw request and response body.
 */
public final class EuclidEkm implements TokenRefreshable, SigningSchemeSelectable {

    /**
     * A singleton instance of {@code ObjectMapper} from the Jackson library used for
     * serializing Java objects to JSON and deserializing JSON to Java objects.
     * <p>
     * This instance is thread-safe and can be reused throughout the application
     * to avoid the overhead of creating multiple instances.
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * The Euclid service every request from this class is addressed to, sent as the
     * {@code x-euclid-target} header.
     */
    private static final String TARGET = "ekm";

    /**
     * The base URL of the Euclid server this instance talks to.
     */
    private final String baseUrl;

    /**
     * Supplies the bearer token for each request, used when no SigV4 access key is configured.
     *
     * <p>A supplier rather than a string so that a token which expires can be replaced without
     * rebuilding the client - see {@link TokenRefreshable#token(Supplier)}. A client built with a
     * fixed token holds a supplier that returns it.
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
     * The namespace requests are scoped to, sent as the {@code x-euclid-namespace} header. Keys are
     * looked up per account and namespace, so this decides which keys an ID resolves against.
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
     * Constructs an EKM client. Normally obtained from
     * {@link de.jensvogt.euclid.module.eam.EuclidSession#ekm()} rather than built directly.
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
    public EuclidEkm(String baseUrl, String token, String region, String accountId, String userId,
                     String accessKeyId, String secretAccessKey, String caCertPath, String nameSpace) {
        this.baseUrl = baseUrl;
        this.token = () -> token;
        this.region = region;
        this.accountId = accountId;
        this.userId = userId;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.nameSpace = nameSpace;
        this.httpClient = new EuclidHttpClient(caCertPath);
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
     * Creates a new 128-bit AES key.
     *
     * @return a {@code CreateKeyResponse} carrying the new key's ID and ERN
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public CreateKeyResponse createKey() throws IOException, InterruptedException {
        return createKey("AES", 128);
    }

    /**
     * Creates a new encryption key. The server mints the key ID rather than taking one from the
     * caller, so the returned {@link CreateKeyResponse#name()} is the only handle to the new key.
     *
     * @param algorithm the key algorithm; the server only generates {@code "AES"} keys so far and
     *                  rejects anything else with HTTP 400
     * @param length the key length in bits, 128 or 256
     * @return a {@code CreateKeyResponse} carrying the new key's ID and ERN
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public CreateKeyResponse createKey(String algorithm, long length) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                CreateKeyRequest.builder().algorithm(algorithm).length(length).build());
        JsonNode root = post("create-key", body);
        return CreateKeyResponse.builder().ern(textOrNull(root, "ern")).name(textOrNull(root, "name"))
                .algorithm(textOrNull(root, "algorithm")).length(root.path("length").asLong(0))
                .status(textOrNull(root, "status")).build();
    }

    /**
     * Lists the keys of this session's account and namespace, using default paging.
     *
     * @return a {@code ListKeysResponse} carrying the keys and their total
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ListKeysResponse listKeys() throws IOException, InterruptedException {
        return listKeys("", 10, 0, "name", "asc");
    }

    /**
     * Lists keys, optionally filtered by name prefix and paginated.
     *
     * @param prefix only keys whose name starts with this prefix are returned
     * @param pageSize the maximum number of keys to return in a single page
     * @param pageIndex the zero-based index of the page to return
     * @param sortColumn the column results are sorted by
     * @param sortDirection the direction to sort in, {@code "asc"} or {@code "desc"}
     * @return a {@code ListKeysResponse} carrying the keys and their total
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ListKeysResponse listKeys(String prefix, long pageSize, long pageIndex, String sortColumn,
                                     String sortDirection) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                ListKeysRequest.builder().prefix(prefix).pageSize(pageSize).pageIndex(pageIndex)
                        .sortColumn(sortColumn).sortDirection(sortDirection).build());
        JsonNode root = post("list-keys", body);
        return ListKeysResponse.builder().keys(toKeyList(root.get("keys")))
                .total(root.path("total").asLong(0)).build();
    }

    /**
     * Schedules a key for permanent deletion after the default seven-day grace period.
     *
     * @param keyId the ID of the key to delete, as returned by {@link #createKey}
     * @return a {@code DeleteKeyResponse} carrying the date the key becomes unrecoverable
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public DeleteKeyResponse deleteKey(String keyId) throws IOException, InterruptedException {
        return deleteKey(keyId, 7);
    }

    /**
     * Schedules a key for permanent deletion rather than removing it outright. Decryption keeps
     * working for the whole grace period, which is what gives callers a chance to migrate whatever
     * was encrypted under the key before it becomes unrecoverable. Encryption, however, is blocked
     * immediately - a key on its way out should not be gaining new data to lose.
     *
     * @param keyId the ID of the key to delete, as returned by {@link #createKey}
     * @param pendingWindowInDays days to wait before the key is permanently removed; the server
     *                            rejects anything below 1 with HTTP 400
     * @return a {@code DeleteKeyResponse} carrying the date the key becomes unrecoverable
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public DeleteKeyResponse deleteKey(String keyId, long pendingWindowInDays)
            throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                DeleteKeyRequest.builder().keyId(keyId).pendingWindowInDays(pendingWindowInDays).build());
        JsonNode root = post("delete-key", body);
        return DeleteKeyResponse.builder().ern(textOrNull(root, "ern")).name(textOrNull(root, "name"))
                .deletionDate(textOrNull(root, "deletionDate")).status(textOrNull(root, "status")).build();
    }

    /**
     * Revokes a key: it can no longer encrypt, but decryption of data already encrypted under it
     * keeps working indefinitely. Unlike {@link #deleteKey}, this schedules nothing for removal.
     * <p>
     * Takes the key's ERN, not its ID. The server refuses to revoke a key already scheduled for
     * deletion, since encryption is blocked there already and downgrading the status to REVOKED
     * would hide the pending deletion.
     *
     * @param ern the ERN of the key to revoke
     * @return a {@code RevokeKeyResponse} carrying the key's new status
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public RevokeKeyResponse revokeKey(String ern) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(RevokeKeyRequest.builder().ern(ern).build());
        JsonNode root = post("revoke-key", body);
        return RevokeKeyResponse.builder().ern(textOrNull(root, "ern")).name(textOrNull(root, "name"))
                .status(textOrNull(root, "status")).build();
    }

    /**
     * Encrypts data with a key held by the server, which returns the ciphertext as
     * {@code IV || ciphertext || tag} - the exact bytes {@link #decrypt} takes back.
     * <p>
     * Only a key with status AVAILABLE can encrypt; a revoked key or one scheduled for deletion is
     * refused with HTTP 403.
     *
     * @param keyId the ID of the key to encrypt with, as returned by {@link #createKey}
     * @param plaintext the bytes to encrypt
     * @return the ciphertext
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public byte[] encrypt(String keyId, byte[] plaintext) throws IOException, InterruptedException {
        return transform("encrypt", keyId, plaintext);
    }

    /**
     * Decrypts data previously produced by {@link #encrypt}. Works for a revoked key and for one
     * scheduled for deletion, right up until its deletion date passes.
     *
     * @param keyId the ID of the key the data was encrypted with
     * @param ciphertext the bytes to decrypt, as {@link #encrypt} returned them
     * @return the recovered plaintext
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public byte[] decrypt(String keyId, byte[] ciphertext) throws IOException, InterruptedException {
        return transform("decrypt", keyId, ciphertext);
    }

    /**
     * Adds a tag to a key. The tag is upserted, so a key already carrying this tag has its value
     * replaced - EKM has no separate set-key-tag action to distinguish the two.
     *
     * @param ern the ERN of the key to tag
     * @param key the tag key
     * @param value the tag value
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public void addKeyTag(String ern, String key, String value) throws IOException, InterruptedException {
        post("add-key-tag", OBJECT_MAPPER.writeValueAsString(
                AddKeyTagRequest.builder().ern(ern).key(key).value(value).build()));
    }

    /**
     * Deletes a tag from a key.
     *
     * @param ern the ERN of the key the tag belongs to
     * @param key the tag key to delete
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public void deleteKeyTag(String ern, String key) throws IOException, InterruptedException {
        post("delete-key-tag", OBJECT_MAPPER.writeValueAsString(
                DeleteKeyTagRequest.builder().ern(ern).key(key).build()));
    }

    /**
     * Posts one of EKM's JSON actions and parses the response body, since every one of them takes a
     * JSON request and answers with JSON.
     *
     * @param action the EKM action to post
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
     * Sends an encrypt or decrypt action, which differ only in direction: both take opaque bytes as
     * the request body, name their key in the {@code x-euclid-key-id} header, and answer with
     * opaque bytes.
     *
     * @param action the action to send, {@code "encrypt"} or {@code "decrypt"}
     * @param keyId the ID of the key the transform uses
     * @param data the bytes to transform
     * @return the transformed bytes
     * @throws IOException if an I/O error occurs during the request
     * @throws InterruptedException if the operation is interrupted while waiting for the response
     */
    private byte[] transform(String action, String keyId, byte[] data) throws IOException, InterruptedException {
        Map<String, String> headers = binaryRequestHeaders();
        headers.put("Content-Type", "application/octet-stream");
        headers.put("x-euclid-key-id", keyId);
        HttpResponse<byte[]> response = httpClient.postBinaryForBinary(baseUrl + "/", data, TARGET, action, headers);

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException(TARGET, action, response.statusCode(),
                    new String(response.body(), StandardCharsets.UTF_8));
        }

        return response.body();
    }

    /**
     * Converts a JsonNode holding an array of keys into a list of Key instances.
     *
     * @param keysNode the JsonNode representing the array of keys
     * @return a list of Key instances, or an empty list if the node is null or not an array
     */
    private static List<Key> toKeyList(JsonNode keysNode) {
        List<Key> keys = new ArrayList<>();
        if (keysNode != null && keysNode.isArray()) {
            for (JsonNode keyNode : keysNode) {
                keys.add(new Key(
                        textOrNull(keyNode, "name"),
                        textOrNull(keyNode, "ern"),
                        textOrNull(keyNode, "algorithm"),
                        keyNode.path("length").asLong(0),
                        textOrNull(keyNode, "status"),
                        toStringMap(keyNode.get("tags")),
                        // Only present on a key scheduled for deletion - the server leaves the
                        // field out entirely otherwise rather than sending an empty value.
                        textOrNull(keyNode, "deletionDate"),
                        textOrNull(keyNode, "created"),
                        textOrNull(keyNode, "modified")));
            }
        }
        return keys;
    }

    /**
     * Converts a JsonNode holding a JSON object of strings into a string-to-string map.
     *
     * @param node the JsonNode to convert
     * @return a map of the node's fields, or an empty map if the node is null or not an object
     */
    private static Map<String, String> toStringMap(JsonNode node) {
        Map<String, String> map = new LinkedHashMap<>();
        if (node != null && node.isObject()) {
            node.fields().forEachRemaining(entry -> map.put(entry.getKey(), entry.getValue().asText()));
        }
        return map;
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
     * Builds the headers for encrypt and decrypt, the two actions whose body is raw bytes rather
     * than JSON.
     * <p>
     * These always authenticate with the bearer token, never SigV4, even when access keys are
     * configured - the same deviation ESM's payload-carrying actions make, and for the same reason:
     * SigV4.sign() hashes the body as a UTF-8 String, which is lossy for arbitrary binary bytes.
     * Ciphertext in particular is uniformly random and would almost never survive that round trip.
     *
     * @return a mutable header map the caller adds its action-specific headers to
     */
    private Map<String, String> binaryRequestHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + token.get());
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
