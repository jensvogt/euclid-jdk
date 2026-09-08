package de.jensvogt.euclid.module.ess;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jensvogt.euclid.auth.CredentialsFileTokens;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.auth.SigningScheme;
import de.jensvogt.euclid.auth.SigningSchemeSelectable;
import de.jensvogt.euclid.auth.TokenRefreshable;
import de.jensvogt.euclid.dto.ess.AddSecretTagRequest;
import de.jensvogt.euclid.dto.ess.CreateSecretRequest;
import de.jensvogt.euclid.dto.ess.DeleteSecretResponse;
import de.jensvogt.euclid.dto.ess.DeleteSecretTagRequest;
import de.jensvogt.euclid.dto.ess.GetSecretResponse;
import de.jensvogt.euclid.dto.ess.ListSecretsRequest;
import de.jensvogt.euclid.dto.ess.ListSecretsResponse;
import de.jensvogt.euclid.dto.ess.SecretNameRequest;
import de.jensvogt.euclid.dto.ess.UpdateSecretRequest;
import de.jensvogt.euclid.dto.ess.model.Secret;
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
 * ESS (secret store) operations for an authenticated
 * {@link de.jensvogt.euclid.module.eam.EuclidSession}: named secrets, stored encrypted under an EKM
 * key.
 * <p>
 * A secret is addressed by name throughout - not by ERN, and not by ID. The value itself only ever
 * travels twice: in on {@link #createSecret} or {@link #updateSecret}, and out on
 * {@link #getSecret}. Every other action here works on ciphertext the server never looks inside, so
 * a listing or a tag change reports what happened without the secret coming with it.
 * <p>
 * Which key protects a secret is a choice: name one on creation, or let the namespace's own key be
 * used. Either way {@link Secret#encryptionKeyErn()} says which, and moving a secret to another key
 * with {@link #updateSecret} re-encrypts the stored value without counting as a rotation - that
 * changes how the secret is protected, not what it is.
 */
public final class EuclidEss implements TokenRefreshable, SigningSchemeSelectable {

    /**
     * A singleton instance of {@code ObjectMapper} from the Jackson library used for
     * serializing Java objects to JSON and deserializing JSON to Java objects.
     * <p>
     * Configured to leave null fields out of the serialized request entirely rather than writing
     * them as {@code null}. ESS distinguishes an absent field from a present one: update-secret
     * only touches the fields it receives, and the difference between omitting a description and
     * sending an empty one is the difference between leaving it alone and clearing it.
     */
    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    /**
     * The Euclid service every request from this class is addressed to, sent as the
     * {@code x-euclid-target} header.
     */
    private static final String TARGET = "ess";

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
     * The namespace requests are scoped to, sent as the {@code x-euclid-namespace} header. Secrets
     * are looked up per account and namespace, so this decides which secret a name resolves to -
     * and, for a secret created without a key of its own, which namespace key protects it.
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
     * Constructs an ESS client. Normally obtained from
     * {@link de.jensvogt.euclid.module.eam.EuclidSession#ess()} rather than built directly.
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
    public EuclidEss(String baseUrl, String token, String region, String accountId, String userId,
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
     * Stores a new secret under the namespace's own encryption key.
     *
     * @param name  the name to store the secret under
     * @param value the secret itself, which must not be empty
     * @return the stored secret's metadata, without the value
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public Secret createSecret(String name, String value) throws IOException, InterruptedException {
        return createSecret(CreateSecretRequest.builder().name(name).value(value).build());
    }

    /**
     * Stores a new secret.
     * <p>
     * Refused with HTTP 409 if a secret of that name already exists: create-secret quietly replacing
     * a value would be a way to destroy one by accident, and {@link #updateSecret} is the action
     * that says it means to. An empty value is refused too - it is almost always a caller whose
     * shell ate the value, and finding that out now beats finding it out when something reads it.
     *
     * @param request the secret to store, optionally naming the EKM key to encrypt it under
     * @return the stored secret's metadata, without the value
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public Secret createSecret(CreateSecretRequest request) throws IOException, InterruptedException {
        return toSecret(post("create-secret", OBJECT_MAPPER.writeValueAsString(request)).get("secret"));
    }

    /**
     * Reads a secret's value.
     * <p>
     * The one action in this module that decrypts anything. Treat the result accordingly: do not log
     * it, and do not hold it longer than the call that needed it.
     *
     * @param name the name of the secret to read
     * @return the secret's metadata together with its decrypted value
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public GetSecretResponse getSecret(String name) throws IOException, InterruptedException {
        JsonNode root = post("get-secret", secretNameBody(name));
        return GetSecretResponse.builder().secret(toSecret(root.get("secret")))
                .value(textOrNull(root, "value")).build();
    }

    /**
     * Lists the secrets of this session's account and namespace, using default paging.
     *
     * @return a {@code ListSecretsResponse} carrying the secrets and their total
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ListSecretsResponse listSecrets() throws IOException, InterruptedException {
        return listSecrets("", 10, 0, "name", "asc");
    }

    /**
     * Lists secrets, optionally filtered by name prefix and paginated. No values come back - see
     * {@link #getSecret} for that.
     * <p>
     * A principal restricted to particular secrets sees those and no others. The list is filtered
     * rather than refused, so an application listing what it may read gets an answer instead of a
     * 403 - which does mean {@link ListSecretsResponse#total()} can exceed the number of secrets
     * actually returned.
     *
     * @param prefix only secrets whose name starts with this prefix are returned
     * @param pageSize the maximum number of secrets to return in a single page
     * @param pageIndex the zero-based index of the page to return
     * @param sortColumn the column results are sorted by
     * @param sortDirection the direction to sort in, {@code "asc"} or {@code "desc"}
     * @return a {@code ListSecretsResponse} carrying the secrets and their total
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ListSecretsResponse listSecrets(String prefix, long pageSize, long pageIndex, String sortColumn,
                                           String sortDirection) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                ListSecretsRequest.builder().prefix(prefix).pageSize(pageSize).pageIndex(pageIndex)
                        .sortColumn(sortColumn).sortDirection(sortDirection).build());
        JsonNode root = post("list-secrets", body);
        return ListSecretsResponse.builder().secrets(toSecretList(root.get("secrets")))
                .total(root.path("total").asLong(0)).build();
    }

    /**
     * Replaces a secret's value, which is what counts as a rotation and what bumps
     * {@link Secret#version()}.
     *
     * @param name  the name of the secret to rotate
     * @param value the new value, which must not be empty
     * @return the secret's metadata afterwards, without the value
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public Secret rotateSecret(String name, String value) throws IOException, InterruptedException {
        return updateSecret(UpdateSecretRequest.builder().name(name).value(value).build());
    }

    /**
     * Changes a secret that already exists: a rotation, a change of description, a move to another
     * key, or any combination. Only what the request names changes.
     * <p>
     * The distinction the server draws is between a field being absent and being present, not
     * between values - so leaving {@link UpdateSecretRequest#description()} null leaves the stored
     * description alone, while sending {@code ""} clears it. A request naming none of the three is
     * refused with HTTP 400 rather than treated as a no-op.
     *
     * @param request what to change about the secret
     * @return the secret's metadata afterwards, without the value
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public Secret updateSecret(UpdateSecretRequest request) throws IOException, InterruptedException {
        return toSecret(post("update-secret", OBJECT_MAPPER.writeValueAsString(request)).get("secret"));
    }

    /**
     * Deletes a secret.
     *
     * @param name the name of the secret to delete
     * @return the ERN and name of the deleted secret
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public DeleteSecretResponse deleteSecret(String name) throws IOException, InterruptedException {
        JsonNode root = post("delete-secret", secretNameBody(name));
        return DeleteSecretResponse.builder().ern(textOrNull(root, "ern")).name(textOrNull(root, "name")).build();
    }

    /**
     * Sets a tag on a secret, overwriting a tag of that key if one is already there.
     *
     * @param name  the name of the secret to tag
     * @param key   the tag key
     * @param value the tag value
     * @return the secret's metadata afterwards, with the tag on it
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public Secret addSecretTag(String name, String key, String value) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                AddSecretTagRequest.builder().name(name).key(key).value(value).build());
        return toSecret(post("add-secret-tag", body).get("secret"));
    }

    /**
     * Removes a tag from a secret.
     *
     * @param name the name of the secret the tag belongs to
     * @param key  the tag key to remove
     * @return the secret's metadata afterwards, without the tag
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public Secret deleteSecretTag(String name, String key) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                DeleteSecretTagRequest.builder().name(name).key(key).build());
        return toSecret(post("delete-secret-tag", body).get("secret"));
    }

    /**
     * Builds the request body for the two actions that name nothing but a secret.
     *
     * @param name the name of the secret the action applies to
     * @return the serialized request body
     * @throws IOException if the request cannot be serialized
     */
    private static String secretNameBody(String name) throws IOException {
        return OBJECT_MAPPER.writeValueAsString(SecretNameRequest.builder().name(name).build());
    }

    /**
     * Posts one of ESS's actions and parses the response body, since every one of them takes a JSON
     * request and answers with JSON.
     *
     * @param action the ESS action to post
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
     * Converts a JsonNode holding an array of secrets into a list of Secret instances.
     *
     * @param secretsNode the JsonNode representing the array of secrets
     * @return a list of Secret instances, or an empty list if the node is null or not an array
     */
    private static List<Secret> toSecretList(JsonNode secretsNode) {
        List<Secret> secrets = new ArrayList<>();
        if (secretsNode != null && secretsNode.isArray()) {
            for (JsonNode secretNode : secretsNode) {
                secrets.add(toSecret(secretNode));
            }
        }
        return secrets;
    }

    /**
     * Builds a {@link Secret} from the secret JSON every ESS action answers with.
     *
     * @param node the JSON object describing the secret
     * @return the parsed secret, or {@code null} if the node is null
     */
    private static Secret toSecret(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new Secret(
                textOrNull(node, "name"),
                textOrNull(node, "ern"),
                textOrNull(node, "description"),
                textOrNull(node, "encryptionKeyErn"),
                node.path("version").asLong(0),
                textOrNull(node, "rotated"),
                toStringMap(node.get("tags")),
                textOrNull(node, "created"),
                textOrNull(node, "modified"));
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
