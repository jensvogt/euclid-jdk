package de.jensvogt.euclid.module.eam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jensvogt.euclid.dto.Metadata;
import de.jensvogt.euclid.dto.eam.LoginRequest;
import de.jensvogt.euclid.dto.eam.LoginResponse;
import de.jensvogt.euclid.exception.EuclidAuthenticationException;
import de.jensvogt.euclid.http.EuclidHttpClient;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Fluent builder for authenticating against an Euclid server.
 *
 * <pre>{@code
 * EuclidSession session = EuclidAccess.forServer("https://euclid.example.com")
 *         .credentials("jens", "secret")
 *         .login();
 * }</pre>
 */
public final class EuclidEam {

    /**
     * A static, thread-safe instance of {@code ObjectMapper} used for JSON processing.
     * This class provides functionality to serialize Java objects to JSON and
     * deserialize JSON to Java objects. It is configured to be reused across
     * the application wherever JSON parsing or generation is required.
     * <p>
     * The {@code ObjectMapper} is part of the Jackson library, commonly used
     * for working with JSON data in Java applications.
     * <p>
     * As a static and final field, this instance is initialized once and
     * remains immutable, ensuring consistent and efficient JSON handling
     * throughout the lifetime of the application.
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * The file system path to the credentials file used for storing authentication details:
     * {@code $HOME/.euclid/credentials}, the same file euclid-cli reads and writes, so a login from
     * either client is picked up by the other.
     * <p>
     * Resolved per call rather than captured once at class load, mirroring euclid-cli's
     * {@code Credentials::FilePath()} - the home directory is read when the file is actually
     * touched, so a process that changes it is not left talking to a stale path.
     *
     * @return the path to the credentials file
     */
    private static Path credentialsPath() {
        return Path.of(System.getProperty("user.home"), ".euclid", "credentials");
    }

    /**
     * Constant representing the default file path to the certificate authority (CA) certificate
     * used for server authentication in Euclid deployments. This path aligns with the default
     * value used by the Euclid command-line interface (`euclid-cli`).
     * <p>
     * The certificate at this path is only applied if the file exists. If the file is not found,
     * the system trust store is used instead, effectively making this configuration a no-op on
     * machines without an Euclid deployment.
     * <p>
     * Users can override this value by explicitly setting a different certificate path via the
     * {@code caCertPath(String caCertPath)} method or opt out entirely.
     */
    private static final Path DEFAULT_CA_CERT_PATH = Path.of("/etc/euclid/euclid_cert.crt");

    /**
     * The base URL of the server to which the {@code EuclidEam} client will connect.
     * This URL serves as the root endpoint for all API interactions, and defines the
     * base address against which relative paths for specific API calls are constructed.
     * <p>
     * It is a required configuration parameter and must be properly set to ensure the
     * client can communicate with the server.
     */
    private final String baseUrl;

    /**
     * Represents the endpoint path used for user login authentication within the application.
     * This path is appended to the base server URL during the authentication process.
     * It typically points to a specific API route or endpoint used for initiating login requests.
     */
    private String loginPath = "/";

    /**
     * Represents the username used for authentication within the {@code EuclidEam} class.
     * This variable stores the login name required to establish a session with the server.
     */
    private String username;

    /**
     * Email address to authenticate with when no username is set. The server resolves the user by
     * user ID first and falls back to this, so exactly one of the two identifies the account.
     */
    private String email;

    /**
     * Stores the password used for authentication.
     * The password is intended to be set and managed securely
     * within the lifecycle of the {@code EuclidEam} instance.
     * <p>
     * Note: This field should never be directly exposed or logged
     * to ensure the security of sensitive authentication data.
     */
    private String password;

    /**
     * Represents the target for a specific operation or request within the {@code EuclidEam} system.
     * This value typically specifies the resource or endpoint that the operation should be
     * directed toward.
     */
    private String target;

    /**
     * Represents the specific action to be performed in the context of the {@code EuclidEam} class.
     * This variable holds the action identifier or descriptor needed for executing
     * operations or communicating with the server.
     */
    private String action;

    /**
     * Represents the file path to the Certificate Authority (CA) certificate
     * used for server authentication. This variable is initialized with the default
     * CA certificate path if it is readable. Otherwise, it is set to {@code null}.
     *
     * The CA certificate is essential for establishing secure communication with
     * the server by verifying its authenticity during the TLS handshake.
     *
     * Use this field to specify a custom path to the CA certificate when
     * configuring a server connection.
     */
    private String caCertPath = Files.isReadable(DEFAULT_CA_CERT_PATH) ? DEFAULT_CA_CERT_PATH.toString() : null;

    /**
     * Namespace to make active for the session on successful login, or {@code null} to leave it
     * unscoped. Set via {@link #namespace(String)}.
     */
    private String namespace;

    /**
     * Constructs an instance of {@code EuclidEam} with the specified base URL.
     *
     * @param baseUrl the base URL of the server to be used for making API requests; must not be null
     */
    private EuclidEam(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Creates an instance of {@code EuclidEam} for the specified server base URL.
     * This method ensures that the trailing slash, if present, is removed from the base URL.
     *
     * @param baseUrl the base URL of the server to be used for API interactions; must not be null
     * @return a new instance of {@code EuclidEam} configured with the given base URL
     * @throws NullPointerException if {@code baseUrl} is null
     */
    public static EuclidEam forServer(String baseUrl) {
        Objects.requireNonNull(baseUrl, "baseUrl must not be null");
        return new EuclidEam(stripTrailingSlash(baseUrl));
    }

    /**
     * Sets the login path for the instance of {@code EuclidEam} and ensures that the path
     * begins with a forward slash, adding one if necessary. Returns the current instance
     * of {@code EuclidEam}.
     *
     * @param loginPath the login path to be set; must not be null
     * @return the current instance of {@code EuclidEam}
     * @throws NullPointerException if {@code loginPath} is null
     */
    public EuclidEam loginPath(String loginPath) {
        Objects.requireNonNull(loginPath, "loginPath must not be null");
        this.loginPath = loginPath.startsWith("/") ? loginPath : "/" + loginPath;
        return this;
    }

    /**
     * Sets the path to the certificate authority (CA) certificate used for server authentication
     * and returns the current instance of {@code EuclidEam}.
     *
     * @param caCertPath the file path to the CA certificate
     * @return the current instance of {@code EuclidEam}
     */
    public EuclidEam caCertPath(String caCertPath) {
        this.caCertPath = caCertPath;
        return this;
    }

    /**
     * Sets the username to be used for authentication and returns the current instance of {@code EuclidEam}.
     *
     * @param username the username to be set
     * @return the current instance of {@code EuclidEam}
     */
    public EuclidEam username(String username) {
        this.username = username;
        return this;
    }

    /**
     * Sets the password to be used for authentication and returns the current instance of {@code EuclidEam}.
     *
     * @param password the password to be set
     * @return the current instance of {@code EuclidEam}
     */
    public EuclidEam password(String password) {
        this.password = password;
        return this;
    }

    /**
     * Sets the username and password to be used for authentication and returns the current instance of {@code EuclidEam}.
     *
     * @param username the username to be set
     * @param password the password to be set
     * @return the current instance of {@code EuclidEam}
     */
    public EuclidEam credentials(String username, String password) {
        return username(username).password(password);
    }

    /**
     * Sets the email address to authenticate with instead of a username. The server looks the user
     * up by user ID first and only falls back to the email when no user ID was given, so set one or
     * the other - a username set alongside an email wins.
     *
     * @param email the email address to log in with
     * @return the current instance of {@code EuclidEam}
     */
    public EuclidEam email(String email) {
        this.email = email;
        return this;
    }

    /**
     * Sets the namespace to make active for the session once login succeeds - validated and
     * applied via a follow-up {@link EuclidSession#changeNamespace(String)} call, mirroring
     * euclid-cli's "eam login --namespace" option. Every namespace-scoped command run afterward
     * through the returned session is automatically restricted to it, until changed again.
     *
     * @param namespace the namespace to make active
     * @return the current instance of {@code EuclidEam}
     */
    public EuclidEam namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    /**
     * Sets the target value and returns the current instance of {@code EuclidEam}.
     *
     * @param target the target value to be set
     * @return the current instance of {@code EuclidEam}
     */
    public EuclidEam target(String target) {
        this.target = target;
        return this;
    }

    /**
     * Sets the action to be performed and returns the current instance of {@code EuclidEam}.
     *
     * @param action the action to be set
     * @return the current instance of {@code EuclidEam}
     */
    public EuclidEam action(String action) {
        this.action = action;
        return this;
    }

    /**
     * Authenticates the user using the configured username and password, and establishes a session
     * with the server. If a cached session is available and still valid, it will be returned instead
     * of performing authentication again.
     *
     * @return an active {@code EuclidSession} object representing the authenticated session
     * @throws IOException if an error occurs during network communication or while processing the server response
     * @throws InterruptedException if the operation is interrupted during execution
     * @throws NullPointerException if the password, or both the username and email, are not set
     *                               before calling this method
     * @throws EuclidAuthenticationException if the server responds with an authentication failure
     */
    public EuclidSession login() throws IOException, InterruptedException {
        EuclidSession cached = loadCachedSession();
        if (cached != null) {
            // A cached session may have been established - by this caller or by euclid-cli,
            // since both share the same credentials file - before namespace(...) was ever asked
            // for, or scoped to a different one. Switching it here is what makes namespace(...)
            // apply regardless of whether login() ends up reusing a cached session or doing a
            // fresh one.
            return namespace != null && !namespace.equals(cached.nameSpace())
                    ? cached.changeNamespace(namespace)
                    : cached;
        }

        if ((username == null || username.isEmpty()) && (email == null || email.isEmpty())) {
            throw new NullPointerException("username or email must be set before calling login()");
        }
        Objects.requireNonNull(password, "password must be set before calling login()");

        // Only one identifier is sent: the server takes the user ID when it is present and only
        // falls back to the email otherwise, so sending both would silently ignore the email.
        String body = OBJECT_MAPPER.writeValueAsString(LoginRequest.builder()
                .userId(username == null ? "" : username).password(password)
                .email(username == null || username.isEmpty() ? (email == null ? "" : email) : "").build());
        Map<String, String> headers = Map.of("Content-Type", "application/json");
        HttpResponse<String> response = new EuclidHttpClient(caCertPath).post(baseUrl + loginPath, body, "eam", "login", headers);

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }

        EuclidSession session = extractSession(response.body(), baseUrl, caCertPath);
        if (namespace != null) {
            session = session.changeNamespace(namespace);
        }
        storeCredentials(session);
        return session;
    }

    /**
     * Loads a previously cached session from the local file system, if available and valid.
     * This method attempts to read the cached session information stored in a predefined
     * location. If the credentials file is not readable, invalid, or the token is no longer
     * valid, the method returns {@code null}.
     *
     * @return an instance of {@code EuclidSession} if a valid cached session exists;
     *         {@code null} otherwise
     */
    private EuclidSession loadCachedSession() {
        if (!Files.isReadable(credentialsPath())) {
            return null;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(Files.readString(credentialsPath()));
            String token = textOrNull(root, "token");
            if (token == null || !baseUrl.equals(textOrNull(root, "baseUrl")) || !isTokenValid(token)) {
                return null;
            }
            return new EuclidSession(token, textOrNull(root, "userId"), textOrNull(root, "accountId"),
                    textOrNull(root, "region"), textOrNull(root, "accessKeyId"), textOrNull(root, "secretAccessKey"),
                    root.path("isAdmin").asBoolean(false), root.toString(), baseUrl, caCertPath,
                    textOrNull(root, "namespace"));
        } catch (IOException ignored) {
            return null;
        }
    }

    /**
     * Stores the provided session credentials to a file on the file system.
     * This includes details such as token, user ID, account ID, region,
     * access key, secret access key, and base URL. The file is created if it
     * does not already exist, and POSIX file permissions are set to restrict
     * access when supported by the file system.
     *
     * @param session the {@code EuclidSession} object containing the credentials
     *                to be saved
     * @throws IOException if an error occurs while writing to the file system
     */
    private void storeCredentials(EuclidSession session) throws IOException {
        Files.createDirectories(credentialsPath().getParent());
        // Field names and shape are euclid-cli's Credentials::Save(), since both clients read and
        // write the same $HOME/.euclid/credentials: the namespace key is "namespace" (not
        // "nameSpace"), isAdmin travels alongside the token, and an absent namespace is written as
        // an empty string rather than null so the CLI's string reader can take it. "baseUrl" is the
        // one field only this client writes - the CLI has no equivalent check.
        Map<String, Object> credentials = new LinkedHashMap<>();
        credentials.put("token", session.token());
        credentials.put("userId", session.userId());
        credentials.put("accountId", session.accountId());
        credentials.put("region", session.region());
        credentials.put("accessKeyId", session.accessKeyId());
        credentials.put("secretAccessKey", session.secretAccessKey());
        credentials.put("isAdmin", session.isAdmin());
        credentials.put("baseUrl", baseUrl);
        credentials.put("namespace", session.nameSpace() == null ? "" : session.nameSpace());
        Files.writeString(credentialsPath(), OBJECT_MAPPER.writeValueAsString(credentials));
        try {
            Files.setPosixFilePermissions(credentialsPath(), PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException ignored) {
            // best-effort; not every filesystem supports POSIX permissions
        }
    }

    /**
     * Patches the cached credentials file's namespace in place, if one is cached for
     * {@code baseUrl} - keeping the on-disk cache in sync when a session's namespace is changed
     * outside of {@link #login()} (i.e. via {@link EuclidSession#changeNamespace(String)} on an
     * already-cached session). Silently a no-op if nothing is cached yet for {@code baseUrl},
     * mirroring the best-effort nature of {@link #loadCachedSession()}.
     *
     * @param baseUrl   the server the cached session must be for
     * @param namespace the namespace to record
     */
    static void updateCachedNamespace(String baseUrl, String namespace) {
        if (!Files.isReadable(credentialsPath())) {
            return;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(Files.readString(credentialsPath()));
            if (!baseUrl.equals(textOrNull(root, "baseUrl"))) {
                return;
            }
            Map<String, Object> credentials = new LinkedHashMap<>();
            credentials.put("token", textOrNull(root, "token"));
            credentials.put("userId", textOrNull(root, "userId"));
            credentials.put("accountId", textOrNull(root, "accountId"));
            credentials.put("region", textOrNull(root, "region"));
            credentials.put("accessKeyId", textOrNull(root, "accessKeyId"));
            credentials.put("secretAccessKey", textOrNull(root, "secretAccessKey"));
            credentials.put("isAdmin", root.path("isAdmin").asBoolean(false));
            credentials.put("baseUrl", baseUrl);
            credentials.put("namespace", namespace == null ? "" : namespace);
            Files.writeString(credentialsPath(), OBJECT_MAPPER.writeValueAsString(credentials));
        } catch (IOException ignored) {
            // best-effort; nothing cached to patch
        }
    }

    /**
     * Validates the provided token to determine if it is correctly formatted and not expired.
     *
     * @param token the token to be validated; must be a non-null string
     * @return {@code true} if the token is correctly formatted and its expiration timestamp
     *         indicates it is still valid; {@code false} otherwise
     */
    private static boolean isTokenValid(String token) {
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            return false;
        }
        try {
            String payloadJson = new String(base64UrlDecode(parts[1]), StandardCharsets.UTF_8);
            JsonNode exp = OBJECT_MAPPER.readTree(payloadJson).get("exp");
            return exp != null && Instant.now().getEpochSecond() < exp.asLong();
        } catch (IllegalArgumentException | IOException e) {
            return false;
        }
    }

    /**
     * Decodes a Base64-URL encoded string into a byte array.
     * This method ensures proper padding for the input string before decoding.
     *
     * @param value the Base64-URL encoded string to decode; must not be null
     * @return a byte array containing the decoded data
     */
    private static byte[] base64UrlDecode(String value) {
        String padded = value.length() % 4 == 0 ? value : value + "=".repeat(4 - value.length() % 4);
        return Base64.getUrlDecoder().decode(padded);
    }

    /**
     * Extracts a {@code EuclidSession} object from the given JSON response body.
     * This method parses the response to retrieve session-related metadata,
     * including the token, user information, account details, and security keys,
     * and initializes a new session with this data.
     *
     * @param responseBody the JSON response body containing session data; must not be null
     * @param baseUrl the base URL for the server associated with the session; must not be null
     * @param caCertPath the file path to the certificate authority (CA) certificate; must not be null
     * @return an instance of {@code EuclidSession} containing the extracted session details
     * @throws IOException if an error occurs while reading or parsing the response body
     */
    private static EuclidSession extractSession(String responseBody, String baseUrl, String caCertPath) throws IOException {
        LoginResponse login = extractLoginResponse(responseBody);
        Metadata metadata = login.metadata();
        return new EuclidSession(login.token(), metadata == null ? null : metadata.user(),
                metadata == null ? null : metadata.accountId(), metadata == null ? null : metadata.region(),
                login.accessKeyId(), login.secretAccessKey(), login.isAdmin(), responseBody, baseUrl,
                caCertPath, null);
    }

    /**
     * Parses a login response. The caller's identity travels in a nested {@code "metadata"} object,
     * so the account and region a session is scoped to come from there rather than the top level.
     *
     * @param responseBody the JSON response body
     * @return the parsed login response
     * @throws IOException if the response body cannot be parsed
     */
    private static LoginResponse extractLoginResponse(String responseBody) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        JsonNode metadata = root.get("metadata");
        return LoginResponse.builder()
                .metadata(metadata == null || !metadata.isObject() ? null
                        : new Metadata(textOrNull(metadata, "region"), textOrNull(metadata, "accountId"),
                                textOrNull(metadata, "user")))
                .token(textOrNull(root, "token")).accessKeyId(textOrNull(root, "accessKeyId"))
                .secretAccessKey(textOrNull(root, "secretAccessKey")).createdAt(textOrNull(root, "createdAt"))
                .isAdmin(root.path("isAdmin").asBoolean(false)).build();
    }

    /**
     * Retrieves the text value of the specified field from the provided JSON node.
     * If the field is missing, null, or the root node is null, this method returns {@code null}.
     *
     * @param root the root JSON node to extract the field from; can be {@code null}
     * @param field the name of the field to retrieve the text value from; must not be {@code null}
     * @return the text value of the specified field, or {@code null} if the field does not exist,
     *         is null, or the root node is {@code null}
     */
    private static String textOrNull(JsonNode root, String field) {
        JsonNode node = root == null ? null : root.get(field);
        return node == null || node.isNull() ? null : node.asText();
    }

    /**
     * Removes the trailing slash from a given URL string if it exists.
     *
     * @param url the URL string from which the trailing slash will be removed; must not be null
     * @return the URL string without a trailing slash, or the original URL string if no trailing slash exists
     */
    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

}
