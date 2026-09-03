package de.jensvogt.euclid.http;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Thin wrapper around {@link java.net.http.HttpClient} for issuing common
 * HTTP requests (GET, POST, PUT, PATCH, DELETE) against a server.
 * <p>
 * TLS is handled transparently for "https://" URLs via the JVM's default trust store. An
 * optional PEM CA certificate can additionally be trusted (alongside, not instead of, the
 * system trust store) - mirroring euclid-cli's {@code --ca-cert} option, used to reach an
 * euclid server presenting a self-signed development certificate.
 */
public class EuclidHttpClient {

    /**
     * Represents the underlying HTTP client used for sending requests and receiving responses.
     * This client is configured during the construction of the {@code EuclidHttpClient} and
     * is responsible for handling the network layer.
     * <p>
     * The configuration of this client can include settings such as request timeouts,
     * SSL context for trusted certificates, and connection options.
     * <p>
     * The {@code client} instance is immutable and provides methods to create and send
     * HTTP requests using various supported HTTP methods like GET, POST, PUT, DELETE, etc.
     */
    private final HttpClient client;

    /**
     * Represents the timeout duration for individual HTTP requests in the {@code EuclidHttpClient}.
     * This value defines how long the client will wait for a response before timing out.
     * <p>
     * The timeout is configurable through the client constructors, allowing users to specify
     * a custom value or rely on sensible defaults.
     */
    private final Duration requestTimeout;

    /**
     * Rebuilds the authentication headers for one {@code (action, body)} pair, or {@code null} if
     * this client was not given one.
     *
     * <p>Set by each module client to its own header builder, and used for one purpose: when the
     * server rejects a request because the credentials it carried had expired, the headers are
     * built again and the request is sent once more. See
     * {@link #post(String, String, String, String, Map)}.
     */
    private volatile BiFunction<String, String, Map<String, String>> headerFactory;

    /**
     * Constructs a new {@code EuclidHttpClient} instance with a default request timeout
     * of 10 seconds and no additional PEM CA certificate configured for trust.
     * <p>
     * This constructor sets up the HTTP client to trust only the system's default
     * trust store.
     */
    public EuclidHttpClient() {
        this(Duration.ofSeconds(10), null);
    }

    /**
     * Constructs a new {@code EuclidHttpClient} instance with the default request timeout of 10 seconds
     * and an additional PEM CA certificate to trust alongside the system trust store.
     *
     * @param caCertPath path to an additional PEM CA certificate to trust alongside the system
     *                   trust store, or {@code null}/blank to trust only the system store.
     */
    public EuclidHttpClient(String caCertPath) {
        this(Duration.ofSeconds(10), caCertPath);
    }

    /**
     * Constructs a new instance of {@code EuclidHttpClient} with the specified per-request timeout.
     *
     * @param requestTimeout the duration to wait before timing out individual HTTP requests.
     */
    public EuclidHttpClient(Duration requestTimeout) {
        this(requestTimeout, null);
    }

    /**
     * Constructs an instance of EuclidHttpClient with the specified request timeout and optional CA certificate path.
     *
     * @param requestTimeout the duration to specify the request timeout; must not be null.
     * @param caCertPath the file path to the CA certificate; can be null or blank if no custom CA certificate is required.
     */
    public EuclidHttpClient(Duration requestTimeout, String caCertPath) {
        this.requestTimeout = requestTimeout;
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10));
        if (caCertPath != null && !caCertPath.isBlank()) {
            builder.sslContext(buildSslContext(caCertPath));
        }
        this.client = builder.build();
    }

    /**
     * Registers how to build the headers for an action, enabling the retry described on
     * {@link #post(String, String, String, String, Map)}.
     *
     * <p>Without one, a request whose credentials expired between being built and being read fails
     * like any other error, which for a long-lived application is a business operation lost to a
     * token that a second attempt would have carried correctly.
     *
     * @param factory builds the headers for a {@code (action, body)} pair, exactly as the caller
     *                built the ones it passed in
     * @return this client, for chaining onto the constructor
     */
    public EuclidHttpClient headerFactory(BiFunction<String, String, Map<String, String>> factory) {
        this.headerFactory = factory;
        return this;
    }

    /**
     * Sends an HTTP GET request to the specified URL.
     *
     * @param url the URL to send the GET request to.
     * @return the HTTP response as a {@code HttpResponse<String>} instance.
     * @throws IOException          if an I/O error occurs when sending or receiving.
     * @throws InterruptedException if the operation is interrupted.
     */
    public HttpResponse<String> get(String url) throws IOException, InterruptedException {
        return get(url, Map.of());
    }

    /**
     * Sends an HTTP GET request to the specified URL with additional headers.
     *
     * @param url     the URL to send the GET request to.
     * @param headers a map of headers to include in the GET request.
     * @return the HTTP response as a {@code HttpResponse<String>} instance.
     * @throws IOException          if an I/O error occurs when sending or receiving.
     * @throws InterruptedException if the operation is interrupted.
     */
    public HttpResponse<String> get(String url, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest request = newRequestBuilder(url, headers)
                .GET()
                .build();
        return send(request);
    }

    /**
     * Sends an HTTP GET request to the specified URL with the given target, action, and default headers.
     *
     * @param url    the URL to send the GET request to.
     * @param target the target path or identifier, used to define the specific resource being requested.
     * @param action the action or operation associated with the request.
     * @return the HTTP response as a {@code HttpResponse<String>} instance.
     * @throws IOException          if an I/O error occurs when sending or receiving.
     * @throws InterruptedException if the operation is interrupted.
     */
    public HttpResponse<String> get(String url, String target, String action) throws IOException, InterruptedException {
        return get(url, target, action, Map.of());
    }

    /**
     * Sends an HTTP GET request to the specified URL with the provided target, action, and headers.
     *
     * @param url     the base URL to send the GET request to.
     * @param target  the target path or identifier used to define the specific resource being requested.
     * @param action  the action or operation associated with the request.
     * @param headers a map of additional headers to include in the GET request.
     * @return the HTTP response as a {@code HttpResponse<String>} instance.
     * @throws IOException          if an I/O error occurs when sending or receiving.
     * @throws InterruptedException if the operation is interrupted.
     */
    public HttpResponse<String> get(String url, String target, String action, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest request = newRequestBuilder(url, target, action, headers)
                .GET()
                .build();
        return send(request);
    }

    /**
     * Sends an HTTP POST request to the specified URL with a given body and additional headers.
     *
     * @param url  the URL to send the POST request to.
     * @param body the request body to be sent in the POST request.
     * @return the HTTP response as a {@code HttpResponse<String>} instance.
     * @throws IOException          if an I/O error occurs when sending or receiving.
     * @throws InterruptedException if the operation is interrupted.
     */
    public HttpResponse<String> post(String url, String body) throws IOException, InterruptedException {
        return post(url, body, Map.of());
    }

    /**
     * Sends an HTTP POST request to the specified URL with the given body and headers.
     *
     * @param url     the target URL for the POST request
     * @param body    the request body to be sent with the POST request
     * @param headers a map of HTTP headers to include in the request
     * @return an HttpResponse containing the response from the server
     * @throws IOException          if an I/O error occurs when sending or receiving
     * @throws InterruptedException if the operation is interrupted
     */
    public HttpResponse<String> post(String url, String body, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest request = newRequestBuilder(url, headers)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return send(request);
    }

    /**
     * Sends an HTTP POST request to the specified URL with the given parameters.
     *
     * @param url    the URL to send the POST request to
     * @param body   the body content of the POST request
     * @param target the target endpoint or resource identifier
     * @param action the action to be performed by the request
     * @return an HttpResponse containing the response body as a String
     * @throws IOException          if an I/O error occurs when sending or receiving
     * @throws InterruptedException if the operation is interrupted
     */
    public HttpResponse<String> post(String url, String body, String target, String action) throws IOException, InterruptedException {
        return post(url, body, target, action, Map.of());
    }

    /**
     * Sends an HTTP POST request to the specified URL with the provided body, target, action, and headers.
     *
     * @param url     the URL to which the POST request is sent
     * @param body    the body content of the POST request
     * @param target  the target identifier, typically used for routing or additional request context
     * @param action  the action to be performed, often used for specifying the operation type
     * @param headers a map of headers to include in the POST request
     * @return the HTTP response as a string
     * @throws IOException          if an I/O error occurs during the request
     * @throws InterruptedException if the operation is interrupted while waiting for the response
     */
    public HttpResponse<String> post(String url, String body, String target, String action, Map<String, String> headers) throws IOException, InterruptedException {
        HttpResponse<String> response = send(newRequestBuilder(url, target, action, headers)
                                                     .POST(HttpRequest.BodyPublishers.ofString(body))
                                                     .build());

        Map<String, String> refreshed = refreshedHeaders(response, action, body, headers);
        if (refreshed == null) {
            return response;
        }

        return send(newRequestBuilder(url, target, action, refreshed)
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .build());
    }

    /**
     * Decides whether a failed request is worth sending a second time with fresh credentials, and
     * builds the headers for that second attempt.
     *
     * <p>An application that runs for days holds credentials that do not: a bearer token from a
     * credentials file the manager rewrites, or a signature that is only valid around the moment it
     * was made. Either can go stale between the header being built and the server reading it, and
     * the request that carries it fails - not because anything was wrong with it, but because it
     * was a moment too late. Building the headers again costs one round trip and turns that into a
     * success.
     *
     * <p>Deliberately narrow, so it never turns a real rejection into two:
     * <ul>
     *   <li>only 401, and only when the server said the credentials had expired - a wrong password
     *       or a missing permission is answered once, as before;</li>
     *   <li>only when the new headers actually differ. A bearer token read from a file nobody has
     *       refreshed comes back identical, and repeating the request with it would fail exactly as
     *       it just did.</li>
     * </ul>
     *
     * @param response the response to the first attempt
     * @param action   the action the request carried
     * @param body     the body the request carried, which a signature covers
     * @param headers  the headers the first attempt used, kept so that per-request headers the
     *                 factory knows nothing about survive into the retry
     * @return the headers to retry with, or {@code null} if the request should not be retried
     */
    private Map<String, String> refreshedHeaders(HttpResponse<String> response, String action, String body,
                                                 Map<String, String> headers) {
        BiFunction<String, String, Map<String, String>> factory = headerFactory;
        if (factory == null || response.statusCode() != 401) {
            return null;
        }

        String reason = response.body();
        if (reason == null || !reason.toLowerCase().contains("expired")) {
            return null;
        }

        Map<String, String> refreshed = new LinkedHashMap<>(headers);
        refreshed.putAll(factory.apply(action, body));
        return refreshed.equals(headers) ? null : refreshed;
    }

    /**
     * Sends a POST request with a raw binary body instead of JSON, mirroring euclid-cli's
     * {@code HttpClient::PostBinary()}: used by ESM's upload-part, where request-specific metadata
     * (upload ID, part number) travels in {@code headers} instead of a JSON field since the body
     * is opaque bytes.
     *
     * @param url     the URL to send the POST request to
     * @param data    the binary data to be sent in the request body
     * @param target  the target endpoint or resource identifier
     * @param action  the action to be performed by the request
     * @param headers a map of headers to include in the POST request
     * @return the HTTP response as a string
     * @throws IOException          if an I/O error occurs during the request
     * @throws InterruptedException if the operation is interrupted while waiting for the response
     */
    public HttpResponse<String> postBinary(String url, byte[] data, String target, String action, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest request = newRequestBuilder(url, target, action, headers)
                .POST(HttpRequest.BodyPublishers.ofByteArray(data))
                .build();
        return send(request);
    }

    /**
     * Sends a POST request with a raw binary body and reads the response as raw bytes, mirroring
     * euclid-cli's {@code HttpClient::PostBinaryForBinary()}: used by EKM's encrypt and decrypt,
     * where opaque bytes go out and opaque bytes come back, and the key the transform uses travels
     * in {@code headers}.
     * <p>
     * An error response is bytes too, holding the server's JSON error body - the caller decodes it
     * once it has seen a non-2xx status.
     *
     * @param url     the URL to send the POST request to
     * @param data    the binary data to be sent in the request body
     * @param target  the target endpoint or resource identifier
     * @param action  the action to be performed by the request
     * @param headers a map of headers to include in the POST request
     * @return the HTTP response with the body as a byte array
     * @throws IOException          if an I/O error occurs during the request
     * @throws InterruptedException if the operation is interrupted while waiting for the response
     */
    public HttpResponse<byte[]> postBinaryForBinary(String url, byte[] data, String target, String action,
                                                    Map<String, String> headers)
            throws IOException, InterruptedException {
        HttpRequest request = newRequestBuilder(url, target, action, headers)
                .POST(HttpRequest.BodyPublishers.ofByteArray(data))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    /**
     * Sends a POST request with no body and reads the response as raw bytes, mirroring euclid-cli's
     * {@code HttpClient::PostForBinary()}: used by ESM's get-object and download-part, where the
     * request is fully described by {@code headers} and the response is object bytes rather than
     * JSON.
     * <p>
     * An error response is bytes too, holding the server's JSON error body - the caller decodes it
     * once it has seen a non-2xx status.
     *
     * @param url     the URL to send the POST request to
     * @param target  the target endpoint or resource identifier
     * @param action  the action to be performed by the request
     * @param headers a map of headers to include in the POST request
     * @return the HTTP response with the body as a byte array
     * @throws IOException          if an I/O error occurs during the request
     * @throws InterruptedException if the operation is interrupted while waiting for the response
     */
    public HttpResponse<byte[]> postForBinary(String url, String target, String action, Map<String, String> headers)
            throws IOException, InterruptedException {
        HttpRequest request = newRequestBuilder(url, target, action, headers)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    /**
     * Sends an HTTP PUT request to the specified URL with the given request body
     * and optional headers.
     *
     * @param url  the target URL to send the PUT request to
     * @param body the request body to be sent with the PUT request
     * @return the HTTP response received from the server, containing the status
     * code, headers, and response body
     * @throws IOException          if an I/O error occurs while sending or receiving the request
     * @throws InterruptedException if the operation is interrupted while waiting for the response
     */
    public HttpResponse<String> put(String url, String body) throws IOException, InterruptedException {
        return put(url, body, Map.of());
    }

    /**
     * Sends an HTTP PUT request to the specified URL with the given body content and headers.
     *
     * @param url     the URL to send the PUT request to
     * @param body    the content to be included in the body of the PUT request
     * @param headers a map of header keys and values to include in the request
     * @return an HttpResponse object containing the response from the server
     * @throws IOException          if an I/O error occurs when sending or receiving the request
     * @throws InterruptedException if the operation is interrupted
     */
    public HttpResponse<String> put(String url, String body, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest request = newRequestBuilder(url, headers)
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return send(request);
    }

    /**
     * Sends an HTTP PUT request to the specified URL with the given body and additional parameters.
     *
     * @param url    the target URL to which the request is sent
     * @param body   the content to be included in the request body
     * @param target a string representing the target resource or endpoint
     * @param action a string specifying the action or operation to be associated with the request
     * @return an HttpResponse object containing the response as a string
     * @throws IOException          if an I/O error occurs when sending or receiving
     * @throws InterruptedException if the operation is interrupted
     */
    public HttpResponse<String> put(String url, String body, String target, String action) throws IOException, InterruptedException {
        return put(url, body, target, action, Map.of());
    }

    /**
     * Sends an HTTP PUT request to the specified URL with the provided body, target, action, and headers.
     *
     * @param url     the URL to which the PUT request is sent
     * @param body    the body content to be included in the PUT request
     * @param target  the target endpoint for the request
     * @param action  the action to be performed in the request
     * @param headers a map of headers to include in the request
     * @return the HTTP response received after sending the PUT request
     * @throws IOException          if an I/O error occurs when sending or receiving
     * @throws InterruptedException if the operation is interrupted while waiting for the response
     */
    public HttpResponse<String> put(String url, String body, String target, String action, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest request = newRequestBuilder(url, target, action, headers)
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return send(request);
    }

    /**
     * Sends an HTTP PATCH request to the specified URL with the provided request body and default headers.
     *
     * @param url  the URL to send the PATCH request to
     * @param body the request body to include in the PATCH request
     * @return the HTTP response containing the response body as a string
     * @throws IOException          if an I/O error occurs when sending or receiving the request
     * @throws InterruptedException if the operation is interrupted while waiting for a response
     */
    public HttpResponse<String> patch(String url, String body) throws IOException, InterruptedException {
        return patch(url, body, Map.of());
    }

    /**
     * Sends an HTTP PATCH request to the specified URL with the provided request body and headers.
     *
     * @param url     the URL to which the PATCH request is sent
     * @param body    the request body to include in the PATCH request
     * @param headers a map of headers to include in the PATCH request
     * @return the HTTP response received after executing the PATCH request
     * @throws IOException          if an I/O error occurs when sending or receiving
     * @throws InterruptedException if the operation is interrupted
     */
    public HttpResponse<String> patch(String url, String body, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest request = newRequestBuilder(url, headers)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build();
        return send(request);
    }

    /**
     * Sends an HTTP PATCH request to the specified URL with the provided body, target, and action.
     * Additional headers can be handled by the overloaded method called within.
     *
     * @param url    the URL to which the PATCH request is sent
     * @param body   the request payload to be included in the PATCH request
     * @param target the target resource or identifier for the operation
     * @param action the action describing the purpose of the HTTP request
     * @return an HttpResponse containing the response as a String
     * @throws IOException          if an I/O error occurs while sending or receiving the request
     * @throws InterruptedException if the operation is interrupted during execution
     */
    public HttpResponse<String> patch(String url, String body, String target, String action) throws IOException, InterruptedException {
        return patch(url, body, target, action, Map.of());
    }

    /**
     * Sends an HTTP PATCH request to the specified URL with the provided body, target, action, and headers.
     *
     * @param url     the URL to which the HTTP PATCH request is sent
     * @param body    the content to be included in the body of the request
     * @param target  the specific resource or endpoint targeted by the request
     * @param action  the action or operation to be performed
     * @param headers a map containing the headers to be included in the request
     * @return the HTTP response containing the response body as a string
     * @throws IOException          if an I/O error occurs when sending or receiving
     * @throws InterruptedException if the operation is interrupted
     */
    public HttpResponse<String> patch(String url, String body, String target, String action, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest request = newRequestBuilder(url, target, action, headers)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build();
        return send(request);
    }

    /**
     * Sends an HTTP DELETE request to the specified URL with optional headers
     * and returns the HTTP response.
     *
     * @param url the URL to send the DELETE request to
     * @return the HTTP response as an instance of HttpResponse&lt;String&gt;
     * @throws IOException          if an I/O error occurs when sending or receiving
     * @throws InterruptedException if the operation is interrupted
     */
    public HttpResponse<String> delete(String url) throws IOException, InterruptedException {
        return delete(url, Map.of());
    }

    /**
     * Sends an HTTP DELETE request to the specified URL with the provided headers.
     *
     * @param url     the URL to which the DELETE request is to be sent
     * @param headers a map containing the headers to include in the request
     * @return an HttpResponse containing the response body as a string
     * @throws IOException          if an I/O error occurs when sending or receiving
     * @throws InterruptedException if the operation is interrupted
     */
    public HttpResponse<String> delete(String url, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest request = newRequestBuilder(url, headers)
                .DELETE()
                .build();
        return send(request);
    }

    /**
     * Sends an HTTP DELETE request to the specified URL with the provided target and action parameters.
     * An additional empty headers map is used for this request.
     *
     * @param url    the URL to which the DELETE request is sent
     * @param target the target endpoint or resource for the DELETE operation
     * @param action the specific action associated with the DELETE request
     * @return an {@code HttpResponse<String>} representing the response received from the server
     * @throws IOException          if an I/O error occurs while sending or receiving
     * @throws InterruptedException if the operation is interrupted
     */
    public HttpResponse<String> delete(String url, String target, String action) throws IOException, InterruptedException {
        return delete(url, target, action, Map.of());
    }

    /**
     * Sends an HTTP DELETE request to the specified URL with the given target and action,
     * and includes the provided headers.
     *
     * @param url     the base URL to send the request to
     * @param target  the specific endpoint or resource to target within the base URL
     * @param action  an action or operation identifier related to the DELETE request
     * @param headers a map of header key-value pairs to include in the request
     * @return the HTTP response as a String wrapped in {@code HttpResponse}
     * @throws IOException          if an I/O error occurs when sending or receiving
     * @throws InterruptedException if the operation is interrupted
     */
    public HttpResponse<String> delete(String url, String target, String action, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest request = newRequestBuilder(url, target, action, headers)
                .DELETE()
                .build();
        return send(request);
    }

    /**
     * The underlying {@link HttpClient}, already configured with this instance's TLS trust
     * settings (system store plus, if configured, the additional PEM CA certificate). Exposed so
     * callers that need capabilities beyond simple request/response - e.g. opening a
     * {@link java.net.http.WebSocket} - can reuse the same TLS configuration instead of
     * duplicating it.
     *
     * @return the underlying, pre-configured {@link HttpClient}
     */
    public HttpClient httpClient() {
        return client;
    }

    /**
     * Creates a new {@link HttpRequest.Builder} instance with the specified URL and headers.
     *
     * @param url     the URL for the HTTP request
     * @param headers a map of headers to be included in the request, where the key is the header name and the value is the header value
     * @return a pre-configured {@link HttpRequest.Builder} instance with the specified URL and headers applied
     */
    private HttpRequest.Builder newRequestBuilder(String url, Map<String, String> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(requestTimeout);
        headers.forEach(builder::header);
        return builder;
    }

    /**
     * Creates a new instance of {@link HttpRequest.Builder} with the specified URL, target, action, and headers.
     *
     * @param url     the URL for the request
     * @param target  the value to be set for the "x-euclid-target" header
     * @param action  the value to be set for the "x-euclid-action" header
     * @param headers a map of additional headers to be included in the request
     * @return a configured instance of {@link HttpRequest.Builder}
     */
    private HttpRequest.Builder newRequestBuilder(String url, String target, String action, Map<String, String> headers) {
        return newRequestBuilder(url, headers)
                .header("x-euclid-target", target)
                .header("x-euclid-action", action);
    }

    /**
     * Sends the given HTTP request and returns the response as a string.
     *
     * @param request the HTTP request to be sent
     * @return the HTTP response received from the request
     * @throws IOException          if an I/O error occurs when sending or receiving
     * @throws InterruptedException if the operation is interrupted
     */
    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Builds an SSLContext that trusts both the system-default certificate authorities (CAs)
     * and the additional CAs specified in the provided CA certificate file path.
     * This is equivalent to merging trust settings from the system's default CAs and
     * the provided CA file.
     * <p>
     * Trusts the system default CAs plus (if given) the CAs in caCertPath - equivalent to
     * OpenSSL's set_default_verify_paths() followed by load_verify_file(caCertPath) in
     * euclid-cli's HttpClient.cpp, so a certificate is accepted if either root set vouches for it.
     *
     * @param caCertPath the file path of the CA certificate to be loaded and trusted.
     *                   This file should contain one or more X.509 certificates in PEM format.
     * @return an SSLContext instance configured to trust the system-default CAs
     * and the additional CAs specified in the provided certificate file.
     * @throws IllegalStateException if there is an error loading the CA certificate file
     *                               or initializing the SSL context.
     */
    private static SSLContext buildSslContext(String caCertPath) {
        try {
            X509TrustManager systemTrustManager = defaultTrustManager(null);

            KeyStore customTrustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            customTrustStore.load(null, null);
            try (InputStream in = Files.newInputStream(Path.of(caCertPath))) {
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                int i = 0;
                for (Certificate certificate : certificateFactory.generateCertificates(in)) {
                    customTrustStore.setCertificateEntry("euclid-ca-" + i++, certificate);
                }
            }
            X509TrustManager customTrustManager = defaultTrustManager(customTrustStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new CompositeTrustManager(systemTrustManager, customTrustManager)}, null);
            return sslContext;
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("failed to load CA certificate from " + caCertPath, e);
        }
    }

    /**
     * Retrieves the default X509TrustManager instance from the platform's TrustManagerFactory
     * initialized with the provided KeyStore.
     *
     * @param trustStore the KeyStore to initialize the TrustManagerFactory.
     * @return an X509TrustManager instance if available from the TrustManagerFactory.
     * @throws GeneralSecurityException if an error occurs while initializing the TrustManagerFactory or if the KeyStore is invalid.
     * @throws IllegalStateException    if no X509TrustManager is available from the initialized TrustManagerFactory.
     */
    private static X509TrustManager defaultTrustManager(KeyStore trustStore) throws GeneralSecurityException {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
            if (trustManager instanceof X509TrustManager x509TrustManager) {
                return x509TrustManager;
            }
        }
        throw new IllegalStateException("no X509TrustManager available from the platform default TrustManagerFactory");
    }

    /**
     * Trusts a certificate chain if either delegate does, and offers the union of both as
     * accepted issuers.
     *
     * @param primary   The primary X509TrustManager used for trust decisions within the CompositeTrustManager.
     *                  This trust manager is consulted first when validating certificates.
     *                  If the primary trust manager does not trust the certificate, the decision falls back
     *                  to the secondary trust manager.
     * @param secondary The secondary X509TrustManager used as a fallback within the CompositeTrustManager.
     *                  This trust manager is consulted if the primary trust manager does not trust the certificate.
     */
    private record CompositeTrustManager(X509TrustManager primary, X509TrustManager secondary) implements X509TrustManager {

        /**
         * Constructs a CompositeTrustManager instance that delegates trust decisions
         * to two specified X509TrustManager implementations.
         * Trust decisions are passed to the primary trust manager first and fall back
         * to the secondary trust manager if the primary does not trust the certificate.
         *
         * @param primary   the primary X509TrustManager used for trust decisions.
         *                  This trust manager is consulted first.
         * @param secondary the secondary X509TrustManager used as a fallback
         *                  if the primary trust manager does not trust the certificate.
         */
        private CompositeTrustManager {
        }

        /**
         * Validates whether the provided client certificate chain is trusted for the specified authentication type.
         * This method first delegates the trust validation to the primary trust manager. If the primary trust manager
         * does not trust the certificate chain, the validation is deferred to the secondary trust manager.
         *
         * @param chain    the client certificate chain to be validated.
         *                 This is an array of X509Certificate objects where the first certificate in the chain
         *                 is the end-entity certificate and the subsequent certificates belong to the certificate hierarchy.
         * @param authType the type of authentication used. Typically, this is a string indicating the key exchange
         *                 algorithm (e.g., "RSA", "DHE", "ECDHE").
         * @throws CertificateException if neither the primary nor the secondary trust manager trusts the provided
         *                              client certificate chain.
         */
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            try {
                primary.checkClientTrusted(chain, authType);
            } catch (CertificateException e) {
                secondary.checkClientTrusted(chain, authType);
            }
        }

        /**
         * Validates whether the provided server certificate chain is trusted for the specified authentication type.
         * This method first delegates the trust validation to the primary trust manager. If the primary trust manager
         * does not trust the certificate chain, the validation is deferred to the secondary trust manager.
         *
         * @param chain    the server certificate chain to be validated.
         *                 This is an array of X509Certificate objects where the first certificate in the chain
         *                 is the end-entity certificate and the subsequent certificates belong to the certificate hierarchy.
         * @param authType the type of authentication used. Typically, this is a string indicating the key exchange
         *                 algorithm (e.g., "RSA", "DHE", "ECDHE").
         * @throws CertificateException if neither the primary nor the secondary trust manager trusts the provided
         *                              server certificate chain.
         */
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            try {
                primary.checkServerTrusted(chain, authType);
            } catch (CertificateException e) {
                secondary.checkServerTrusted(chain, authType);
            }
        }

        /**
         * Retrieves the list of certificate authorities trusted by this CompositeTrustManager.
         * This method aggregates the trusted certificate authorities from both the primary
         * and secondary X509TrustManager instances.
         *
         * @return an array of X509Certificate objects representing the trusted certificate authorities
         *         from both the primary and secondary trust managers. The returned array will contain
         *         the combined issuers from both sources.
         */
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            X509Certificate[] a = primary.getAcceptedIssuers();
            X509Certificate[] b = secondary.getAcceptedIssuers();
            X509Certificate[] combined = new X509Certificate[a.length + b.length];
            System.arraycopy(a, 0, combined, 0, a.length);
            System.arraycopy(b, 0, combined, a.length, b.length);
            return combined;
        }
    }
}
