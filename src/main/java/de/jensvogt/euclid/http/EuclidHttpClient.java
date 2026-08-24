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
import java.util.Map;

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

    private final HttpClient client;
    private final Duration requestTimeout;

    public EuclidHttpClient() {
        this(Duration.ofSeconds(10), null);
    }

    public EuclidHttpClient(String caCertPath) {
        this(Duration.ofSeconds(10), caCertPath);
    }

    public EuclidHttpClient(Duration requestTimeout) {
        this(requestTimeout, null);
    }

    /**
     * @param requestTimeout per-request timeout.
     * @param caCertPath     path to an additional PEM CA certificate to trust alongside the
     *                       system trust store, or {@code null}/blank to trust only the system
     *                       store.
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

    public HttpResponse<String> get(String url) throws IOException, InterruptedException {
        return get(url, Map.of());
    }

    public HttpResponse<String> get(String url, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest request = newRequestBuilder(url, headers)
                .GET()
                .build();
        return send(request);
    }

    public HttpResponse<String> get(String url, String target, String action) throws IOException, InterruptedException {
        return get(url, target, action, Map.of());
    }

    public HttpResponse<String> get(String url, String target, String action, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest request = newRequestBuilder(url, target, action, headers)
                .GET()
                .build();
        return send(request);
    }

    public HttpResponse<String> post(String url, String body) throws IOException, InterruptedException {
        return post(url, body, Map.of());
    }

    public HttpResponse<String> post(String url, String body, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest request = newRequestBuilder(url, headers)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return send(request);
    }

    public HttpResponse<String> post(String url, String body, String target, String action) throws IOException, InterruptedException {
        return post(url, body, target, action, Map.of());
    }

    public HttpResponse<String> post(String url, String body, String target, String action, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest request = newRequestBuilder(url, target, action, headers)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return send(request);
    }

    /**
     * Sends a POST request with a raw binary body instead of JSON, mirroring euclid-cli's
     * {@code HttpClient::PostBinary()}: used by ESM's upload-part, where request-specific metadata
     * (upload ID, part number) travels in {@code headers} instead of a JSON field since the body
     * is opaque bytes.
     */
    public HttpResponse<String> postBinary(String url, byte[] data, String target, String action, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest request = newRequestBuilder(url, target, action, headers)
                .POST(HttpRequest.BodyPublishers.ofByteArray(data))
                .build();
        return send(request);
    }

    public HttpResponse<String> put(String url, String body) throws IOException, InterruptedException {
        return put(url, body, Map.of());
    }

    public HttpResponse<String> put(String url, String body, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest request = newRequestBuilder(url, headers)
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return send(request);
    }

    public HttpResponse<String> put(String url, String body, String target, String action) throws IOException, InterruptedException {
        return put(url, body, target, action, Map.of());
    }

    public HttpResponse<String> put(String url, String body, String target, String action, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest request = newRequestBuilder(url, target, action, headers)
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return send(request);
    }

    public HttpResponse<String> patch(String url, String body) throws IOException, InterruptedException {
        return patch(url, body, Map.of());
    }

    public HttpResponse<String> patch(String url, String body, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest request = newRequestBuilder(url, headers)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build();
        return send(request);
    }

    public HttpResponse<String> patch(String url, String body, String target, String action) throws IOException, InterruptedException {
        return patch(url, body, target, action, Map.of());
    }

    public HttpResponse<String> patch(String url, String body, String target, String action, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest request = newRequestBuilder(url, target, action, headers)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build();
        return send(request);
    }

    public HttpResponse<String> delete(String url) throws IOException, InterruptedException {
        return delete(url, Map.of());
    }

    public HttpResponse<String> delete(String url, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest request = newRequestBuilder(url, headers)
                .DELETE()
                .build();
        return send(request);
    }

    public HttpResponse<String> delete(String url, String target, String action) throws IOException, InterruptedException {
        return delete(url, target, action, Map.of());
    }

    public HttpResponse<String> delete(String url, String target, String action, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest request = newRequestBuilder(url, target, action, headers)
                .DELETE()
                .build();
        return send(request);
    }

    private HttpRequest.Builder newRequestBuilder(String url, Map<String, String> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(requestTimeout);
        headers.forEach(builder::header);
        return builder;
    }

    private HttpRequest.Builder newRequestBuilder(String url, String target, String action, Map<String, String> headers) {
        return newRequestBuilder(url, headers)
                .header("x-euclid-target", target)
                .header("x-euclid-action", action);
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    // Trusts the system default CAs plus (if given) the CAs in caCertPath - equivalent to
    // OpenSSL's set_default_verify_paths() followed by load_verify_file(caCertPath) in
    // euclid-cli's HttpClient.cpp, so a certificate is accepted if either root set vouches for it.
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

    // Trusts a certificate chain if either delegate does, and offers the union of both as
    // accepted issuers.
    private static final class CompositeTrustManager implements X509TrustManager {

        private final X509TrustManager primary;
        private final X509TrustManager secondary;

        CompositeTrustManager(X509TrustManager primary, X509TrustManager secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            try {
                primary.checkClientTrusted(chain, authType);
            } catch (CertificateException e) {
                secondary.checkClientTrusted(chain, authType);
            }
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            try {
                primary.checkServerTrusted(chain, authType);
            } catch (CertificateException e) {
                secondary.checkServerTrusted(chain, authType);
            }
        }

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
