package de.jensvogt.euclid.auth;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test vectors taken verbatim from AWS's official published SigV4 test suite
 * (github.com/aws/aws-sig-v4-test-suite, credentials/date/region/service shared by all cases:
 * access key "AKIDEXAMPLE", secret "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", 20150830T123600Z,
 * us-east-1, service "service") - independent of euclid's own signature literals, so a bug shared
 * between sign() and verify() can't make these agree with each other while both being wrong
 * relative to the real algorithm.
 */
class SigV4Test {

    private static final String SECRET_ACCESS_KEY = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";
    private static final String DATE_STAMP = "20150830";
    private static final String AMZ_DATE = "20150830T123600Z";
    private static final String REGION = "us-east-1";
    private static final String SERVICE = "service";
    private static final String CREDENTIAL_SCOPE = "20150830/us-east-1/service/aws4_request";

    private static String sha256Hex(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHexLower(digest.digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] hmacSha256(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String toHexLower(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format(Locale.ROOT, "%02x", b));
        }
        return sb.toString();
    }

    private static String signatureFor(String canonicalRequest) {
        String stringToSign = SigV4.buildStringToSign(AMZ_DATE, CREDENTIAL_SCOPE, sha256Hex(canonicalRequest));
        byte[] signingKey = SigV4.deriveSigningKey(SECRET_ACCESS_KEY, DATE_STAMP, REGION, SERVICE);
        return toHexLower(hmacSha256(signingKey, stringToSign));
    }

    // get-vanilla: GET /, no query, only host/x-amz-date signed, empty body.
    @Test
    void getVanilla() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("host", "example.amazonaws.com");
        headers.put("x-amz-date", AMZ_DATE);
        List<String> signedHeaders = List.of("host", "x-amz-date");
        String payloadHash = sha256Hex("");

        String canonicalRequest = SigV4.buildCanonicalRequest("GET", "/", "", headers, signedHeaders, payloadHash);

        assertEquals("GET\n/\n\nhost:example.amazonaws.com\nx-amz-date:20150830T123600Z\n\nhost;x-amz-date\n"
                + "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", canonicalRequest);

        String stringToSign = SigV4.buildStringToSign(AMZ_DATE, CREDENTIAL_SCOPE, sha256Hex(canonicalRequest));
        assertEquals("AWS4-HMAC-SHA256\n20150830T123600Z\n20150830/us-east-1/service/aws4_request\n"
                + "bb579772317eb040ac9ed261061d46c1f17a8133879d6129b6e1c25292927e63", stringToSign);

        assertEquals("5fa00fa31553b73ebf1942676e86291e8372ff2a2260956d9b8aae1d763fbf31", signatureFor(canonicalRequest));
    }

    // get-vanilla-query-order-key-case: query parameters must be sorted alphabetically by key,
    // regardless of the order they appear in the request.
    @Test
    void getVanillaQueryOrderKeyCase() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("host", "example.amazonaws.com");
        headers.put("x-amz-date", AMZ_DATE);
        List<String> signedHeaders = List.of("host", "x-amz-date");
        String payloadHash = sha256Hex("");

        String canonicalQuery = SigV4.canonicalizeQueryString("Param2=value2&Param1=value1");
        assertEquals("Param1=value1&Param2=value2", canonicalQuery);

        String canonicalRequest = SigV4.buildCanonicalRequest("GET", "/", canonicalQuery, headers, signedHeaders, payloadHash);

        assertEquals("GET\n/\nParam1=value1&Param2=value2\nhost:example.amazonaws.com\nx-amz-date:20150830T123600Z\n\n"
                + "host;x-amz-date\ne3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", canonicalRequest);

        assertEquals("b97d918cfa904a5beff61c982a1b6f458b799221646efd99d3219ec94cdf2500", signatureFor(canonicalRequest));
    }

    // post-x-www-form-urlencoded: non-empty body and multiple signed headers, including one
    // (content-length) that isn't part of euclid's own fixed signed-header set -
    // buildCanonicalRequest takes the header list as a parameter precisely so it isn't tied to
    // that fixed set.
    @Test
    void postXWwwFormUrlencoded() {
        String body = "Param1=value1";
        assertEquals("9095672bbd1f56dfc5b65f3e153adc8731a4a654192329106275f4c7b24d0b6e", sha256Hex(body));

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("content-length", "13");
        headers.put("content-type", "application/x-www-form-urlencoded");
        headers.put("host", "example.amazonaws.com");
        headers.put("x-amz-date", AMZ_DATE);
        List<String> signedHeaders = List.of("content-length", "content-type", "host", "x-amz-date");

        String canonicalRequest = SigV4.buildCanonicalRequest("POST", "/", "", headers, signedHeaders, sha256Hex(body));

        assertEquals("POST\n/\n\ncontent-length:13\ncontent-type:application/x-www-form-urlencoded\n"
                + "host:example.amazonaws.com\nx-amz-date:20150830T123600Z\n\n"
                + "content-length;content-type;host;x-amz-date\n"
                + "9095672bbd1f56dfc5b65f3e153adc8731a4a654192329106275f4c7b24d0b6e", canonicalRequest);

        assertEquals("fec50118d90ecf934441dd37fb9a49bd7f5adb6450802ca3a0977623bbb7c27f", signatureFor(canonicalRequest));
    }

    // sign() followed by verify() over the same request must round-trip, and a request tampered
    // with after signing (either the body or a signed header) must be rejected - this is the
    // actual property the rest of the system relies on for MITM tamper detection.
    @Test
    void signThenVerifyRoundTrips() {
        String accessKeyId = "AKIDEXAMPLE";

        Function<String, Optional<String>> lookup = id -> id.equals(accessKeyId) ? Optional.of(SECRET_ACCESS_KEY) : Optional.empty();

        SignableRequest signed = buildRequest();
        SigV4.sign(signed, accessKeyId, SECRET_ACCESS_KEY, REGION, "sqs");

        Optional<SigV4.VerifyResult> verified = SigV4.verify(signed, lookup);
        assertTrue(verified.isPresent());
        assertEquals(accessKeyId, verified.get().accessKeyId());

        // Tampered body must invalidate the signature.
        SignableRequest tamperedBody = buildRequest();
        copyHeaders(signed, tamperedBody);
        tamperedBody.body("{\"queueUrl\":\"http://localhost/q\",\"messageBody\":\"pwned\"}");
        assertFalse(SigV4.verify(tamperedBody, lookup).isPresent());

        // Tampered routing header (a MITM re-targeting the request) must invalidate the signature too.
        SignableRequest tamperedTarget = buildRequest();
        copyHeaders(signed, tamperedTarget);
        tamperedTarget.header("x-euclid-target", "s3");
        assertFalse(SigV4.verify(tamperedTarget, lookup).isPresent());

        // An unknown access key ID must be rejected.
        SignableRequest unknownKey = buildRequest();
        SigV4.sign(unknownKey, "AKIDNOTFOUND", SECRET_ACCESS_KEY, REGION, "sqs");
        assertFalse(SigV4.verify(unknownKey, lookup).isPresent());
    }

    private static SignableRequest buildRequest() {
        SignableRequest req = new SignableRequest("POST", "/");
        req.header("host", "example.amazonaws.com");
        req.header("x-euclid-target", "sqs");
        req.header("x-euclid-action", "send-message");
        req.header("x-euclid-region", "eu-central-1");
        req.header("x-euclid-account-id", "863459426936");
        req.header("x-euclid-user-id", "alice");
        req.body("{\"queueUrl\":\"http://localhost/q\",\"messageBody\":\"hello\"}");
        return req;
    }

    private static void copyHeaders(SignableRequest from, SignableRequest to) {
        from.headers().forEach(to::header);
    }
}
