package de.jensvogt.euclid.module.access;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public final class EuclidAccess {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path CREDENTIALS_PATH = Path.of(System.getProperty("user.home"), ".euclid", "credentials");

    // Same default euclid-cli's --ca-cert uses. Only applied if the file is actually present, so
    // this is a no-op (falls back to the system trust store) on machines without an euclid
    // deployment - callers can still override or opt out via caCertPath(...).
    private static final Path DEFAULT_CA_CERT_PATH = Path.of("/etc/euclid/euclid_cert.crt");

    private final String baseUrl;
    private String loginPath = "/";
    private String username;
    private String password;
    private String target;
    private String action;
    private String caCertPath = Files.isReadable(DEFAULT_CA_CERT_PATH) ? DEFAULT_CA_CERT_PATH.toString() : null;

    private EuclidAccess(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public static EuclidAccess forServer(String baseUrl) {
        Objects.requireNonNull(baseUrl, "baseUrl must not be null");
        return new EuclidAccess(stripTrailingSlash(baseUrl));
    }

    public EuclidAccess loginPath(String loginPath) {
        Objects.requireNonNull(loginPath, "loginPath must not be null");
        this.loginPath = loginPath.startsWith("/") ? loginPath : "/" + loginPath;
        return this;
    }

    /**
     * Trusts an additional PEM CA certificate (alongside the system trust store) when connecting
     * over TLS - for reaching an euclid server presenting a self-signed development certificate,
     * mirroring euclid-cli's {@code --ca-cert} option. Overrides the default of
     * "/etc/euclid/euclid_cert.crt" (used automatically when that file is readable); pass
     * {@code null} or {@code ""} to trust only the system store instead.
     */
    public EuclidAccess caCertPath(String caCertPath) {
        this.caCertPath = caCertPath;
        return this;
    }

    public EuclidAccess username(String username) {
        this.username = username;
        return this;
    }

    public EuclidAccess password(String password) {
        this.password = password;
        return this;
    }

    public EuclidAccess credentials(String username, String password) {
        return username(username).password(password);
    }

    public EuclidAccess target(String target) {
        this.target = target;
        return this;
    }

    public EuclidAccess action(String action) {
        this.action = action;
        return this;
    }

    public EuclidSession login() throws IOException, InterruptedException {
        EuclidSession cached = loadCachedSession();
        if (cached != null) {
            return cached;
        }

        Objects.requireNonNull(username, "username must be set before calling login()");
        Objects.requireNonNull(password, "password must be set before calling login()");

        String body = OBJECT_MAPPER.writeValueAsString(new LoginRequest(username, password));
        Map<String, String> headers = Map.of("Content-Type", "application/json");
        HttpResponse<String> response = new EuclidHttpClient(caCertPath).post(baseUrl + loginPath, body, "access", "login", headers);

        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }

        EuclidSession session = extractSession(response.body(), baseUrl, caCertPath);
        storeCredentials(session);
        return session;
    }

    private EuclidSession loadCachedSession() {
        if (!Files.isReadable(CREDENTIALS_PATH)) {
            return null;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(Files.readString(CREDENTIALS_PATH));
            String token = textOrNull(root, "token");
            if (token == null || !baseUrl.equals(textOrNull(root, "baseUrl")) || !isTokenValid(token)) {
                return null;
            }
            return new EuclidSession(token, textOrNull(root, "userId"), textOrNull(root, "accountId"),
                    textOrNull(root, "region"), textOrNull(root, "accessKeyId"), textOrNull(root, "secretAccessKey"),
                    root.toString(), baseUrl, caCertPath);
        } catch (IOException ignored) {
            return null;
        }
    }

    private void storeCredentials(EuclidSession session) throws IOException {
        Files.createDirectories(CREDENTIALS_PATH.getParent());
        Map<String, String> credentials = new LinkedHashMap<>();
        credentials.put("token", session.token());
        credentials.put("userId", session.userId());
        credentials.put("accountId", session.accountId());
        credentials.put("region", session.region());
        credentials.put("accessKeyId", session.accessKeyId());
        credentials.put("secretAccessKey", session.secretAccessKey());
        credentials.put("baseUrl", baseUrl);
        Files.writeString(CREDENTIALS_PATH, OBJECT_MAPPER.writeValueAsString(credentials));
        try {
            Files.setPosixFilePermissions(CREDENTIALS_PATH, PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException ignored) {
            // best-effort; not every filesystem supports POSIX permissions
        }
    }

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

    private static byte[] base64UrlDecode(String value) {
        String padded = value.length() % 4 == 0 ? value : value + "=".repeat(4 - value.length() % 4);
        return Base64.getUrlDecoder().decode(padded);
    }

    private static EuclidSession extractSession(String responseBody, String baseUrl, String caCertPath) throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        JsonNode metadata = root.get("metadata");
        return new EuclidSession(textOrNull(root, "token"), textOrNull(metadata, "user"),
                textOrNull(metadata, "accountId"), textOrNull(metadata, "region"),
                textOrNull(root, "accessKeyId"), textOrNull(root, "secretAccessKey"), responseBody, baseUrl, caCertPath);
    }

    private static String textOrNull(JsonNode root, String field) {
        JsonNode node = root == null ? null : root.get(field);
        return node == null || node.isNull() ? null : node.asText();
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private record LoginRequest(String userId, String password) {
    }
}
