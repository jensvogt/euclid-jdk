package de.jensvogt.euclid.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * The bearer token euclid currently holds on disk for a deployed application.
 *
 * <p>An application euclid runs is not given a token. It is given the name of a file holding one,
 * in {@code EUCLID_CREDENTIALS_FILE}, and euclid rewrites that file with a fresh token once less
 * than half the token's lifetime is left - roughly every thirty minutes on the default one-hour
 * lifetime. Which means the token a process read when it started is not the token it should be
 * sending an hour later, and a client that kept the first one it saw works until it doesn't:
 * {@code publish-message failed with status 401: Bearer token expired}, in the middle of whatever
 * the application was doing.
 *
 * <p>This reads the file instead of remembering it. {@link #get()} is called once per request, so
 * it does not re-read the file each time - it stats it, and re-reads only when the file has
 * actually changed since the last read. A rotation is therefore picked up by the next request
 * after it happens, and every request in between costs one {@code stat}.
 *
 * <p>Nothing here decides whether the token is still valid. That is the server's answer to give,
 * and reaching for the clock instead only introduces a second thing that can be wrong about the
 * time. Whatever the file holds is what goes on the wire; when a token goes stale in flight
 * anyway, {@code EuclidHttpClient} asks for the headers again and the file has the new one by
 * then.
 *
 * <p>Every client the JDK builds uses this automatically when the environment names a credentials
 * file belonging to the same user the client was built for - see
 * {@link #forClient(String, String)}. A caller that wants it somewhere else can install it
 * directly:
 *
 * <pre>{@code
 * EuclidEns ens = session.ens();
 * ens.token(CredentialsFileTokens.fromEnvironment());
 * }</pre>
 *
 * <p>Instances are safe to share between threads.
 */
public final class CredentialsFileTokens implements Supplier<String> {

    /**
     * The environment variable euclid sets on a deployed application, naming the file this reads.
     */
    public static final String CREDENTIALS_FILE_VARIABLE = "EUCLID_CREDENTIALS_FILE";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    /**
     * What was read, and the state of the file it was read from.
     *
     * <p>Held as one immutable object behind a single volatile field so a reader either sees a
     * whole earlier read or a whole later one, never a token from one read paired with the file
     * state of another - which is what would make a rotation look as though it had already been
     * picked up.
     *
     * @param token      the token the file held
     * @param userId     the user the file names, or an empty string if it names none
     * @param fingerprint identifies the file's contents as of that read; {@code null} before the
     *                    first successful read
     */
    private record Snapshot(String token, String userId, String fingerprint) {}

    private static final Snapshot NOTHING_READ = new Snapshot("", "", null);

    private final Path path;
    private volatile Snapshot snapshot = NOTHING_READ;

    private CredentialsFileTokens(Path path) {
        this.path = path;
    }

    /**
     * Reads the credentials file named by {@code EUCLID_CREDENTIALS_FILE}.
     *
     * @return a supplier of the token that file currently holds
     * @throws IllegalStateException if the variable is unset or blank, i.e. this process was not
     *                               started by euclid as an application - a caller asking for the
     *                               managed credentials in a process that has none has made a
     *                               mistake worth hearing about rather than silently getting an
     *                               empty token and a 401 later
     */
    public static CredentialsFileTokens fromEnvironment() {
        final String path = System.getenv(CREDENTIALS_FILE_VARIABLE);
        if (path == null || path.isBlank()) {
            throw new IllegalStateException(CREDENTIALS_FILE_VARIABLE + " is not set - this process holds no euclid-managed credentials");
        }
        return forFile(Path.of(path));
    }

    /**
     * Reads a credentials file at a path of the caller's choosing.
     *
     * @param path the file to read; it need not exist yet
     * @return a supplier of the token that file currently holds
     */
    public static CredentialsFileTokens forFile(Path path) {
        return new CredentialsFileTokens(Objects.requireNonNull(path, "path must not be null"));
    }

    /**
     * The token supplier a client built with this fixed token should use.
     *
     * <p>The reason clients call this rather than freezing the token they were handed: inside an
     * application euclid deployed, that token came out of the credentials file and will be
     * replaced there, so the client should follow the file rather than the copy it was given.
     * Outside one - a command-line tool, a test, a process that logged in with a password - there
     * is no file to follow and the fixed token is the only answer.
     *
     * <p>The identity is what tells those apart, and it is checked rather than assumed. An
     * application is free to log in as somebody else while its own credentials sit in a file it is
     * not using; a client built for that other user keeps the token it was built with, because
     * quietly swapping in the application's identity would change who the call is made as. Only a
     * client built for the same user the file names follows the file.
     *
     * @param token  the token the client was constructed with, possibly empty
     * @param userId the user the client was constructed for, possibly empty
     * @return a supplier that follows the credentials file, or one returning {@code token}
     */
    public static Supplier<String> forClient(String token, String userId) {
        return forClient(token, userId, System.getenv(CREDENTIALS_FILE_VARIABLE));
    }

    /**
     * {@link #forClient(String, String)} with the credentials file named explicitly, so the
     * decision can be exercised without an environment to set.
     *
     * @param token          the token the client was constructed with, possibly empty
     * @param userId         the user the client was constructed for, possibly empty
     * @param credentialsFile path of the credentials file, or {@code null}/blank if there is none
     * @return a supplier that follows the credentials file, or one returning {@code token}
     */
    static Supplier<String> forClient(String token, String userId, String credentialsFile) {

        final String fixed = token == null ? "" : token;
        if (credentialsFile == null || credentialsFile.isBlank()) {
            return () -> fixed;
        }

        final CredentialsFileTokens managed = forFile(Path.of(credentialsFile));
        if (managed.get().isEmpty()) {
            // Named but unreadable or malformed. Nothing to follow, and refusing to build a client
            // over it would be worse than carrying on with the token the caller already has.
            return () -> fixed;
        }

        // An empty userId on either side is not a mismatch: a client built without one has not
        // said it means somebody else, and a file without one predates them being recorded.
        final String owner = managed.userId();
        if (!owner.isEmpty() && userId != null && !userId.isEmpty() && !owner.equals(userId)) {
            return () -> fixed;
        }
        return managed;
    }

    /**
     * The token to present right now.
     *
     * <p>Re-reads the file only when it has changed since the last read, so this stays cheap
     * enough to call on every request. If the file has gone missing or cannot be read, the last
     * token successfully read from it is returned rather than an empty one - a token that may
     * still be good beats a request that is certain to fail.
     *
     * @return the current token, or an empty string if the file has never been read successfully
     */
    @Override
    public String get() {
        return current().token();
    }

    /**
     * The user the credentials file names.
     *
     * @return the user ID, or an empty string if the file has never been read successfully
     */
    public String userId() {
        return current().userId();
    }

    private Snapshot current() {

        final Snapshot cached = snapshot;
        final String fingerprint = fingerprint();

        // Unreadable, or unchanged since the read that produced what is cached.
        if (fingerprint == null || fingerprint.equals(cached.fingerprint())) {
            return cached;
        }

        final Snapshot reread = read(fingerprint);
        if (reread == null) {
            return cached;
        }

        // Two threads arriving together both read and both store; they read the same file and
        // produce the same snapshot, so whichever lands last is the same answer.
        snapshot = reread;
        return reread;
    }

    /**
     * Identifies the file's contents cheaply enough to check per request: euclid replaces the file
     * by renaming a new one over it, so a rotation always changes both its modification time and,
     * because the token differs, very nearly always its size.
     *
     * @return the fingerprint, or {@code null} if the file cannot be stat'd
     */
    private String fingerprint() {
        try {
            final BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
            return attributes.lastModifiedTime().toMillis() + ":" + attributes.size();
        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }

    private Snapshot read(String fingerprint) {
        try {
            final JsonNode parsed = OBJECT_MAPPER.readTree(Files.readString(path));
            final String token = parsed.path("token").asText("");
            if (token.isEmpty()) {
                return null;
            }
            return new Snapshot(token, parsed.path("userId").asText(""), fingerprint);
        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }
}
