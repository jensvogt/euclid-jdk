package de.jensvogt.euclid.module.eam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jensvogt.euclid.dto.Metadata;
import de.jensvogt.euclid.dto.eam.ListMyAccountsResponse;
import de.jensvogt.euclid.dto.eam.LoginRequest;
import de.jensvogt.euclid.dto.eam.LoginResponse;
import de.jensvogt.euclid.dto.eam.OidcAuthorizeRequest;
import de.jensvogt.euclid.dto.eam.OidcAuthorizeResponse;
import de.jensvogt.euclid.dto.eam.OidcLoginRequest;
import de.jensvogt.euclid.dto.eam.SamlAcsRequest;
import de.jensvogt.euclid.dto.eam.SamlAuthorizeRequest;
import de.jensvogt.euclid.dto.eam.SamlAuthorizeResponse;
import de.jensvogt.euclid.dto.eam.model.Account;
import de.jensvogt.euclid.exception.EuclidAuthenticationException;
import de.jensvogt.euclid.exception.EuclidServiceException;
import de.jensvogt.euclid.http.EuclidHttpClient;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Fluent builder for authenticating against an Euclid server.
 *
 * <pre>{@code
 * EuclidSession session = EuclidEam.forServer("https://euclid.example.com")
 *         .credentials("jens", "secret")
 *         .login();
 * }</pre>
 *
 * <p>An installation that federates its logins to an identity provider is signed into through a
 * browser instead, exactly as euclid-cli's {@code eam login --oidc} / {@code --saml} do - no
 * password is asked for or sent, because the provider decides who is signing in:
 *
 * <pre>{@code
 * EuclidSession session = EuclidEam.forServer("https://euclid.example.com")
 *         .loginWithOidc();      // or loginWithSaml()
 * }</pre>
 *
 * <p>Every way in ends the same way: the session is cached in {@code $HOME/.euclid/credentials},
 * shared with euclid-cli, and reused by the next login while its token is valid. A federated
 * session is not a different kind of session - the server issues the same token, access key and
 * admin flag whichever proof it was given.
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
     * Account to work in once login succeeds - by account ID or by name - or {@code null} for the
     * user's home account. Set via {@link #account(String)}.
     */
    private String account;

    /**
     * How a federated login shows the person the URL to authenticate at. Defaults to opening the
     * system browser, with the URL printed to standard error either way.
     */
    private Consumer<String> browser = EuclidEam::openInBrowser;

    /**
     * How long a federated login waits for the browser to come back, the same three minutes
     * euclid-cli waits.
     */
    private Duration callbackTimeout = Duration.ofMinutes(3);

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
     * Sets the account the session should work in once login succeeds, instead of the user's home
     * account - mirroring euclid-cli's {@code eam login --account}. Every request made through the
     * returned session then carries it as {@code x-euclid-account-id}.
     * <p>
     * Given by account ID or by name, and checked against {@link EuclidSession#listMyAccounts()}
     * with the session just issued: only the server knows which accounts a grant covers, and an
     * account the user may not work in is one every later call would be refused in, one at a time,
     * for a reason none of them would say. An ID is exact and wins outright; a name is taken only
     * when no ID matched, since nothing stops an account being named like another account's ID.
     * <p>
     * Applied before {@link #namespace(String)}, since a namespace belongs to an account.
     *
     * @param account the account ID or name to work in
     * @return the current instance of {@code EuclidEam}
     */
    public EuclidEam account(String account) {
        this.account = account;
        return this;
    }

    /**
     * Sets how a federated login ({@link #loginWithOidc()}, {@link #loginWithSaml()}) shows the
     * person the URL they have to authenticate at.
     * <p>
     * The default opens the system browser and prints the URL to standard error whether or not
     * that works, so somebody on a machine without one - over ssh, in a container - can copy it to
     * one that has. An application with a user interface of its own will rather show the link
     * there; a test drives the flow by fetching the URL itself.
     *
     * @param browser receives the URL to open; called once per login, before the wait begins
     * @return the current instance of {@code EuclidEam}
     * @throws NullPointerException if {@code browser} is null
     */
    public EuclidEam browser(Consumer<String> browser) {
        this.browser = Objects.requireNonNull(browser, "browser must not be null");
        return this;
    }

    /**
     * Sets how long a federated login waits for the browser to come back before giving up. Three
     * minutes unless set - long enough to type a password and a second factor, short enough that a
     * login nobody is attending to does not hold a port open indefinitely.
     *
     * @param callbackTimeout how long to wait for the callback
     * @return the current instance of {@code EuclidEam}
     * @throws NullPointerException if {@code callbackTimeout} is null
     */
    public EuclidEam callbackTimeout(Duration callbackTimeout) {
        this.callbackTimeout = Objects.requireNonNull(callbackTimeout, "callbackTimeout must not be null");
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
     * <p>
     * Either way the session is moved to the {@link #account(String)} and {@link #namespace(String)}
     * asked for, if any, before it is returned and cached.
     *
     * @return an active {@code EuclidSession} object representing the authenticated session
     * @throws IllegalArgumentException if {@link #account(String)} names an account the user may
     *                               not work in; the session is cached in the home account all the same
     * @throws IOException if an error occurs during network communication or while processing the server response
     * @throws InterruptedException if the operation is interrupted during execution
     * @throws NullPointerException if the password, or both the username and email, are not set
     *                               before calling this method
     * @throws EuclidAuthenticationException if the server responds with an authentication failure
     */
    public EuclidSession login() throws IOException, InterruptedException {
        EuclidSession cached = cachedSession();
        if (cached != null) {
            return cached;
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

        return establish(response.body());
    }

    /**
     * Signs in through the installation's OpenID Connect provider, in a browser, the way
     * euclid-cli's {@code eam login --oidc} does - and caches the session exactly as
     * {@link #login()} does.
     * <p>
     * The flow: a listener is bound on a loopback port first, because the redirect URI has to name
     * the port it ended up on; the server is asked ({@code oidc-authorize}) where to send the
     * person, with that listener as the redirect URI; the URL goes to the {@link #browser(Consumer)};
     * and once the provider redirects back with a code, the code and its state are redeemed
     * ({@code oidc-login}) for an ordinary euclid session. No password is asked for or sent - the
     * provider says who is signing in - so a username or password set on this builder is ignored.
     * <p>
     * A still-valid cached session is reused instead, as {@link #login()} reuses one: the point of
     * caching is not having to go through the browser again within the hour.
     * <p>
     * The state that comes back is checked against the one that went out before anything is
     * redeemed. The server would refuse a state it did not seal anyway, but a mismatch caught here
     * says plainly that the callback belongs to a different login attempt.
     *
     * @return an active session, cached for reuse
     * @throws EuclidServiceException        if the server cannot start the flow - OIDC is not
     *                                       enabled (404), misconfigured (500), or the provider is
     *                                       unreachable (502)
     * @throws EuclidAuthenticationException if the provider refused the login, the callback does
     *                                       not belong to this attempt, or the server would not
     *                                       redeem the code (401) or give this identity an account
     *                                       (403)
     * @throws HttpTimeoutException          if the browser did not come back within the
     *                                       {@link #callbackTimeout(Duration)}
     * @throws IOException                   if the listener cannot be bound, or on any other I/O
     *                                       failure
     * @throws InterruptedException          if the thread is interrupted while waiting
     */
    public EuclidSession loginWithOidc() throws IOException, InterruptedException {
        EuclidSession cached = cachedSession();
        if (cached != null) {
            return cached;
        }

        try (LoopbackCallbackListener listener = new LoopbackCallbackListener()) {
            OidcAuthorizeResponse authorization = oidcAuthorize(
                    OidcAuthorizeRequest.builder().redirectUri(listener.callbackUrl()).build());
            if (authorization.authorizationUrl() == null || authorization.authorizationUrl().isEmpty()) {
                throw new EuclidServiceException("eam", "oidc-authorize", 200, "the server did not say where to authenticate");
            }

            LoopbackCallbackListener.Callback callback = awaitCallback(listener, authorization.authorizationUrl());
            if (!Objects.equals(callback.state(), authorization.state())) {
                throw new EuclidAuthenticationException(401, "the callback does not belong to this login attempt");
            }

            return oidcLogin(OidcLoginRequest.builder().code(callback.code()).state(callback.state()).build());
        }
    }

    /**
     * Signs in through the installation's SAML 2.0 identity provider, in a browser, the way
     * euclid-cli's {@code eam login --saml} does - and caches the session exactly as
     * {@link #login()} does.
     * <p>
     * Unlike OIDC, nothing about this client is ever registered with the provider: the provider
     * posts its signed assertion to the server's own assertion consumer service, the server
     * verifies it and issues the session, and only then hands that session to the loopback
     * listener named as {@code returnTo} - in a self-submitting form post, so the token never
     * appears in a URL. The listener therefore receives a finished session rather than something
     * to redeem.
     * <p>
     * A still-valid cached session is reused instead, as {@link #login()} reuses one.
     *
     * @return an active session, cached for reuse
     * @throws EuclidServiceException        if the server cannot start the flow - SAML is not
     *                                       enabled (404), or misconfigured (500)
     * @throws EuclidAuthenticationException if the provider refused the login, or the browser came
     *                                       back without a session
     * @throws HttpTimeoutException          if the browser did not come back within the
     *                                       {@link #callbackTimeout(Duration)}
     * @throws IOException                   if the listener cannot be bound, or on any other I/O
     *                                       failure
     * @throws InterruptedException          if the thread is interrupted while waiting
     */
    public EuclidSession loginWithSaml() throws IOException, InterruptedException {
        EuclidSession cached = cachedSession();
        if (cached != null) {
            return cached;
        }

        try (LoopbackCallbackListener listener = new LoopbackCallbackListener()) {
            SamlAuthorizeResponse authentication = samlAuthorize(
                    SamlAuthorizeRequest.builder().returnTo(listener.callbackUrl()).build());
            if (authentication.authenticationUrl() == null || authentication.authenticationUrl().isEmpty()) {
                throw new EuclidServiceException("eam", "saml-authorize", 200, "the server did not say where to authenticate");
            }

            LoopbackCallbackListener.Callback callback = awaitCallback(listener, authentication.authenticationUrl());
            if (callback.session() == null || callback.session().isEmpty()) {
                throw new EuclidAuthenticationException(401, "the callback carried no session");
            }

            JsonNode session;
            try {
                session = OBJECT_MAPPER.readTree(callback.session());
            } catch (IOException e) {
                throw new EuclidAuthenticationException(401, "the session euclid sent back is not valid JSON");
            }
            if (textOrNull(session, "token") == null || textOrNull(session, "token").isEmpty()) {
                throw new EuclidAuthenticationException(401, "the session euclid sent back did not contain a token");
            }
            return establish(callback.session());
        }
    }

    /**
     * Turns a SAML response obtained some other way into a session, by handing it to the server's
     * assertion consumer service ({@code saml-acs}) - the last step of euclid-cli's
     * {@code eam login --onelogin}, which fetches the assertion from OneLogin's API rather than
     * through a browser.
     * <p>
     * Verified exactly as one a browser posts: the provider's signature is what is trusted, not how
     * the assertion arrived. An assertion with no RelayState answers no login request euclid made,
     * so the server refuses it unless it allows IdP-initiated logins
     * ({@code saml.allow-idp-initiated}); and each assertion is accepted once only.
     * <p>
     * Never answered from the cache, unlike {@link #login()}: the caller has spent an assertion on
     * this, and quietly returning some other session would discard it.
     *
     * @param request the base64-encoded SAML response, and the RelayState it came with
     * @return an active session, cached for reuse
     * @throws EuclidAuthenticationException if the assertion is refused - unsigned, expired,
     *                                       unsolicited, or replayed (401) - or the identity may not
     *                                       have an account here (403)
     * @throws IOException                   if an I/O error occurs
     * @throws InterruptedException          if the request is interrupted
     */
    public EuclidSession loginWithSamlAssertion(SamlAcsRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = new EuclidHttpClient(caCertPath).post(baseUrl + loginPath,
                OBJECT_MAPPER.writeValueAsString(request), "eam", "saml-acs", Map.of("Content-Type", "application/json"));
        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }
        return establish(response.body());
    }

    /**
     * Starts an OpenID Connect login without driving it: answers with where to send the person and
     * the state the attempt travels under ({@code oidc-authorize}).
     * <p>
     * The building block for a caller that runs the browser leg itself - a web application that
     * registers its own callback, say - and finishes with {@link #oidcLogin(OidcLoginRequest)}.
     * {@link #loginWithOidc()} is this and the rest of the flow put together for a program on the
     * signing-in person's own machine.
     *
     * @param request where the provider should send the browser back to, and where the server
     *                should send it once the login has succeeded
     * @return the authorization URL to open, and the state to expect back
     * @throws EuclidServiceException if OIDC is not enabled (404), misconfigured (500), the
     *                                {@code returnTo} is not a permitted destination (400), or the
     *                                provider is unreachable (502)
     * @throws IOException            if an I/O error occurs
     * @throws InterruptedException   if the request is interrupted
     */
    public OidcAuthorizeResponse oidcAuthorize(OidcAuthorizeRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = new EuclidHttpClient(caCertPath).post(baseUrl + loginPath,
                OBJECT_MAPPER.writeValueAsString(request), "eam", "oidc-authorize", Map.of("Content-Type", "application/json"));
        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eam", "oidc-authorize", response.statusCode(), response.body());
        }
        return OBJECT_MAPPER.readValue(response.body(), OidcAuthorizeResponse.class);
    }

    /**
     * Finishes an OpenID Connect login: redeems the code the provider sent back for a euclid session
     * ({@code oidc-login}), and caches it as {@link #login()} does - applying
     * {@link #account(String)} and {@link #namespace(String)} on the way.
     * <p>
     * Never answered from the cache: a code is good for one redemption, and the caller has
     * presumably just been through a browser to get it.
     *
     * @param request the code and state from the provider's callback, exactly as they arrived
     * @return an active session, cached for reuse
     * @throws EuclidAuthenticationException if the code or state is refused (401), or the identity
     *                                       may not have an account here (403)
     * @throws IOException                   if an I/O error occurs
     * @throws InterruptedException          if the request is interrupted
     */
    public EuclidSession oidcLogin(OidcLoginRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = new EuclidHttpClient(caCertPath).post(baseUrl + loginPath,
                OBJECT_MAPPER.writeValueAsString(request), "eam", "oidc-login", Map.of("Content-Type", "application/json"));
        if (response.statusCode() / 100 != 2) {
            throw new EuclidAuthenticationException(response.statusCode(), response.body());
        }
        return establish(response.body());
    }

    /**
     * Starts a SAML login without driving it: answers with where to send the person and the
     * RelayState the attempt travels under ({@code saml-authorize}, also reachable as
     * {@code saml-login}).
     * <p>
     * The building block for a caller that runs the browser leg itself; {@link #loginWithSaml()}
     * is the whole flow for a program on the signing-in person's own machine. Where the session
     * ends up is decided by {@link SamlAuthorizeRequest#returnTo()}.
     *
     * @param request where the server should deliver the session once the assertion is accepted
     * @return the single sign-on URL to open, and the RelayState it carries
     * @throws EuclidServiceException if SAML is not enabled (404), misconfigured (500), or the
     *                                {@code returnTo} is not a permitted destination (400)
     * @throws IOException            if an I/O error occurs
     * @throws InterruptedException   if the request is interrupted
     */
    public SamlAuthorizeResponse samlAuthorize(SamlAuthorizeRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = new EuclidHttpClient(caCertPath).post(baseUrl + loginPath,
                OBJECT_MAPPER.writeValueAsString(request), "eam", "saml-authorize", Map.of("Content-Type", "application/json"));
        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException("eam", "saml-authorize", response.statusCode(), response.body());
        }
        return OBJECT_MAPPER.readValue(response.body(), SamlAuthorizeResponse.class);
    }

    /**
     * Hands the URL to the browser and waits for it to come back, turning the two ways that can go
     * wrong - no answer, or the provider's refusal - into exceptions.
     *
     * @param listener the listener the browser will come back to
     * @param url      the URL the person has to authenticate at
     * @return the callback, carrying no error
     * @throws HttpTimeoutException if nothing came back in time
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    private LoopbackCallbackListener.Callback awaitCallback(LoopbackCallbackListener listener, String url)
            throws HttpTimeoutException, InterruptedException {
        browser.accept(url);
        LoopbackCallbackListener.Callback callback = listener.await(callbackTimeout);
        if (callback == null) {
            throw new HttpTimeoutException("no callback from the identity provider within " + callbackTimeout);
        }
        if (callback.error() != null) {
            throw new EuclidAuthenticationException(401, "the identity provider refused the login: " + callback.error());
        }
        return callback;
    }

    /**
     * A still-valid cached session for this server, with {@link #account(String)} and
     * {@link #namespace(String)} applied to it, or {@code null} if there is none to reuse.
     * <p>
     * A cached session may have been established - by this caller or by euclid-cli, since both
     * share the same credentials file - before an account or namespace was ever asked for, or
     * scoped to different ones. Switching it here is what makes those settings apply whether a
     * login ends up reusing a cached session or doing a fresh one.
     *
     * @return the session to reuse, or {@code null}
     * @throws IOException          if switching the account or namespace fails on the wire
     * @throws InterruptedException if switching is interrupted
     */
    private EuclidSession cachedSession() throws IOException, InterruptedException {
        EuclidSession cached = loadCachedSession();
        if (cached == null) {
            return null;
        }
        EuclidSession selected = applySelections(cached);
        if (selected != cached) {
            storeCredentials(selected);
        }
        return selected;
    }

    /**
     * What every way in does with the login response it got: build the session, move it to the
     * account and namespace asked for, and cache it. Shared deliberately - a federated login is not
     * a different kind of session, and a difference between the paths here would be one nobody
     * meant to introduce.
     *
     * @param responseBody the login response JSON
     * @return the established session
     * @throws IOException          if the response cannot be parsed or a follow-up call fails
     * @throws InterruptedException if a follow-up call is interrupted
     */
    private EuclidSession establish(String responseBody) throws IOException, InterruptedException {
        EuclidSession session = applySelections(extractSession(responseBody, baseUrl, caCertPath));
        storeCredentials(session);
        return session;
    }

    /**
     * Moves a session to the account and namespace this builder was asked for - the account first,
     * because a namespace belongs to one.
     *
     * @param session the session as issued or cached
     * @return the same instance if nothing had to change, otherwise the switched session
     * @throws IOException          if a follow-up call fails
     * @throws InterruptedException if a follow-up call is interrupted
     */
    private EuclidSession applySelections(EuclidSession session) throws IOException, InterruptedException {
        EuclidSession selected = session;
        if (account != null && !account.isEmpty() && !account.equals(selected.accountId())) {
            selected = selectAccount(selected);
        }
        if (namespace != null && !namespace.equals(selected.nameSpace())) {
            selected = selected.changeNamespace(namespace);
        }
        return selected;
    }

    /**
     * Resolves {@link #account(String)} against the accounts the user may work in, as euclid-cli's
     * {@code resolveAccount} does.
     * <p>
     * On a miss the session is still cached, in the home account, before the refusal is thrown:
     * the login itself succeeded, and throwing the session away would make the next attempt a
     * second password entry - or a second trip through the browser - for a typo in an account name.
     *
     * @param session the session to switch
     * @return the session working in the resolved account
     * @throws IllegalArgumentException if the account is not one the user may work in; the message
     *                                  names the ones they may
     * @throws IOException              if listing the accounts fails
     * @throws InterruptedException     if listing the accounts is interrupted
     */
    private EuclidSession selectAccount(EuclidSession session) throws IOException, InterruptedException {
        ListMyAccountsResponse mine = session.listMyAccounts();
        String byName = null;
        List<String> known = new ArrayList<>();
        for (Account candidate : mine.accounts()) {
            if (account.equals(candidate.accountId())) {
                return session.withAccountId(candidate.accountId());
            }
            if (byName == null && candidate.name() != null && account.equals(candidate.name())) {
                byName = candidate.accountId();
            }
            known.add(candidate.name() == null || candidate.name().isEmpty() || candidate.name().equals(candidate.accountId())
                    ? candidate.accountId() : candidate.accountId() + " (" + candidate.name() + ")");
        }
        if (byName != null) {
            return session.withAccountId(byName);
        }

        storeCredentials(session);
        throw new IllegalArgumentException("'" + account + "' is not an account you may work in"
                + (known.isEmpty() ? "" : "; yours are: " + String.join(", ", known))
                + " - logged in to your home account " + session.accountId() + " instead");
    }

    /**
     * The default {@link #browser(Consumer)}: prints the URL, then opens it the way the platform
     * offers - best effort, and said out loud either way, so a person on a machine with no browser
     * can copy the URL somewhere that has one.
     *
     * @param url the URL to open
     */
    private static void openInBrowser(String url) {
        System.err.println("Opening your browser to sign in. If it does not open, go to:\n\n  " + url
                + "\n\nWaiting for the callback ...");
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
            // No shell in between, so nothing in the URL is ever interpreted as anything but an
            // argument.
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            List<String> command = os.contains("mac") ? List.of("open", url)
                    : os.contains("win") ? List.of("rundll32", "url.dll,FileProtocolHandler", url)
                    : List.of("xdg-open", url);
            new ProcessBuilder(command).redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD).start();
        } catch (IOException | RuntimeException ignored) {
            // The URL is printed above; that is the fallback.
        }
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
     *                to be saved; its own {@link EuclidSession#baseUrl()} is the server recorded
     * @throws IOException if an error occurs while writing to the file system
     */
    private static void storeCredentials(EuclidSession session) throws IOException {
        String baseUrl = session.baseUrl();
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
     * Replaces the cached session with a renewed one - see {@link EuclidSession#refreshSession()} -
     * but only when what is cached is this same user's session on this same server.
     * <p>
     * The file is shared with euclid-cli and with every other session this process holds, so a
     * refresh must not take it over: a process that logged in as one user while somebody else's
     * session sits in the cache would otherwise silently swap who the next command runs as. When
     * nothing is cached, nothing is written either - a session that was never cached has no copy
     * to keep current. Best-effort, like {@link #updateCachedNamespace(String, String)}.
     *
     * @param session the renewed session
     */
    static void updateCachedSession(EuclidSession session) {
        if (!Files.isReadable(credentialsPath())) {
            return;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(Files.readString(credentialsPath()));
            if (!Objects.equals(session.baseUrl(), textOrNull(root, "baseUrl"))
                    || !Objects.equals(session.userId(), textOrNull(root, "userId"))) {
                return;
            }
            storeCredentials(session);
        } catch (IOException ignored) {
            // best-effort; the renewed session is returned to the caller either way
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
    static EuclidSession extractSession(String responseBody, String baseUrl, String caCertPath) throws IOException {
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
