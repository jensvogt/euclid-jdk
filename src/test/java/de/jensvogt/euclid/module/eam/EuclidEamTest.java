package de.jensvogt.euclid.module.eam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.jensvogt.euclid.dto.eam.SamlAcsRequest;
import de.jensvogt.euclid.exception.EuclidAuthenticationException;
import de.jensvogt.euclid.exception.EuclidServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EuclidEamTest {

    private HttpServer server;

    // A successful login writes $HOME/.euclid/credentials - the very file a real euclid-cli or SDK
    // login on this machine lives in. Pointing "user.home" at a temp directory for the duration of
    // each test keeps the suite from overwriting it with a test token.
    @TempDir
    Path fakeHome;

    private String realUserHome;

    @BeforeEach
    void redirectHome() {
        realUserHome = System.getProperty("user.home");
        System.setProperty("user.home", fakeHome.toString());
    }

    @AfterEach
    void stopServer() {
        System.setProperty("user.home", realUserHome);
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void loginCarriesIsAdminFromTheServer() throws Exception {
        server = startServer("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, "{\"token\":\"abc123\",\"isAdmin\":true,"
                    + "\"metadata\":{\"region\":\"eu-central-1\",\"accountId\":\"863459426936\",\"user\":\"jens\"}}");
        });

        EuclidSession session = EuclidEam.forServer(baseUrl()).username("jens").password("s3cret").login();

        assertTrue(session.isAdmin());
        assertEquals("jens", session.userId());
        assertEquals("863459426936", session.accountId());
        assertEquals("eu-central-1", session.region());
    }

    // The credentials file is shared with euclid-cli, which reads the namespace from "namespace"
    // and expects "isAdmin" alongside the token - a mismatch here silently loses the namespace when
    // the other client picks the session up.
    @Test
    void loginWritesCredentialsInTheFormatTheCliReads() throws Exception {
        server = startServer("/", exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            exchange.getRequestBody().readAllBytes();
            if ("login".equals(action)) {
                sendResponse(exchange, 200, "{\"token\":\"abc123\",\"isAdmin\":true,\"accessKeyId\":\"AKIA1\","
                        + "\"secretAccessKey\":\"s3cr3t\",\"metadata\":{\"region\":\"eu-central-1\","
                        + "\"accountId\":\"863459426936\",\"user\":\"jens\"}}");
            } else {
                sendResponse(exchange, 200, "{}");
            }
        });

        EuclidEam.forServer(baseUrl()).username("jens").password("s3cret").namespace("prod").login();

        JsonNode stored = new ObjectMapper().readTree(Files.readString(fakeHome.resolve(".euclid/credentials")));
        assertEquals("prod", stored.path("namespace").asText(), "the CLI reads the namespace from \"namespace\"");
        assertTrue(stored.path("isAdmin").asBoolean(), "the CLI expects isAdmin alongside the token");
        assertEquals("abc123", stored.path("token").asText());
        assertEquals("AKIA1", stored.path("accessKeyId").asText());
        assertEquals("jens", stored.path("userId").asText());
    }

    @Test
    void loginReturnsSessionWithTokenForValidCredentials() throws Exception {
        AtomicReference<String> receivedBody = new AtomicReference<>();
        server = startServer("/", exchange -> {
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendResponse(exchange, 200, "{\"token\":\"abc123\"}");
        });

        EuclidSession session = EuclidEam.forServer(baseUrl())
                .username("jens")
                .password("s3cret")
                .login();

        assertEquals("abc123", session.token());
        assertTrue(receivedBody.get().contains("\"userId\":\"jens\""));
        assertTrue(receivedBody.get().contains("\"password\":\"s3cret\""));
    }

    @Test
    void loginWithNamespaceAppliesActiveNamespace() throws Exception {
        AtomicReference<String> changeNamespaceBody = new AtomicReference<>();
        server = startServer("/", exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            if ("login".equals(action)) {
                exchange.getRequestBody().readAllBytes();
                sendResponse(exchange, 200, "{\"token\":\"abc123\"}");
            } else if ("change-namespace".equals(action)) {
                changeNamespaceBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                sendResponse(exchange, 200, "{}");
            } else {
                sendResponse(exchange, 500, "{\"error\":\"unexpected action " + action + "\"}");
            }
        });

        EuclidSession session = EuclidEam.forServer(baseUrl())
                .username("jens")
                .password("s3cret")
                .namespace("prod")
                .login();

        assertEquals("prod", session.nameSpace());
        assertTrue(changeNamespaceBody.get().contains("\"namespace\":\"prod\""));
    }

    // The server resolves the user by ID first and only falls back to the email, so a login that
    // identifies the account by email must not also send a user ID.
    @Test
    void loginWithEmailSendsTheEmailAndNoUserId() throws Exception {
        AtomicReference<String> receivedBody = new AtomicReference<>();
        server = startServer("/", exchange -> {
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendResponse(exchange, 200, "{\"token\":\"abc123\"}");
        });

        EuclidSession session = EuclidEam.forServer(baseUrl())
                .email("jens@example.com")
                .password("s3cret")
                .login();

        assertEquals("abc123", session.token());
        assertTrue(receivedBody.get().contains("\"email\":\"jens@example.com\""));
        assertTrue(receivedBody.get().contains("\"userId\":\"\""), "a userId would win over the email");
    }

    // A username set alongside an email wins, so the email is left out rather than sent and ignored.
    @Test
    void loginWithUsernameLeavesTheEmailOut() throws Exception {
        AtomicReference<String> receivedBody = new AtomicReference<>();
        server = startServer("/", exchange -> {
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendResponse(exchange, 200, "{\"token\":\"abc123\"}");
        });

        EuclidEam.forServer(baseUrl()).username("jens").email("jens@example.com").password("s3cret").login();

        assertTrue(receivedBody.get().contains("\"userId\":\"jens\""));
        assertTrue(receivedBody.get().contains("\"email\":\"\""));
    }

    @Test
    void loginRequiresAUsernameOrAnEmail() {
        EuclidEam access = EuclidEam.forServer("http://localhost:1").password("s3cret");

        NullPointerException exception = assertThrows(NullPointerException.class, access::login);
        assertTrue(exception.getMessage().contains("username or email"));
    }

    @Test
    void loginThrowsAuthenticationExceptionForRejectedCredentials() throws Exception {
        server = startServer("/login", exchange -> sendResponse(exchange, 401, "{\"error\":\"invalid credentials\"}"));

        EuclidEam access = EuclidEam.forServer(baseUrl())
                .loginPath("/login")
                .username("jens")
                .password("wrong");

        EuclidAuthenticationException exception = assertThrows(EuclidAuthenticationException.class, access::login);
        assertEquals(401, exception.statusCode());
    }

    // ── Federated login ──────────────────────────────────────────────────

    // The whole OIDC round trip, with the "browser" played by a plain GET of the authorization URL:
    // the fake server's authorization URL is the listener's own redirect URI with the code on it,
    // which is what a provider's redirect amounts to once the person has signed in.
    @Test
    void loginWithOidcRedeemsTheCodeTheBrowserBroughtBack() throws Exception {
        AtomicReference<String> redirectUri = new AtomicReference<>();
        AtomicReference<String> loginBody = new AtomicReference<>();
        server = startServer("/", exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            JsonNode body = new ObjectMapper().readTree(exchange.getRequestBody().readAllBytes());
            if ("oidc-authorize".equals(action)) {
                redirectUri.set(body.path("redirectUri").asText());
                sendResponse(exchange, 200, "{\"authorizationUrl\":\"" + redirectUri.get()
                        + "?code=c0de&state=s3aled\",\"state\":\"s3aled\"}");
            } else if ("oidc-login".equals(action)) {
                loginBody.set(body.toString());
                sendResponse(exchange, 200, LOGIN_RESPONSE);
            } else {
                sendResponse(exchange, 500, "{\"error\":\"unexpected action " + action + "\"}");
            }
        });

        EuclidSession session = EuclidEam.forServer(baseUrl()).browser(EuclidEamTest::visit).loginWithOidc();

        assertTrue(redirectUri.get().startsWith("http://127.0.0.1:"), "the code has to come back to this machine");
        assertTrue(redirectUri.get().endsWith("/callback"));
        assertEquals("{\"code\":\"c0de\",\"state\":\"s3aled\"}", loginBody.get());
        assertEquals("fed-token", session.token());
        assertEquals("carol", session.userId());
        assertEquals("AKIAFED", session.accessKeyId());

        JsonNode stored = new ObjectMapper().readTree(Files.readString(fakeHome.resolve(".euclid/credentials")));
        assertEquals("fed-token", stored.path("token").asText(), "a federated session is cached like any other");
    }

    // EAM would refuse a state it did not seal anyway, but the code must not even be spent on a
    // callback that belongs to some other login attempt.
    @Test
    void loginWithOidcRefusesACallbackFromAnotherAttempt() throws Exception {
        AtomicBoolean redeemed = new AtomicBoolean();
        server = startServer("/", exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            JsonNode body = new ObjectMapper().readTree(exchange.getRequestBody().readAllBytes());
            if ("oidc-authorize".equals(action)) {
                sendResponse(exchange, 200, "{\"authorizationUrl\":\"" + body.path("redirectUri").asText()
                        + "?code=c0de&state=somebody-elses\",\"state\":\"mine\"}");
            } else {
                redeemed.set(true);
                sendResponse(exchange, 200, LOGIN_RESPONSE);
            }
        });

        EuclidEam access = EuclidEam.forServer(baseUrl()).browser(EuclidEamTest::visit);

        EuclidAuthenticationException exception = assertThrows(EuclidAuthenticationException.class, access::loginWithOidc);
        assertTrue(exception.getMessage().contains("does not belong to this login attempt"));
        assertFalse(redeemed.get());
    }

    @Test
    void loginWithOidcSurfacesTheProvidersRefusal() throws Exception {
        server = startServer("/", exchange -> {
            JsonNode body = new ObjectMapper().readTree(exchange.getRequestBody().readAllBytes());
            sendResponse(exchange, 200, "{\"authorizationUrl\":\"" + body.path("redirectUri").asText()
                    + "?error=access_denied&error_description=not+for+you\",\"state\":\"s\"}");
        });

        EuclidEam access = EuclidEam.forServer(baseUrl()).browser(EuclidEamTest::visit);

        EuclidAuthenticationException exception = assertThrows(EuclidAuthenticationException.class, access::loginWithOidc);
        assertTrue(exception.getMessage().contains("access_denied (not for you)"), exception.getMessage());
    }

    @Test
    void loginWithOidcGivesUpWhenTheBrowserNeverComesBack() throws Exception {
        server = startServer("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, "{\"authorizationUrl\":\"https://idp.example.com/authorize\",\"state\":\"s\"}");
        });

        EuclidEam access = EuclidEam.forServer(baseUrl()).browser(url -> { }).callbackTimeout(Duration.ofMillis(200));

        assertThrows(HttpTimeoutException.class, access::loginWithOidc);
    }

    // A stray request - a browser asking for the favicon - must not consume the login.
    @Test
    void theCallbackListenerIgnoresRequestsThatAreNotTheCallback() throws Exception {
        server = startServer("/", exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            JsonNode body = new ObjectMapper().readTree(exchange.getRequestBody().readAllBytes());
            if ("oidc-authorize".equals(action)) {
                sendResponse(exchange, 200, "{\"authorizationUrl\":\"" + body.path("redirectUri").asText()
                        + "?code=c&state=s\",\"state\":\"s\"}");
            } else {
                sendResponse(exchange, 200, LOGIN_RESPONSE);
            }
        });

        EuclidSession session = EuclidEam.forServer(baseUrl()).browser(url -> {
            String favicon = url.substring(0, url.indexOf("/callback")) + "/favicon.ico";
            assertEquals(404, fetch(HttpRequest.newBuilder(URI.create(favicon)).GET().build()));
            visit(url);
        }).loginWithOidc();

        assertEquals("fed-token", session.token());
    }

    // The SAML leg ends differently: EAM posts the finished session to the listener in a form, so
    // there is nothing left to redeem.
    @Test
    void loginWithSamlTakesTheSessionEuclidPostsToTheListener() throws Exception {
        AtomicReference<String> returnTo = new AtomicReference<>();
        server = startServer("/", exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            JsonNode body = new ObjectMapper().readTree(exchange.getRequestBody().readAllBytes());
            if ("saml-authorize".equals(action)) {
                returnTo.set(body.path("returnTo").asText());
                sendResponse(exchange, 200, "{\"authenticationUrl\":\"https://idp.example.com/sso?SAMLRequest=x\","
                        + "\"relayState\":\"rs\"}");
            } else {
                sendResponse(exchange, 500, "{\"error\":\"unexpected action " + action + "\"}");
            }
        });

        EuclidSession session = EuclidEam.forServer(baseUrl()).browser(url -> {
            // What EAM's hand-off page submits: the login response, base64url-encoded, unpadded.
            String encoded = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(LOGIN_RESPONSE.getBytes(StandardCharsets.UTF_8));
            fetch(HttpRequest.newBuilder(URI.create(returnTo.get()))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("session=" + encoded)).build());
        }).loginWithSaml();

        assertTrue(returnTo.get().startsWith("http://127.0.0.1:"));
        assertEquals("fed-token", session.token());
        assertEquals("863459426936", session.accountId());
        assertTrue(Files.readString(fakeHome.resolve(".euclid/credentials")).contains("fed-token"));
    }

    @Test
    void loginWithSamlAssertionHandsTheAssertionToTheConsumerService() throws Exception {
        AtomicReference<String> action = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        server = startServer("/", exchange -> {
            action.set(exchange.getRequestHeaders().getFirst("x-euclid-action"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendResponse(exchange, 200, LOGIN_RESPONSE);
        });

        EuclidSession session = EuclidEam.forServer(baseUrl())
                .loginWithSamlAssertion(SamlAcsRequest.builder().samlResponse("PHNhbWw+").build());

        assertEquals("saml-acs", action.get());
        assertEquals("{\"samlResponse\":\"PHNhbWw+\",\"relayState\":\"\"}", body.get());
        assertEquals("fed-token", session.token());
    }

    @Test
    void loginWithSamlAssertionSurfacesARefusedAssertion() throws Exception {
        server = startServer("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 401, "{\"error\":\"Unsolicited logins are not enabled\"}");
        });

        EuclidEam access = EuclidEam.forServer(baseUrl());

        EuclidAuthenticationException exception = assertThrows(EuclidAuthenticationException.class,
                () -> access.loginWithSamlAssertion(SamlAcsRequest.builder().samlResponse("x").build()));
        assertEquals(401, exception.statusCode());
    }

    // Going through the browser again within the hour is exactly what the cache is for.
    @Test
    void federatedLoginReusesAStillValidCachedSession() throws Exception {
        String token = validJwt();
        server = startServer("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, "{\"token\":\"" + token + "\",\"metadata\":{\"region\":\"eu-central-1\","
                    + "\"accountId\":\"863459426936\",\"user\":\"jens\"}}");
        });
        EuclidEam.forServer(baseUrl()).credentials("jens", "s3cret").login();

        EuclidSession session = EuclidEam.forServer(baseUrl())
                .browser(url -> { throw new AssertionError("the browser must not be needed"); })
                .loginWithOidc();

        assertEquals(token, session.token());
    }

    // ── Account selection ────────────────────────────────────────────────

    @Test
    void loginWithAccountWorksInTheAccountNamedByName() throws Exception {
        server = startServer("/", exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            exchange.getRequestBody().readAllBytes();
            if ("login".equals(action)) {
                sendResponse(exchange, 200, LOGIN_RESPONSE);
            } else if ("list-my-accounts".equals(action)) {
                sendResponse(exchange, 200, "{\"accounts\":[{\"accountId\":\"863459426936\",\"name\":\"home\"},"
                        + "{\"accountId\":\"111122223333\",\"name\":\"staging\"}],\"total\":2,"
                        + "\"homeAccountId\":\"863459426936\"}");
            } else {
                sendResponse(exchange, 500, "{\"error\":\"unexpected action " + action + "\"}");
            }
        });

        EuclidSession session = EuclidEam.forServer(baseUrl()).credentials("carol", "pw").account("staging").login();

        assertEquals("111122223333", session.accountId());
        JsonNode stored = new ObjectMapper().readTree(Files.readString(fakeHome.resolve(".euclid/credentials")));
        assertEquals("111122223333", stored.path("accountId").asText(), "the CLI must pick up the chosen account");
    }

    // The credentials were right, so the session is kept - in the home account - rather than making
    // the next attempt a second login for a typo in an account name.
    @Test
    void loginWithAnAccountTheUserMayNotWorkInKeepsTheHomeSession() throws Exception {
        server = startServer("/", exchange -> {
            String action = exchange.getRequestHeaders().getFirst("x-euclid-action");
            exchange.getRequestBody().readAllBytes();
            if ("login".equals(action)) {
                sendResponse(exchange, 200, LOGIN_RESPONSE);
            } else {
                sendResponse(exchange, 200, "{\"accounts\":[{\"accountId\":\"863459426936\",\"name\":\"home\"}],"
                        + "\"total\":1,\"homeAccountId\":\"863459426936\"}");
            }
        });

        EuclidEam access = EuclidEam.forServer(baseUrl()).credentials("carol", "pw").account("elsewhere");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, access::login);
        assertTrue(exception.getMessage().contains("863459426936 (home)"), exception.getMessage());
        JsonNode stored = new ObjectMapper().readTree(Files.readString(fakeHome.resolve(".euclid/credentials")));
        assertEquals("fed-token", stored.path("token").asText());
        assertEquals("863459426936", stored.path("accountId").asText());
    }

    // ── Refreshing ───────────────────────────────────────────────────────

    // What the session chose for itself - the account it works in, its namespace - is not the
    // server's to undo, though the server's answer names the home account.
    @Test
    void refreshSessionRenewsTheTokenAndKeepsWhatTheSessionChose() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> action = new AtomicReference<>();
        server = startServer("/", exchange -> {
            action.set(exchange.getRequestHeaders().getFirst("x-euclid-action"));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, LOGIN_RESPONSE);
        });
        EuclidSession session = new EuclidSession("old-token", "carol", "111122223333", "eu-central-1", "AKIAFED",
                "s3cr3t", false, "{}", baseUrl(), null, "prod");

        EuclidSession refreshed = session.refreshSession();

        assertEquals("refresh-session", action.get());
        assertEquals("Bearer old-token", authorization.get(), "a refresh is authenticated by the token it renews");
        assertEquals("fed-token", refreshed.token());
        assertEquals("111122223333", refreshed.accountId());
        assertEquals("prod", refreshed.nameSpace());
        assertEquals("AKIAFED", refreshed.accessKeyId());
    }

    @Test
    void refreshSessionUpdatesTheCacheOnlyWhenItHoldsThisSession() throws Exception {
        server = startServer("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 200, LOGIN_RESPONSE);
        });
        Path credentials = fakeHome.resolve(".euclid/credentials");
        Files.createDirectories(credentials.getParent());

        Files.writeString(credentials, "{\"token\":\"theirs\",\"userId\":\"dave\",\"baseUrl\":\"" + baseUrl() + "\"}");
        new EuclidSession("old-token", "carol", "863459426936", "eu-central-1", null, null, false, "{}",
                baseUrl(), null, null).refreshSession();
        assertTrue(Files.readString(credentials).contains("theirs"), "somebody else's cached session is left alone");

        Files.writeString(credentials, "{\"token\":\"old-token\",\"userId\":\"carol\",\"baseUrl\":\"" + baseUrl() + "\"}");
        new EuclidSession("old-token", "carol", "863459426936", "eu-central-1", null, null, false, "{}",
                baseUrl(), null, null).refreshSession();
        assertEquals("fed-token", new ObjectMapper().readTree(Files.readString(credentials)).path("token").asText());
    }

    @Test
    void refreshSessionSurfacesAnExpiredToken() throws Exception {
        server = startServer("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            sendResponse(exchange, 401, "{\"error\":\"Bearer token expired\"}");
        });
        EuclidSession session = new EuclidSession("old-token", "carol", "863459426936", "eu-central-1", null, null,
                false, "{}", baseUrl(), null, null);

        EuclidServiceException exception = assertThrows(EuclidServiceException.class, session::refreshSession);
        assertEquals("refresh-session", exception.action());
        assertEquals(401, exception.statusCode());
    }

    /**
     * A login response as EAM answers every way in with it.
     */
    private static final String LOGIN_RESPONSE = "{\"token\":\"fed-token\",\"accessKeyId\":\"AKIAFED\","
            + "\"secretAccessKey\":\"s3cr3t\",\"createdAt\":\"2026-01-01T00:00:00Z\",\"isAdmin\":false,"
            + "\"metadata\":{\"region\":\"eu-central-1\",\"accountId\":\"863459426936\",\"user\":\"carol\"}}";

    private static String validJwt() {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String payload = "{\"sub\":\"jens\",\"exp\":" + (Instant.now().getEpochSecond() + 3600) + "}";
        return encoder.encodeToString("{\"alg\":\"HS256\"}".getBytes(StandardCharsets.UTF_8)) + "."
                + encoder.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + ".sig";
    }

    // Plays the browser: follows the URL to wherever it points, which in these tests is the
    // listener's own callback.
    private static void visit(String url) {
        fetch(HttpRequest.newBuilder(URI.create(url)).GET().build());
    }

    private static int fetch(HttpRequest request) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private HttpServer startServer(String path, HttpHandler handler) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        httpServer.createContext(path, handler);
        httpServer.start();
        return httpServer;
    }

    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    private static void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
