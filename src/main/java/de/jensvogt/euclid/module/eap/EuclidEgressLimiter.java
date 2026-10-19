package de.jensvogt.euclid.module.eap;

import de.jensvogt.euclid.dto.eap.EgressPermitResponse;
import de.jensvogt.euclid.exception.EuclidEgressTimeoutException;
import de.jensvogt.euclid.exception.EuclidServiceException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds an application's calls to outside sites to the limits euclid keeps for it, per site - for
 * all of its instances together.
 *
 * <p>A site that defends itself against denial of service counts per source address, and every
 * instance of the application reaches it from the same one. A limiter inside the application would
 * count per instance, which the autoscaler multiplies just when there is most to fetch. So the
 * limit is kept in euclid (EAP), and this asks it for a permit before each request:
 *
 * <pre>{@code
 * EuclidEgressLimiter limiter = new EuclidEgressLimiter(eap);   // the application from EUCLID_APPLICATION_ID
 *
 * try (EuclidEgressLimiter.Permit permit = limiter.acquire(uri)) {
 *     HttpResponse<Path> response = http.send(request, BodyHandlers.ofFile(target));
 *     if (response.statusCode() == 429 || response.statusCode() == 503) {
 *         limiter.throttled(uri.getHost(), EuclidEgressLimiter.retryAfter(response).orElse(Duration.ofMinutes(1)));
 *     }
 * }
 * }</pre>
 *
 * <p>{@link #acquire(String)} waits until euclid grants a permit, polling with jitter so a crowd of
 * instances does not ask in step. Closing the permit gives its slot back; one never closed - the
 * instance died - comes back when its lease runs out. What the application is allowed for a site
 * is set with {@code euclid-cli eap set-egress-limit}; another application calling the same site
 * has limits of its own.
 *
 * <p>If euclid cannot be asked at all, the limiter does not stop the application: it falls back to
 * one request per {@link #fallbackInterval(Duration) fallback interval} per site, for this instance
 * alone, and says so in the log. Slower than it needs to be, rather than a download run that halts
 * because the manager was restarted.
 *
 * <p>Thread-safe; one instance per application is enough.
 */
public final class EuclidEgressLimiter {

    private static final Logger LOG = Logger.getLogger(EuclidEgressLimiter.class.getName());

    /**
     * The longest a single wait between two requests for a permit, whatever euclid suggested: a
     * slot freed early is noticed within this.
     */
    private static final Duration MAX_POLL = Duration.ofSeconds(5);

    /**
     * How often the fallback is reported in the log, per site.
     */
    private static final Duration FALLBACK_LOG_INTERVAL = Duration.ofMinutes(1);

    /**
     * The three calls this needs - EuclidEap's, or a test's.
     */
    interface Backend {
        EgressPermitResponse acquire(String site, long leaseSeconds) throws IOException, InterruptedException;

        boolean release(String site, String permitId) throws IOException, InterruptedException;

        void throttled(String site, long retryAfterSeconds) throws IOException, InterruptedException;
    }

    /**
     * How the limiter waits - Thread.sleep, or a test's clock.
     */
    interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }

    private final Backend backend;
    private final Sleeper sleeper;
    private final LongSupplier nanoTime;

    private volatile Duration lease = Duration.ofMinutes(15);
    private volatile Duration maxWait = Duration.ofMinutes(30);
    private volatile Duration fallbackInterval = Duration.ofSeconds(2);

    /**
     * When this instance last let a request through to a site on its own, without euclid.
     */
    private final Map<String, Long> lastFallbackNanos = new ConcurrentHashMap<>();

    /**
     * When the fallback was last logged, per site.
     */
    private final Map<String, Long> lastFallbackLogNanos = new ConcurrentHashMap<>();

    /**
     * Creates a limiter for the application this process runs as: the one named by
     * {@code EUCLID_APPLICATION_ID}, which euclid sets for every application it starts.
     *
     * @param eap the EAP client, authenticated as the application
     * @throws IllegalStateException if {@code EUCLID_APPLICATION_ID} is not set - the process was
     *                               not started by euclid; use {@link #EuclidEgressLimiter(EuclidEap, String)}
     */
    public EuclidEgressLimiter(EuclidEap eap) {
        this(eap, applicationIdFromEnvironment());
    }

    /**
     * Creates a limiter for the given application.
     *
     * @param eap           the EAP client, authenticated as the identity the application runs as
     * @param applicationId the application whose limits apply
     */
    public EuclidEgressLimiter(EuclidEap eap, String applicationId) {
        this(new Backend() {
            @Override
            public EgressPermitResponse acquire(String site, long leaseSeconds) throws IOException, InterruptedException {
                return eap.acquireEgressPermit(applicationId, site, leaseSeconds);
            }

            @Override
            public boolean release(String site, String permitId) throws IOException, InterruptedException {
                return eap.releaseEgressPermit(applicationId, site, permitId);
            }

            @Override
            public void throttled(String site, long retryAfterSeconds) throws IOException, InterruptedException {
                eap.reportEgressThrottled(applicationId, site, retryAfterSeconds);
            }
        }, duration -> Thread.sleep(duration.toMillis()), System::nanoTime);
        if (applicationId == null || applicationId.isBlank()) {
            throw new IllegalArgumentException("applicationId is required");
        }
    }

    private static String applicationIdFromEnvironment() {
        String id = System.getenv("EUCLID_APPLICATION_ID");
        if (id == null || id.isBlank()) {
            throw new IllegalStateException("EUCLID_APPLICATION_ID is not set - this process was not started by euclid; "
                    + "pass the application id explicitly");
        }
        return id;
    }

    EuclidEgressLimiter(Backend backend, Sleeper sleeper, LongSupplier nanoTime) {
        this.backend = Objects.requireNonNull(backend);
        this.sleeper = Objects.requireNonNull(sleeper);
        this.nanoTime = Objects.requireNonNull(nanoTime);
    }

    /**
     * How long a permit's slot is held if it is never closed. Longer than any request takes: a
     * request outliving its lease stops counting against the site. Default 15 minutes; at most six
     * hours.
     *
     * @param lease the lease
     * @return this limiter
     */
    public EuclidEgressLimiter leaseTime(Duration lease) {
        this.lease = Objects.requireNonNull(lease);
        return this;
    }

    /**
     * How long {@link #acquire(String)} waits for a permit before giving up with an
     * {@link EuclidEgressTimeoutException}. Default 30 minutes - a site blocked after a 429 can
     * keep everyone waiting that long.
     *
     * @param maxWait the longest wait
     * @return this limiter
     */
    public EuclidEgressLimiter maxWait(Duration maxWait) {
        this.maxWait = Objects.requireNonNull(maxWait);
        return this;
    }

    /**
     * How far apart this instance spaces its requests to one site while euclid cannot be asked.
     * Default 2 seconds.
     *
     * @param fallbackInterval the spacing
     * @return this limiter
     */
    public EuclidEgressLimiter fallbackInterval(Duration fallbackInterval) {
        this.fallbackInterval = Objects.requireNonNull(fallbackInterval);
        return this;
    }

    /**
     * Waits for a permit to call the host of {@code uri}.
     *
     * @param uri the URI about to be requested
     * @return the permit; close it when the request is done
     * @throws EuclidEgressTimeoutException if none came free within {@link #maxWait(Duration)}
     * @throws InterruptedException         if interrupted while waiting
     */
    public Permit acquire(URI uri) throws EuclidEgressTimeoutException, InterruptedException {
        return acquire(uri.getHost());
    }

    /**
     * Waits for a permit to call {@code site}.
     *
     * @param site the host name about to be called
     * @return the permit; close it when the request is done
     * @throws EuclidEgressTimeoutException if none came free within {@link #maxWait(Duration)}
     * @throws InterruptedException         if interrupted while waiting
     */
    public Permit acquire(String site) throws EuclidEgressTimeoutException, InterruptedException {
        String key = normalize(site);
        long started = nanoTime.getAsLong();
        long leaseSeconds = Math.max(1, lease.toSeconds());

        while (true) {
            EgressPermitResponse answer;
            try {
                answer = backend.acquire(key, leaseSeconds);
            } catch (IOException | RuntimeException e) {
                return fallback(key, e);
            }

            if (answer.granted()) {
                return new Permit(this, key, answer.permitId());
            }

            Duration waited = Duration.ofNanos(nanoTime.getAsLong() - started);
            Duration wait = jittered(answer.retryAfterMs());
            if (waited.plus(wait).compareTo(maxWait) > 0) {
                throw new EuclidEgressTimeoutException(key, waited, answer.reason());
            }
            sleeper.sleep(wait);
        }
    }

    /**
     * Reports that {@code site} refused a request - a 429 or 503 - so that no instance of this
     * application calls it until {@code retryAfter} has passed. Never throws: failing to report
     * is logged, and the caller goes on handling the refusal it got.
     *
     * @param site       the site that refused
     * @param retryAfter how long it asked to be left alone; see {@link #retryAfter(HttpResponse)}
     */
    public void throttled(String site, Duration retryAfter) {
        String key = normalize(site);
        try {
            backend.throttled(key, Math.max(1, retryAfter.toSeconds()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException | RuntimeException e) {
            LOG.log(Level.WARNING, "cannot report egress throttling for " + key + ": " + e.getMessage());
        }
    }

    /**
     * Reads a response's {@code Retry-After}, in either of its forms: a number of seconds, or an
     * HTTP date.
     *
     * @param response the response that refused the request
     * @return how long to wait, if the response said
     */
    public static Optional<Duration> retryAfter(HttpResponse<?> response) {
        return response.headers().firstValue("Retry-After").flatMap(EuclidEgressLimiter::parseRetryAfter);
    }

    static Optional<Duration> parseRetryAfter(String value) {
        String v = value.trim();
        try {
            return Optional.of(Duration.ofSeconds(Math.max(0, Long.parseLong(v))));
        } catch (NumberFormatException ignored) {
            // not seconds - try a date
        }
        try {
            ZonedDateTime at = ZonedDateTime.parse(v, DateTimeFormatter.RFC_1123_DATE_TIME);
            Duration until = Duration.between(ZonedDateTime.now(at.getZone()), at);
            return Optional.of(until.isNegative() ? Duration.ZERO : until);
        } catch (DateTimeParseException ignored) {
            return Optional.empty();
        }
    }

    // ── internals ────────────────────────────────────────────────────────────

    private Permit fallback(String site, Exception cause) throws InterruptedException {
        long now = nanoTime.getAsLong();
        Long lastLog = lastFallbackLogNanos.get(site);
        if (lastLog == null || now - lastLog >= FALLBACK_LOG_INTERVAL.toNanos()) {
            lastFallbackLogNanos.put(site, now);
            LOG.log(Level.WARNING, "cannot ask euclid for an egress permit for " + site + " ("
                    + describe(cause) + "); spacing requests " + fallbackInterval.toMillis()
                    + "ms apart in this instance until it answers again");
        }

        // One request per interval per site, for this instance: reserve the next free moment
        // atomically, so threads of the same instance queue up rather than all going at once.
        long interval = fallbackInterval.toNanos();
        long[] slot = new long[1];
        lastFallbackNanos.compute(site, (k, last) -> {
            long next = last == null ? now : Math.max(now, last + interval);
            slot[0] = next;
            return next;
        });
        long wait = slot[0] - now;
        if (wait > 0) sleeper.sleep(Duration.ofNanos(wait));
        return new Permit(this, site, null);
    }

    private static String describe(Exception e) {
        if (e instanceof EuclidServiceException se) return "HTTP " + se.statusCode();
        return e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage());
    }

    private static Duration jittered(long retryAfterMs) {
        long base = Math.max(10, Math.min(retryAfterMs, MAX_POLL.toMillis()));
        long jitter = ThreadLocalRandom.current().nextLong(base / 5 + 1);
        return Duration.ofMillis(base + jitter);
    }

    private static String normalize(String site) {
        if (site == null || site.isBlank()) throw new IllegalArgumentException("site is required: the host name to be called");
        String s = site.trim().toLowerCase(Locale.ROOT);
        while (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s;
    }

    void release(Permit permit) {
        if (permit.id == null) return;
        try {
            backend.release(permit.site, permit.id);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException | RuntimeException e) {
            // The slot comes back when its lease runs out; nothing the caller can do about it.
            LOG.log(Level.FINE, "cannot release egress permit " + permit.id + " for " + permit.site + ": " + e.getMessage());
        }
    }

    /**
     * Permission to make one request to a site. Close it when the request is done - in a
     * try-with-resources - to give its slot back.
     */
    public static final class Permit implements AutoCloseable {

        private final EuclidEgressLimiter limiter;
        private final String site;
        private final String id;
        private final AtomicBoolean closed = new AtomicBoolean();

        Permit(EuclidEgressLimiter limiter, String site, String id) {
            this.limiter = limiter;
            this.site = site;
            this.id = id;
        }

        /**
         * The site the permit is for.
         *
         * @return the lower-cased host name
         */
        public String site() {
            return site;
        }

        /**
         * Whether this permit was granted by this instance alone, because euclid could not be
         * asked - see the class description.
         *
         * @return true for a fallback permit
         */
        public boolean local() {
            return id == null;
        }

        /**
         * Gives the slot back. Safe to call more than once; never throws.
         */
        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) limiter.release(this);
        }
    }
}
