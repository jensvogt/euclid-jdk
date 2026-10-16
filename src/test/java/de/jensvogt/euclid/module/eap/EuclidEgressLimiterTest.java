package de.jensvogt.euclid.module.eap;

import de.jensvogt.euclid.dto.eap.EgressPermitResponse;
import de.jensvogt.euclid.exception.EuclidEgressTimeoutException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The waiting the limiter does on euclid's behalf: ask, wait what euclid said, ask again - and
 * what it does when euclid cannot be asked at all. Time is a fake clock, so nothing here sleeps.
 */
class EuclidEgressLimiterTest {

    /**
     * Answers from a script, and remembers what it was asked.
     */
    private static final class ScriptedBackend implements EuclidEgressLimiter.Backend {
        final Deque<Object> answers = new ArrayDeque<>();
        final List<String> acquired = new ArrayList<>();
        final List<String> released = new ArrayList<>();
        final List<String> throttled = new ArrayList<>();

        @Override
        public EgressPermitResponse acquire(String site, long leaseSeconds) throws IOException {
            acquired.add(site + "/" + leaseSeconds);
            Object next = answers.isEmpty() ? granted("p") : answers.pop();
            if (next instanceof IOException e) throw e;
            return (EgressPermitResponse) next;
        }

        @Override
        public boolean release(String site, String permitId) {
            released.add(site + "/" + permitId);
            return true;
        }

        @Override
        public void throttled(String site, long retryAfterSeconds) {
            throttled.add(site + "/" + retryAfterSeconds);
        }
    }

    private static EgressPermitResponse granted(String id) {
        return new EgressPermitResponse("s", true, id, "2026-10-16T00:00:00Z", null, 0);
    }

    private static EgressPermitResponse refused(String reason, long retryAfterMs) {
        return new EgressPermitResponse("s", false, null, null, reason, retryAfterMs);
    }

    private final ScriptedBackend backend = new ScriptedBackend();
    private final AtomicLong clock = new AtomicLong(1_000_000_000L);
    private final List<Duration> sleeps = new ArrayList<>();

    private EuclidEgressLimiter limiter() {
        return new EuclidEgressLimiter(backend, d -> {
            sleeps.add(d);
            clock.addAndGet(d.toNanos());
        }, clock::get);
    }

    @Test
    void aGrantedPermitIsReleasedWhenClosed() throws Exception {
        backend.answers.add(granted("p1"));

        try (EuclidEgressLimiter.Permit permit = limiter().acquire(URI.create("https://Downloads.Example.com/file.zip"))) {
            assertEquals("downloads.example.com", permit.site());
            assertFalse(permit.local());
        }

        assertEquals(List.of("downloads.example.com/900"), backend.acquired);
        assertEquals(List.of("downloads.example.com/p1"), backend.released);
        assertTrue(sleeps.isEmpty());
    }

    @Test
    void itWaitsWhatEuclidSaysAndAsksAgain() throws Exception {
        backend.answers.add(refused("rate", 400));
        backend.answers.add(refused("concurrency", 1000));
        backend.answers.add(granted("p2"));

        try (EuclidEgressLimiter.Permit ignored = limiter().acquire("a.example.com")) {
            assertEquals(3, backend.acquired.size());
        }

        assertEquals(2, sleeps.size());
        // What euclid said, plus at most a fifth more, so instances do not ask in step.
        assertTrue(sleeps.get(0).toMillis() >= 400 && sleeps.get(0).toMillis() <= 480, sleeps.toString());
        assertTrue(sleeps.get(1).toMillis() >= 1000 && sleeps.get(1).toMillis() <= 1200, sleeps.toString());
    }

    // A site blocked for twenty minutes: the limiter keeps asking every few seconds rather than
    // sleeping the whole block, so a block lifted early is noticed.
    @Test
    void aLongWaitIsTakenInShortSteps() throws Exception {
        backend.answers.add(refused("blocked", 20 * 60 * 1000));
        backend.answers.add(granted("p3"));

        limiter().acquire("b.example.com").close();

        assertTrue(sleeps.getFirst().toMillis() <= 6000, sleeps.toString());
    }

    @Test
    void itGivesUpAfterTheLongestWait() {
        for (int i = 0; i < 1000; i++) backend.answers.add(refused("blocked", 5000));

        EuclidEgressTimeoutException e = assertThrows(EuclidEgressTimeoutException.class,
                () -> limiter().maxWait(Duration.ofSeconds(30)).acquire("c.example.com"));

        assertEquals("c.example.com", e.site());
        assertTrue(e.getMessage().contains("blocked"), e.getMessage());
        assertTrue(Duration.ofNanos(clock.get() - 1_000_000_000L).toSeconds() <= 30);
    }

    // euclid restarting must not halt a download run: requests go on, one per fallback interval,
    // and a fallback permit has nothing to release.
    @Test
    void withoutEuclidItSpacesRequestsItself() throws Exception {
        backend.answers.add(new ConnectException("Connection refused"));
        backend.answers.add(new ConnectException("Connection refused"));
        backend.answers.add(new ConnectException("Connection refused"));
        EuclidEgressLimiter limiter = limiter().fallbackInterval(Duration.ofSeconds(2));

        long start = clock.get();
        for (int i = 0; i < 3; i++) {
            try (EuclidEgressLimiter.Permit permit = limiter.acquire("d.example.com")) {
                assertTrue(permit.local());
            }
        }

        assertTrue(backend.released.isEmpty());
        assertEquals(Duration.ofSeconds(4), Duration.ofNanos(clock.get() - start));
    }

    @Test
    void closingTwiceReleasesOnce() throws Exception {
        backend.answers.add(granted("p4"));
        EuclidEgressLimiter.Permit permit = limiter().acquire("e.example.com");
        permit.close();
        permit.close();
        assertEquals(1, backend.released.size());
    }

    @Test
    void aRefusalIsReportedForEveryone() {
        limiter().throttled("F.example.com", Duration.ofSeconds(90));
        assertEquals(List.of("f.example.com/90"), backend.throttled);
    }

    @Test
    void retryAfterIsReadInBothItsForms() {
        assertEquals(Duration.ofSeconds(120), EuclidEgressLimiter.parseRetryAfter("120").orElseThrow());
        assertEquals(Duration.ZERO, EuclidEgressLimiter.parseRetryAfter("Wed, 21 Oct 2015 07:28:00 GMT").orElseThrow());
        assertTrue(EuclidEgressLimiter.parseRetryAfter("soon").isEmpty());
    }
}
