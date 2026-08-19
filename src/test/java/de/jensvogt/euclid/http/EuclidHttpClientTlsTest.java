package de.jensvogt.euclid.http;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Confirms EuclidHttpClient actually speaks TLS: a self-signed server certificate is rejected by
 * default (same as any JVM would, via the system trust store) but accepted once its issuing
 * certificate is passed as {@code caCertPath} - mirroring euclid-cli's {@code --ca-cert}.
 */
class EuclidHttpClientTlsTest {

    private static final char[] STORE_PASSWORD = "changeit".toCharArray();

    private HttpsServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void trustsSelfSignedServerCertificateWhenCaCertPathConfigured(@TempDir Path tempDir) throws Exception {
        Path keystorePath = tempDir.resolve("keystore.p12");
        Path certPath = tempDir.resolve("cert.pem");
        generateSelfSignedCertificate(keystorePath, certPath);

        server = startTlsServer(keystorePath);

        HttpResponse<String> response = new EuclidHttpClient(certPath.toString()).get(baseUrl());

        assertEquals(200, response.statusCode());
        assertEquals("ok", response.body());
    }

    @Test
    void rejectsSelfSignedServerCertificateWithoutCaCertPath(@TempDir Path tempDir) throws Exception {
        Path keystorePath = tempDir.resolve("keystore.p12");
        Path certPath = tempDir.resolve("cert.pem");
        generateSelfSignedCertificate(keystorePath, certPath);

        server = startTlsServer(keystorePath);

        assertThrows(IOException.class, () -> new EuclidHttpClient().get(baseUrl()));
    }

    private String baseUrl() {
        return "https://localhost:" + server.getAddress().getPort() + "/";
    }

    private static HttpsServer startTlsServer(Path keystorePath) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(keystorePath)) {
            keyStore.load(in, STORE_PASSWORD);
        }
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, STORE_PASSWORD);

        SSLContext serverContext = SSLContext.getInstance("TLS");
        serverContext.init(keyManagerFactory.getKeyManagers(), null, null);

        HttpsServer httpsServer = HttpsServer.create(new InetSocketAddress("localhost", 0), 0);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(serverContext));
        httpsServer.createContext("/", exchange -> {
            byte[] bytes = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (var os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        httpsServer.start();
        return httpsServer;
    }

    private static void generateSelfSignedCertificate(Path keystorePath, Path certPath) throws Exception {
        run("keytool", "-genkeypair", "-alias", "euclid", "-keyalg", "RSA", "-keysize", "2048", "-validity", "1",
                "-storetype", "PKCS12", "-keystore", keystorePath.toString(), "-storepass", "changeit",
                "-keypass", "changeit", "-dname", "CN=localhost",
                "-ext", "SAN=dns:localhost,ip:127.0.0.1");
        run("keytool", "-exportcert", "-alias", "euclid", "-keystore", keystorePath.toString(),
                "-storepass", "changeit", "-rfc", "-file", certPath.toString());
    }

    private static void run(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("command failed (" + exitCode + "): " + String.join(" ", command) + "\n" + output);
        }
    }
}
