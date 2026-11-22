package de.jensvogt.euclid.module.eam;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Catches the one request a browser brings back at the end of a federated login, on a port this
 * process owns for the length of that login - the equivalent of euclid-cli's
 * {@code CallbackListener}.
 * <p>
 * A loopback listener rather than a URL on the euclid gateway because a program logging somebody in
 * from their own machine is a public client: it has no secret to identify itself with, so what the
 * provider hands back has to come back to the machine that asked for it, and {@code 127.0.0.1} is
 * the one address nothing else can reach. The port is whatever the kernel hands out - providers
 * expect a native client to register a wildcard loopback URI for exactly this reason.
 * <p>
 * Two things can arrive, and both are taken: an OpenID Connect provider redirects back with
 * {@code code} and {@code state} in the query, and euclid's SAML assertion consumer posts the
 * finished session as a form field named {@code session} - base64url-encoded JSON, in a body
 * rather than a URL so the token never sits in a browser history entry. Either may instead carry
 * the provider's {@code error}. Anything else - a browser asking for a favicon, most often - is
 * answered with a 404 and does not end the wait, so one stray request cannot consume the login.
 */
final class LoopbackCallbackListener implements AutoCloseable {

    /**
     * The page the browser is left on once the callback has been taken. Says where to go next,
     * because the person reading it is about to wonder why a terminal or an application asked them
     * to sign in.
     */
    private static final String CALLBACK_PAGE =
            "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>euclid</title></head>"
                    + "<body style=\"font-family:sans-serif;padding:3rem\"><h1>Signed in</h1>"
                    + "<p>You can close this tab and go back to the application.</p></body></html>";

    /**
     * What the browser brought back.
     *
     * @param code    the OIDC authorization code, or {@code null}
     * @param state   the OIDC state, exactly as it arrived, or {@code null}
     * @param session the SAML hand-off's decoded login response JSON, or {@code null}
     * @param error   the provider's refusal (with its description, when it gave one), or a reason
     *                the hand-off could not be read; {@code null} when there was none
     */
    record Callback(String code, String state, String session, String error) {
    }

    /**
     * The embedded server, bound to {@code 127.0.0.1} on a kernel-chosen port.
     */
    private final HttpServer server;

    /**
     * Completed by the first request that carries a callback.
     */
    private final CompletableFuture<Callback> result = new CompletableFuture<>();

    /**
     * Binds the listener and starts accepting. Bound before anything is asked of the server,
     * because the redirect URI has to name the port the listener ended up on.
     *
     * @throws IOException if no loopback port could be bound
     */
    LoopbackCallbackListener() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    /**
     * The URL to hand out as the place to come back to.
     *
     * @return {@code http://127.0.0.1:<port>/callback}
     */
    String callbackUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/callback";
    }

    /**
     * Waits for the callback.
     *
     * @param timeout how long to wait
     * @return what arrived, or {@code null} if nothing did in time
     * @throws InterruptedException if the waiting thread is interrupted
     */
    Callback await(Duration timeout) throws InterruptedException {
        try {
            return result.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            return null;
        } catch (ExecutionException e) {
            // Never completed exceptionally - handle() only ever completes it with a value.
            return new Callback(null, null, null, String.valueOf(e.getCause()));
        }
    }

    /**
     * Stops the listener; the port is released at once rather than after a grace period, since
     * whatever the browser had to say has been said.
     */
    @Override
    public void close() {
        server.stop(0);
    }

    /**
     * Answers one request from the browser.
     *
     * @param exchange the request
     * @throws IOException if the answer cannot be written
     */
    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            Map<String, String> parameters = parse(exchange.getRequestURI().getRawQuery());

            // The SAML hand-off is a form post from the page euclid answered the assertion with.
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                parameters.putAll(parse(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            }

            boolean hasCode = parameters.containsKey("code");
            boolean hasSession = parameters.containsKey("session");
            boolean hasError = parameters.containsKey("error");
            if (!hasCode && !hasSession && !hasError) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            String session = null;
            String error = null;
            if (hasSession) {
                try {
                    session = new String(Base64.getUrlDecoder().decode(parameters.get("session")), StandardCharsets.UTF_8);
                } catch (IllegalArgumentException e) {
                    error = "the session euclid sent back could not be read: " + e.getMessage();
                }
            }
            if (hasError) {
                error = parameters.get("error");
                String description = parameters.get("error_description");
                if (description != null) {
                    error += " (" + description + ")";
                }
            }

            byte[] page = CALLBACK_PAGE.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.getResponseHeaders().add("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, page.length);
            try (OutputStream body = exchange.getResponseBody()) {
                body.write(page);
            }

            result.complete(new Callback(parameters.get("code"), parameters.get("state"), session, error));
        }
    }

    /**
     * Parses a query string or form body - the two are the same encoding.
     *
     * @param encoded the raw, still percent-encoded text, or {@code null}
     * @return the decoded parameters; the first occurrence of a name wins
     */
    private static Map<String, String> parse(String encoded) {
        Map<String, String> parameters = new HashMap<>();
        if (encoded == null || encoded.isEmpty()) {
            return parameters;
        }
        for (String pair : encoded.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int equals = pair.indexOf('=');
            String name = URLDecoder.decode(equals < 0 ? pair : pair.substring(0, equals), StandardCharsets.UTF_8);
            String value = equals < 0 ? "" : URLDecoder.decode(pair.substring(equals + 1), StandardCharsets.UTF_8);
            parameters.putIfAbsent(name, value);
        }
        return parameters;
    }
}
