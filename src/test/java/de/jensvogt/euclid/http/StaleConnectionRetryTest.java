package de.jensvogt.euclid.http;

import de.jensvogt.euclid.module.ens.EuclidEns;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A pooled connection can be closed by the far end between being handed out and being written to,
 * and nothing says so until the client reads back an immediate end of stream. What the application
 * sees is "HTTP/1.1 header parser received no bytes" out of an ordinary call - a poll, a publish -
 * that a second attempt on a fresh connection would have completed.
 *
 * <p>These cover the one retry for that, and its limit.
 */
class StaleConnectionRetryTest {

    private ServerSocket server;
    private Thread accepting;
    private final AtomicInteger connections = new AtomicInteger();

    @AfterEach
    void stopServer() throws Exception {
        if (accepting != null) {
            accepting.interrupt();
        }
        if (server != null) {
            server.close();
        }
    }

    @Test
    void aConnectionClosedBeforeAnyResponseIsSentAgainOnAFreshOne() throws Exception {
        // The first attempt gets the dead connection, the second a working one - which is exactly
        // what a server that closed an idle connection while it sat in the pool looks like.
        start(1);

        assertEquals("msg-1", client().publishMessage("ern:topic", "hello").messageId());
        assertEquals(2, connections.get(), "the request should have been sent again on a new connection");
    }

    @Test
    void aServerThatNeverAnswersIsReportedRatherThanRetriedForever() throws Exception {
        start(Integer.MAX_VALUE);

        // Retrying past the first attempt would turn a server that is down into a client that
        // hammers it, and hide the failure from the caller for as long as it kept trying.
        assertThrows(IOException.class, () -> client().publishMessage("ern:topic", "hello"));
        assertEquals(2, connections.get(), "one retry, not more");
    }

    private EuclidEns client() {
        return new EuclidEns("http://localhost:" + server.getLocalPort(), "unused", "eu-central-1", "000000000000",
                "alice", null, null, null, null);
    }

    /**
     * Accepts connections, closing the first {@code dropped} of them once the request has been
     * read and answering the rest - the request is read either way, so the client fails on the
     * response rather than on writing.
     */
    private void start(int dropped) throws IOException {
        server = new ServerSocket(0);
        accepting = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try (Socket socket = server.accept()) {
                    int connection = connections.incrementAndGet();
                    readRequest(socket.getInputStream());
                    if (connection <= dropped) {
                        continue;
                    }
                    String body = "{\"messageId\":\"msg-1\"}";
                    OutputStream out = socket.getOutputStream();
                    out.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: "
                            + body.length() + "\r\n\r\n" + body).getBytes(StandardCharsets.UTF_8));
                    out.flush();
                } catch (IOException e) {
                    return;
                }
            }
        }, "stale-connection-server");
        accepting.setDaemon(true);
        accepting.start();
    }

    /**
     * Consumes one request - headers, then as many body bytes as Content-Length promised - so that
     * closing afterwards is the server dropping the answer rather than refusing the question.
     */
    private static void readRequest(InputStream in) throws IOException {
        StringBuilder headers = new StringBuilder();
        while (!headers.toString().endsWith("\r\n\r\n")) {
            int read = in.read();
            if (read < 0) {
                return;
            }
            headers.append((char) read);
        }
        long length = 0;
        for (String header : headers.toString().split("\r\n")) {
            if (header.toLowerCase().startsWith("content-length:")) {
                length = Long.parseLong(header.substring("content-length:".length()).trim());
            }
        }
        for (long i = 0; i < length && in.read() >= 0; i++) {
            // drained, so the client's write completes before the connection goes
        }
    }
}
