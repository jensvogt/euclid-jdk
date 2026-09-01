package de.jensvogt.euclid.testutil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * Minimal hand-rolled test double for the Euclid gateway, standing in for both its plain HTTP
 * action-call path and its websocket upgrade/subscribe/event-push path on the same port - since
 * neither {@code com.sun.net.httpserver.HttpServer} (used elsewhere in these tests) nor the JDK's
 * {@code java.net.http} package offer a server-side websocket implementation.
 * <p>
 * A plain (non-upgrade) request is answered via {@code httpHandler}, keyed by the
 * {@code x-euclid-action} header, mirroring how the real gateway dispatches. An upgrade request
 * (an {@code Upgrade: websocket} header present) completes the RFC 6455 handshake; the connection
 * is then held open, reading masked client frames on the same connection-handling thread and
 * acking {@code subscribe}/{@code unsubscribe} frames exactly like {@code GatewayWsSession} does
 * (see {@code HandleSubscriptionFrame()} in {@code GatewayWsSession.cpp}). {@link #sendEventFrame}
 * mirrors {@code GatewayWsRegistry::Broadcast()}'s opt-in delivery: a socket only receives an
 * event if one of its current subscriptions matches the event's topic and filter.
 */
public final class FakeGatewayServer implements AutoCloseable {

    private static final String WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ServerSocket serverSocket;
    private final BiFunction<Map<String, String>, String, String> httpHandler;
    private final CopyOnWriteArrayList<Map<String, String>> handshakeHeaders = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Socket> wsSockets = new CopyOnWriteArrayList<>();
    private final Map<Socket, List<Subscription>> subscriptionsBySocket = new ConcurrentHashMap<>();
    private final CountDownLatch handshakeLatch = new CountDownLatch(1);
    private volatile boolean closed;

    /**
     * Creates a new fake gateway server listening on a random port.
     *
     * @param httpHandler receives the request headers and body of a plain (non-upgrade) request,
     *                    and returns the JSON body to answer with (always as a 200 OK)
     * @throws IOException if the server socket cannot be opened
     */
    public FakeGatewayServer(BiFunction<Map<String, String>, String, String> httpHandler) throws IOException {
        this.httpHandler = httpHandler;
        this.serverSocket = new ServerSocket(0);
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "fake-gateway-accept");
            t.setDaemon(true);
            return t;
        }).submit(this::acceptLoop);
    }

    /**
     * Returns the port the server is listening on.
     *
     * @return the port the server is listening on
     */
    public int port() {
        return serverSocket.getLocalPort();
    }

    /**
     * Blocks until at least one websocket handshake has completed, returning the headers the
     * client sent on its upgrade request.
     *
     * @param timeoutSeconds the maximum time to wait for the handshake to complete
     * @return the headers sent by the client on the upgrade request, keyed by header name
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public Map<String, String> awaitHandshake(long timeoutSeconds) throws InterruptedException {
        if (!handshakeLatch.await(timeoutSeconds, TimeUnit.SECONDS)) {
            throw new AssertionError("no websocket handshake received within " + timeoutSeconds + "s");
        }
        return handshakeHeaders.getFirst();
    }

    /**
     * Blocks until the given socket has at least one subscription matching {@code topic}, polling
     * at a short interval - used to avoid races where a test pushes an event before the client's
     * subscribe frame has been read and recorded.
     *
     * @param topic          the topic to wait for
     * @param timeoutSeconds the maximum time to wait for the subscription to be received
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public void awaitSubscription(String topic, long timeoutSeconds) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000;
        while (System.currentTimeMillis() < deadline) {
            for (List<Subscription> subs : subscriptionsBySocket.values()) {
                if (subs.stream().anyMatch(s -> s.topic.equals(topic))) {
                    return;
                }
            }
            Thread.sleep(10);
        }
        throw new AssertionError("no subscription for topic " + topic + " received within " + timeoutSeconds + "s");
    }

    /**
     * Sends an "event" frame for {@code topic}/{@code body} to every websocket client currently
     * subscribed to a matching topic/filter - mirroring {@code GatewayWsRegistry::Broadcast()}'s
     * opt-in delivery. A silent no-op for any socket with no matching subscription.
     *
     * @param topic the event topic to publish to, e.g. "eqs.create"
     * @param body  the event body, keyed by the event's field names
     * @throws IOException if the frame cannot be written to any connected websocket client
     */
    public void sendEventFrame(String topic, Map<String, String> body) throws IOException {
        String frame = buildEventFrame(topic, body);
        for (Socket socket : wsSockets) {
            List<Subscription> subs = subscriptionsBySocket.getOrDefault(socket, List.of());
            if (subs.stream().anyMatch(s -> s.matches(topic, body))) {
                writeFrame(socket, frame);
            }
        }
    }

    /**
     * Sends a raw pre-built frame to every connected websocket client, bypassing subscription
     * matching entirely - for tests that need to prove the client's own defense-in-depth
     * filtering (rather than the fake server's) rejects a non-matching frame.
     *
     * @param json the raw frame to send, e.g. "{\"type\":\"subscribe\",\"id\":\"sub123\"}"
     * @throws IOException if the frame cannot be written to any connected websocket client
     */
    public void sendRawFrame(String json) throws IOException {
        for (Socket socket : wsSockets) {
            writeFrame(socket, json);
        }
    }

    /**
     * Drops every connected websocket client, the way an idle timeout or a gateway restart does,
     * forgetting the subscriptions those connections carried.
     *
     * <p>Forgetting them is the point: the real gateway attaches a subscription to the session
     * that asked for it ({@code GatewayWsRegistry}), so a client that reconnects without saying
     * what it wants again is connected and receives nothing.
     *
     * @throws IOException if a socket cannot be closed
     */
    public void dropConnections() throws IOException {
        List<Socket> dropped = List.copyOf(wsSockets);
        wsSockets.removeAll(dropped);
        for (Socket socket : dropped) {
            subscriptionsBySocket.remove(socket);
            socket.close();
        }
    }

    /**
     * Blocks until some websocket connection other than the ones already dropped carries a
     * subscription for {@code topic}.
     *
     * @param topic          the topic to wait for
     * @param timeoutSeconds the maximum time to wait
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public void awaitSubscriptionCount(String topic, int count, long timeoutSeconds) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000;
        while (System.currentTimeMillis() < deadline) {
            long matching = subscriptionsBySocket.values().stream()
                    .filter(subs -> subs.stream().anyMatch(s -> s.topic.equals(topic)))
                    .count();
            if (matching >= count) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("fewer than " + count + " connection(s) subscribed to " + topic
                + " within " + timeoutSeconds + "s");
    }

    /**
     * Closes the fake gateway server and releases all associated resources.
     * This includes closing all active websocket connections as well as the server socket.
     *
     * @throws IOException if an I/O error occurs while closing the server socket or any connected websocket.
     */
    @Override
    public void close() throws IOException {
        closed = true;
        for (Socket socket : wsSockets) {
            socket.close();
        }
        serverSocket.close();
    }

    private void acceptLoop() {
        while (!closed) {
            try {
                Socket socket = serverSocket.accept();
                Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r, "fake-gateway-conn");
                    t.setDaemon(true);
                    return t;
                }).submit(() -> handleConnection(socket));
            } catch (IOException e) {
                return;
            }
        }
    }

    private void handleConnection(Socket socket) {
        try {
            InputStream in = socket.getInputStream();
            String requestLine = readLine(in);
            if (requestLine == null || requestLine.isEmpty()) {
                socket.close();
                return;
            }
            Map<String, String> headers = readHeaders(in);

            if ("websocket".equalsIgnoreCase(headers.get("upgrade"))) {
                handshake(socket, headers);
                wsSockets.add(socket);
                subscriptionsBySocket.put(socket, new CopyOnWriteArrayList<>());
                handshakeHeaders.add(headers);
                handshakeLatch.countDown();
                readClientFrames(socket, in);
                return;
            }

            String body = readBody(in, headers);
            String responseBody = httpHandler.apply(headers, body);
            writeHttpResponse(socket.getOutputStream(), responseBody);
            socket.close();
        } catch (IOException e) {
            // Connection dropped mid-handshake/response - nothing to clean up beyond closing.
            try {
                socket.close();
            } catch (IOException ignored) {
                // best-effort
            }
        }
    }

    // Reads masked client->server frames until the connection closes, acking every
    // subscribe/unsubscribe frame exactly like GatewayWsSession::handleSubscriptionFrame().
    private void readClientFrames(Socket socket, InputStream in) {
        List<Subscription> subs = subscriptionsBySocket.get(socket);
        while (!closed) {
            String text;
            try {
                text = readClientTextFrame(in);
            } catch (IOException e) {
                return;
            }
            if (text == null) {
                return;
            }
            try {
                handleClientFrame(socket, subs, text);
            } catch (IOException ignored) {
                // malformed frame from the client - not exercised by these tests
            }
        }
    }

    private void handleClientFrame(Socket socket, List<Subscription> subs, String text) throws IOException {
        JsonNode node = OBJECT_MAPPER.readTree(text);
        String type = node.path("type").asText(null);
        String id = node.path("id").asText(null);
        if (!"subscribe".equals(type) && !"unsubscribe".equals(type)) {
            return;
        }
        String topic = node.path("topic").asText(null);
        Map<String, String> filter = new LinkedHashMap<>();
        JsonNode filterNode = node.get("filter");
        if (filterNode != null && filterNode.isObject()) {
            filterNode.fields().forEachRemaining(entry -> filter.put(entry.getKey(), entry.getValue().asText()));
        }
        Subscription subscription = new Subscription(topic, filter);
        if ("subscribe".equals(type)) {
            subs.add(subscription);
        } else {
            subs.removeIf(s -> s.topic.equals(subscription.topic) && s.filter.equals(subscription.filter));
        }
        writeFrame(socket, "{\"type\":\"" + type + "d\",\"id\":\"" + id + "\"}");
    }

    private static void writeFrame(Socket socket, String json) throws IOException {
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        OutputStream out = socket.getOutputStream();
        synchronized (out) {
            out.write(0x81); // FIN + text opcode
            if (payload.length < 126) {
                out.write(payload.length);
            } else if (payload.length < 65536) {
                out.write(126);
                out.write((payload.length >> 8) & 0xFF);
                out.write(payload.length & 0xFF);
            } else {
                out.write(127);
                for (int i = 7; i >= 0; i--) {
                    out.write((payload.length >> (8 * i)) & 0xFF);
                }
            }
            out.write(payload);
            out.flush();
        }
    }

    private static String buildEventFrame(String topic, Map<String, String> body) {
        StringBuilder json = new StringBuilder();
        json.append("{\"type\":\"event\",\"id\":\"evt\",\"topic\":\"").append(topic).append('"')
                .append(",\"accountId\":\"863459426936\",\"region\":\"eu-central-1\",\"body\":{");
        boolean first = true;
        for (Map.Entry<String, String> entry : body.entrySet()) {
            if (!first) {
                json.append(',');
            }
            json.append('"').append(entry.getKey()).append("\":\"").append(entry.getValue()).append('"');
            first = false;
        }
        json.append("}}");
        return json.toString();
    }

    private static void handshake(Socket socket, Map<String, String> headers) throws IOException {
        String key = headers.get("sec-websocket-key");
        String accept = computeAccept(key);
        String response = "HTTP/1.1 101 Switching Protocols\r\n"
                + "Upgrade: websocket\r\n"
                + "Connection: Upgrade\r\n"
                + "Sec-WebSocket-Accept: " + accept + "\r\n\r\n";
        OutputStream out = socket.getOutputStream();
        out.write(response.getBytes(StandardCharsets.US_ASCII));
        out.flush();
    }

    private static String computeAccept(String key) throws IOException {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] hash = sha1.digest((key + WEBSOCKET_GUID).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    private static void writeHttpResponse(OutputStream out, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        String response = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: application/json\r\n"
                + "Content-Length: " + bytes.length + "\r\n"
                + "Connection: close\r\n\r\n";
        out.write(response.getBytes(StandardCharsets.US_ASCII));
        out.write(bytes);
        out.flush();
    }

    private static String readBody(InputStream in, Map<String, String> headers) throws IOException {
        String contentLength = headers.get("content-length");
        if (contentLength == null) {
            return "";
        }
        int length = Integer.parseInt(contentLength.trim());
        byte[] bytes = in.readNBytes(length);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static Map<String, String> readHeaders(InputStream in) throws IOException {
        Map<String, String> headers = new LinkedHashMap<>();
        String line;
        while ((line = readLine(in)) != null && !line.isEmpty()) {
            int idx = line.indexOf(':');
            if (idx > 0) {
                headers.put(line.substring(0, idx).trim().toLowerCase(Locale.ROOT), line.substring(idx + 1).trim());
            }
        }
        return headers;
    }

    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int prev = -1;
        int cur;
        while ((cur = in.read()) != -1) {
            if (prev == '\r' && cur == '\n') {
                byte[] bytes = buf.toByteArray();
                return new String(bytes, 0, bytes.length - 1, StandardCharsets.US_ASCII);
            }
            buf.write(cur);
            prev = cur;
        }
        return buf.size() == 0 ? null : buf.toString(StandardCharsets.US_ASCII);
    }

    // Reads one masked (client->server) websocket frame per RFC 6455 and returns its text
    // payload, or null on a close frame/EOF. Frames are assumed unfragmented (FIN set), which is
    // all EuclidEventStream ever sends.
    private static String readClientTextFrame(InputStream in) throws IOException {
        int byte0 = in.read();
        if (byte0 == -1) {
            return null;
        }
        int opcode = byte0 & 0x0F;
        int byte1 = in.read();
        if (byte1 == -1) {
            return null;
        }
        boolean masked = (byte1 & 0x80) != 0;
        long length = byte1 & 0x7F;
        if (length == 126) {
            length = ((long) (in.read() & 0xFF) << 8) | (in.read() & 0xFF);
        } else if (length == 127) {
            length = 0;
            for (int i = 0; i < 8; i++) {
                length = (length << 8) | (in.read() & 0xFF);
            }
        }
        byte[] maskKey = new byte[4];
        if (masked) {
            int off = 0;
            while (off < 4) {
                int r = in.read(maskKey, off, 4 - off);
                if (r == -1) {
                    return null;
                }
                off += r;
            }
        }
        byte[] payload = in.readNBytes((int) length);
        if (masked) {
            for (int i = 0; i < payload.length; i++) {
                payload[i] ^= maskKey[i % 4];
            }
        }
        if (opcode == 0x8) {
            return null; // close frame
        }
        return new String(payload, StandardCharsets.UTF_8);
    }

    private record Subscription(String topic, Map<String, String> filter) {
        boolean matches(String eventTopic, Map<String, String> body) {
            if (!topic.equals(eventTopic)) {
                return false;
            }
            for (Map.Entry<String, String> entry : filter.entrySet()) {
                if (!entry.getValue().equals(body.get(entry.getKey()))) {
                    return false;
                }
            }
            return true;
        }
    }
}
