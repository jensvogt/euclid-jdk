package de.jensvogt.euclid.auth;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The first two tests use the vectors published in RFC 9421 §B.2.5 and RFC 9530 §3.1 - the
 * signature base, the HMAC over it and the Content-Digest of the RFC's own example request - so
 * that a mistake shared between {@link Rfc9421#sign} and {@link Rfc9421#verify} cannot make them
 * agree with each other while both disagree with the standard. The rest exercise euclid's own
 * policy on top of it, which no published vector covers.
 */
class Rfc9421Test {

    /**
     * The shared secret of RFC 9421 §B.1.4, base64-encoded as the RFC prints it.
     */
    private static final String RFC_SHARED_SECRET =
            "uzvJfB4u3N0Jy4T7NZ75MDVcr8zSTInedJtkgcu46YW4XByzNJjxBdtjUkdJPBtbmHhIDi6pcl8jsasjlTMtDQ==";

    private static final String ACCESS_KEY_ID = "AKIDEXAMPLE";
    private static final String SECRET_ACCESS_KEY = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";

    private static final Function<String, Optional<String>> LOOKUP =
            id -> id.equals(ACCESS_KEY_ID) ? Optional.of(SECRET_ACCESS_KEY) : Optional.empty();

    private static byte[] hmacSha256(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * The request of RFC 9421 §B.2, as far as the signed components reach.
     */
    private static SignableRequest rfcExampleRequest() {
        return new SignableRequest("POST", "/foo?param=Value&Pet=dog")
                .scheme("http")
                .header("host", "example.com")
                .header("date", "Tue, 20 Apr 2021 02:07:55 GMT")
                .header("content-type", "application/json")
                .body("{\"hello\": \"world\"}");
    }

    private static SignableRequest euclidRequest() {
        return new SignableRequest("POST", "/")
                .header("host", "euclid.example.com")
                .header("content-type", "application/json")
                .header("x-euclid-target", "eqs")
                .header("x-euclid-action", "ListQueues")
                .header("x-euclid-region", "eu-central-1")
                .header("x-euclid-account-id", "863459426936")
                .header("x-euclid-user-id", "alice")
                .body("{\"pageSize\":10}");
    }

    @Test
    void signatureBaseMatchesRfcExample() {
        String params = "(\"date\" \"@authority\" \"content-type\");created=1618884473;keyid=\"test-shared-secret\"";

        String base = Rfc9421.signatureBase(rfcExampleRequest(), List.of("date", "@authority", "content-type"), params)
                .orElseThrow();

        assertEquals("""
                "date": Tue, 20 Apr 2021 02:07:55 GMT
                "@authority": example.com
                "content-type": application/json
                "@signature-params": ("date" "@authority" "content-type");created=1618884473;keyid="test-shared-secret\"""",
                base);
    }

    @Test
    void hmacOverRfcSignatureBaseMatchesRfcSignature() {
        String params = "(\"date\" \"@authority\" \"content-type\");created=1618884473;keyid=\"test-shared-secret\"";
        String base = Rfc9421.signatureBase(rfcExampleRequest(), List.of("date", "@authority", "content-type"), params)
                .orElseThrow();

        byte[] signature = hmacSha256(Base64.getDecoder().decode(RFC_SHARED_SECRET), base);

        assertEquals("pxcQw6G3AjtMBQjwo8XzkZf/bws5LelbaMk5rGIGtE8=", Base64.getEncoder().encodeToString(signature));
    }

    @Test
    void contentDigestMatchesRfc9530Example() {
        assertEquals("sha-256=:X48E9qOokqqrvdts8nOJRJN3OWDUoyWxBf7kbu9DBPE=:", Rfc9421.contentDigest("{\"hello\": \"world\"}"));
    }

    @Test
    void derivedComponentsFollowTheRfcsNormalization() {
        SignableRequest req = new SignableRequest("post", "/foo?param=Value&Pet=dog")
                .scheme("https")
                .header("host", "Example.COM:443");

        String base = Rfc9421.signatureBase(req, List.of("@method", "@authority", "@path", "@query"), "()").orElseThrow();

        assertEquals("""
                "@method": POST
                "@authority": example.com
                "@path": /foo
                "@query": ?param=Value&Pet=dog
                "@signature-params": ()""", base);
    }

    @Test
    void queryIsASingleQuestionMarkWhenTheRequestHasNone() {
        SignableRequest req = new SignableRequest("POST", "/").header("host", "example.com");

        String base = Rfc9421.signatureBase(req, List.of("@path", "@query"), "()").orElseThrow();

        assertEquals("\"@path\": /\n\"@query\": ?\n\"@signature-params\": ()", base);
    }

    @Test
    void portIsKeptWhenItIsNotTheSchemesDefault() {
        SignableRequest req = new SignableRequest("POST", "/").scheme("https").header("host", "example.com:8443");

        assertEquals("\"@authority\": example.com:8443\n\"@signature-params\": ()",
                Rfc9421.signatureBase(req, List.of("@authority"), "()").orElseThrow());
    }

    @Test
    void signedRequestVerifies() {
        SignableRequest req = euclidRequest();

        Rfc9421.sign(req, ACCESS_KEY_ID, SECRET_ACCESS_KEY);

        assertTrue(req.header("signature-input").startsWith("sig1=(\"@method\" \"@authority\" \"@path\" \"@query\" \"content-digest\""));
        assertTrue(req.header("signature-input").contains(";keyid=\"" + ACCESS_KEY_ID + "\""));
        assertTrue(req.header("signature-input").contains(";alg=\"hmac-sha256\""));
        assertTrue(req.header("signature-input").contains(";tag=\"euclid\""));
        assertTrue(req.header("signature").startsWith("sig1=:"));
        assertEquals(Rfc9421.contentDigest("{\"pageSize\":10}"), req.header("content-digest"));
        // The scheme leaves Authorization alone, so a token can ride alongside during a migration.
        assertEquals("", req.header("authorization"));

        Optional<Rfc9421.VerifyResult> result = Rfc9421.verify(req, LOOKUP);

        assertTrue(result.isPresent());
        assertEquals(ACCESS_KEY_ID, result.get().keyId());
    }

    @Test
    void signaturesCoverOnlyTheRoutingHeadersTheRequestCarries() {
        SignableRequest full = euclidRequest();
        SignableRequest sparse = new SignableRequest("POST", "/")
                .header("host", "euclid.example.com")
                .header("x-euclid-target", "eqs")
                .header("x-euclid-action", "ListQueues");

        assertEquals(List.of("@method", "@authority", "@path", "@query", "content-digest", "x-euclid-action",
                "x-euclid-target", "x-euclid-account-id", "x-euclid-region", "x-euclid-user-id"),
                Rfc9421.coveredComponents(full));
        assertEquals(List.of("@method", "@authority", "@path", "@query", "content-digest", "x-euclid-action",
                "x-euclid-target"), Rfc9421.coveredComponents(sparse));

        Rfc9421.sign(sparse, ACCESS_KEY_ID, SECRET_ACCESS_KEY);
        assertTrue(Rfc9421.verify(sparse, LOOKUP).isPresent());
    }

    @Test
    void tamperingWithTheBodyFails() {
        SignableRequest req = euclidRequest();
        Rfc9421.sign(req, ACCESS_KEY_ID, SECRET_ACCESS_KEY);

        req.body("{\"pageSize\":1000}");

        assertFalse(Rfc9421.verify(req, LOOKUP).isPresent());
    }

    @Test
    void tamperingWithADigestToMatchATamperedBodyStillFails() {
        SignableRequest req = euclidRequest();
        Rfc9421.sign(req, ACCESS_KEY_ID, SECRET_ACCESS_KEY);

        // Content-Digest is itself covered, so repairing it does not repair the signature.
        req.body("{\"pageSize\":1000}").header("Content-Digest", Rfc9421.contentDigest("{\"pageSize\":1000}"));

        assertFalse(Rfc9421.verify(req, LOOKUP).isPresent());
    }

    @Test
    void tamperingWithTheTargetFails() {
        SignableRequest signed = euclidRequest();
        Rfc9421.sign(signed, ACCESS_KEY_ID, SECRET_ACCESS_KEY);

        SignableRequest moved = new SignableRequest("POST", "/admin");
        signed.headers().forEach(moved::header);
        moved.body(signed.body());

        assertFalse(Rfc9421.verify(moved, LOOKUP).isPresent());
    }

    @Test
    void tamperingWithARoutingHeaderFails() {
        SignableRequest req = euclidRequest();
        Rfc9421.sign(req, ACCESS_KEY_ID, SECRET_ACCESS_KEY);

        req.header("x-euclid-user-id", "root");

        assertFalse(Rfc9421.verify(req, LOOKUP).isPresent());
    }

    @Test
    void addingARoutingHeaderTheSignerNeverCoveredFails() {
        SignableRequest req = new SignableRequest("POST", "/")
                .header("host", "euclid.example.com")
                .header("x-euclid-target", "eqs")
                .header("x-euclid-action", "ListQueues")
                .body("{}");
        Rfc9421.sign(req, ACCESS_KEY_ID, SECRET_ACCESS_KEY);

        req.header("x-euclid-namespace", "someone-elses");

        assertFalse(Rfc9421.verify(req, LOOKUP).isPresent());
    }

    @Test
    void removingASignedRoutingHeaderFails() {
        SignableRequest signed = euclidRequest();
        Rfc9421.sign(signed, ACCESS_KEY_ID, SECRET_ACCESS_KEY);

        SignableRequest stripped = new SignableRequest("POST", "/").body(signed.body());
        signed.headers().forEach((name, value) -> {
            if (!name.equals("x-euclid-user-id")) {
                stripped.header(name, value);
            }
        });

        assertFalse(Rfc9421.verify(stripped, LOOKUP).isPresent());
    }

    @Test
    void unknownKeyFails() {
        SignableRequest req = euclidRequest();
        Rfc9421.sign(req, "AKIDNOTFOUND", SECRET_ACCESS_KEY);

        assertFalse(Rfc9421.verify(req, LOOKUP).isPresent());
    }

    @Test
    void staleSignatureFails() {
        SignableRequest req = euclidRequest();
        signAt(req, Instant.now().minus(Duration.ofMinutes(20)));

        assertFalse(Rfc9421.verify(req, LOOKUP).isPresent());
        assertTrue(Rfc9421.verify(req, LOOKUP, Duration.ofMinutes(30)).isPresent(),
                "a wider window is the only thing that should have been rejecting it");
    }

    @Test
    void signatureFromTheFutureFails() {
        SignableRequest req = euclidRequest();
        signAt(req, Instant.now().plus(Duration.ofMinutes(20)));

        assertFalse(Rfc9421.verify(req, LOOKUP).isPresent());
    }

    @Test
    void expiredSignatureFails() {
        SignableRequest req = euclidRequest();
        String params = Rfc9421.serializeSignatureParams(Rfc9421.coveredComponents(withDigest(req)), Instant.now(),
                ACCESS_KEY_ID, "nonce") + ";expires=" + Instant.now().minusSeconds(30).getEpochSecond();
        applySignature(req, params);

        assertFalse(Rfc9421.verify(req, LOOKUP).isPresent());
    }

    @Test
    void aSignatureUnderADifferentAlgorithmFails() {
        SignableRequest req = euclidRequest();
        withDigest(req);
        String components = componentList(Rfc9421.coveredComponents(req));
        applySignature(req, components + ";created=" + Instant.now().getEpochSecond()
                + ";keyid=\"" + ACCESS_KEY_ID + "\";alg=\"rsa-pss-sha512\";tag=\"euclid\"");

        assertFalse(Rfc9421.verify(req, LOOKUP).isPresent(),
                "an alg euclid does not implement must be a rejection, never a fallback");
    }

    @Test
    void aSignatureMadeForAnotherApplicationFails() {
        SignableRequest req = euclidRequest();
        withDigest(req);
        String components = componentList(Rfc9421.coveredComponents(req));
        applySignature(req, components + ";created=" + Instant.now().getEpochSecond()
                + ";keyid=\"" + ACCESS_KEY_ID + "\";alg=\"hmac-sha256\";tag=\"not-euclid\"");

        assertFalse(Rfc9421.verify(req, LOOKUP).isPresent());
    }

    @Test
    void aSignatureCoveringLessThanEuclidRequiresFails() {
        SignableRequest req = euclidRequest();
        withDigest(req);
        // Everything but @authority, i.e. a signature that would replay against any host.
        List<String> narrowed = Rfc9421.coveredComponents(req).stream().filter(c -> !c.equals("@authority")).toList();
        applySignature(req, componentList(narrowed) + ";created=" + Instant.now().getEpochSecond()
                + ";keyid=\"" + ACCESS_KEY_ID + "\";alg=\"hmac-sha256\";tag=\"euclid\"");

        assertFalse(Rfc9421.verify(req, LOOKUP).isPresent());
    }

    @Test
    void severalSignaturesAreRefusedRatherThanChosenBetween() {
        SignableRequest req = euclidRequest();
        Rfc9421.sign(req, ACCESS_KEY_ID, SECRET_ACCESS_KEY);

        req.header("Signature-Input", req.header("signature-input") + ", proxy=(\"@method\");created=1618884473;keyid=\"p\"");

        assertFalse(Rfc9421.verify(req, LOOKUP).isPresent());
    }

    @Test
    void mismatchedLabelsBetweenTheTwoHeadersFail() {
        SignableRequest req = euclidRequest();
        Rfc9421.sign(req, ACCESS_KEY_ID, SECRET_ACCESS_KEY);

        req.header("Signature", "other=" + req.header("signature").substring("sig1=".length()));

        assertFalse(Rfc9421.verify(req, LOOKUP).isPresent());
    }

    @Test
    void malformedHeadersAreRejectedRatherThanThrowing() {
        for (String signatureInput : List.of("", "sig1", "sig1=", "=()", "sig1=(\"@method\"", "sig1=(\"@method\");created",
                "sig1=(\"@method\");created=notanumber;keyid=\"k\"", "sig1=(\"@method\");keyid=k", "sig1=()",
                "sig1=(\"@method\";req);created=1;keyid=\"k\"")) {
            SignableRequest req = euclidRequest();
            req.header("Content-Digest", Rfc9421.contentDigest(req.body()));
            req.header("Signature-Input", signatureInput);
            req.header("Signature", "sig1=:YWJj:");

            assertFalse(Rfc9421.verify(req, LOOKUP).isPresent(), "should reject: " + signatureInput);
        }
    }

    @Test
    void aRequestWithNoSignatureAtAllIsRejected() {
        assertFalse(Rfc9421.verify(euclidRequest(), LOOKUP).isPresent());
    }

    @Test
    void signingARequestMissingACoveredHeaderIsAProgrammingError() {
        SignableRequest req = new SignableRequest("POST", "/").header("host", "euclid.example.com").body("{}");

        // RFC 9421 forbids signing over an absent field, so this cannot silently sign an empty
        // value the way SigV4 does - x-euclid-target/action are missing.
        try {
            Rfc9421.sign(req, ACCESS_KEY_ID, SECRET_ACCESS_KEY);
            throw new AssertionError("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("x-euclid-target"));
        }
    }

    @Test
    void nonceDiffersBetweenSignaturesOfTheSameRequest() {
        SignableRequest first = euclidRequest();
        SignableRequest second = euclidRequest();

        Rfc9421.sign(first, ACCESS_KEY_ID, SECRET_ACCESS_KEY);
        Rfc9421.sign(second, ACCESS_KEY_ID, SECRET_ACCESS_KEY);

        assertNotEquals(first.header("signature"), second.header("signature"));
    }

    @Test
    void schemeOfIdentifiesWhichSignatureARequestPresents() {
        SignableRequest rfc = euclidRequest();
        Rfc9421.sign(rfc, ACCESS_KEY_ID, SECRET_ACCESS_KEY);
        SignableRequest sigV4 = euclidRequest();
        SigV4.sign(sigV4, ACCESS_KEY_ID, SECRET_ACCESS_KEY, "eu-central-1", "eqs");

        assertEquals(Optional.of(SigningScheme.RFC9421), SigningScheme.of(rfc));
        assertEquals(Optional.of(SigningScheme.SIGV4), SigningScheme.of(sigV4));
        assertEquals(Optional.empty(), SigningScheme.of(euclidRequest()));
    }

    @Test
    void bothSchemesVerifyThroughTheEnum() {
        for (SigningScheme scheme : SigningScheme.values()) {
            SignableRequest req = euclidRequest();
            scheme.sign(req, ACCESS_KEY_ID, SECRET_ACCESS_KEY, "eu-central-1", "eqs");

            assertEquals(Optional.of(ACCESS_KEY_ID), scheme.verify(req, LOOKUP), scheme.name());
            for (String header : scheme.signatureHeaderNames()) {
                assertFalse(req.header(header).isEmpty(), scheme + " should have set " + header);
            }
        }
    }

    // --- helpers ----------------------------------------------------------------------------

    private static SignableRequest withDigest(SignableRequest req) {
        return req.header("Content-Digest", Rfc9421.contentDigest(req.body()));
    }

    private static String componentList(List<String> components) {
        return components.stream().map(c -> '"' + c + '"').reduce((a, b) -> a + " " + b).map(s -> "(" + s + ")").orElse("()");
    }

    /**
     * Signs as {@link Rfc9421#sign} does but with a chosen creation time, which the public API
     * deliberately does not let a caller pick.
     */
    private static void signAt(SignableRequest req, Instant created) {
        withDigest(req);
        applySignature(req, Rfc9421.serializeSignatureParams(Rfc9421.coveredComponents(req), created, ACCESS_KEY_ID, "nonce"));
    }

    /**
     * Computes and attaches a valid signature over {@code params}, so that a test can vary one
     * parameter and know the signature itself is not what fails.
     */
    private static void applySignature(SignableRequest req, String params) {
        String base = Rfc9421.signatureBase(req, Rfc9421.parseSignatureParams(params).orElseThrow().components(), params)
                .orElseThrow();
        req.header("Signature-Input", "sig1=" + params);
        req.header("Signature", "sig1=:" + Base64.getEncoder()
                .encodeToString(hmacSha256(SECRET_ACCESS_KEY.getBytes(StandardCharsets.UTF_8), base)) + ":");
    }
}
