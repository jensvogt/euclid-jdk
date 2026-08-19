package de.jensvogt.euclid.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * AWS Signature Version 4 request signing and verification.
 * <p>
 * Ported from euclid's {@code Core::SigV4} (C++): unlike real AWS, the target service/action live
 * in custom "x-euclid-*" headers rather than the URI, so those headers are always part of the
 * signed set - a fixed, non-negotiable list rather than a client-chosen "SignedHeaders" list, so a
 * MITM can't downgrade which headers a signature actually covers.
 */
public final class SigV4 {

    private static final String ALGORITHM = "AWS4-HMAC-SHA256";
    private static final String SCOPE_TERMINATOR = "aws4_request";
    private static final Duration DEFAULT_MAX_SKEW = Duration.ofMinutes(15);
    private static final DateTimeFormatter AMZ_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.ROOT);

    /**
     * Headers that are always part of the signature, in the (already alphabetical) order the
     * canonical request requires.
     * <p>
     * host and x-amz-content-sha256/x-amz-date give the usual SigV4 transport/payload integrity;
     * the x-euclid-* headers are signed too because that's where euclid carries routing
     * information AWS would normally put in the URI.
     */
    private static final List<String> SIGNED_HEADER_NAMES = List.of(
            "host", "x-amz-content-sha256", "x-amz-date",
            "x-euclid-account-id", "x-euclid-action", "x-euclid-region", "x-euclid-target", "x-euclid-user-id");

    private SigV4() {
    }

    public static List<String> signedHeaderNames() {
        return SIGNED_HEADER_NAMES;
    }

    /**
     * The "&lt;accessKeyId&gt;/&lt;date&gt;/&lt;region&gt;/&lt;service&gt;/aws4_request" scope
     * parsed out of an Authorization header.
     */
    public record CredentialScope(String accessKeyId, String dateStamp, String region, String service) {
    }

    /**
     * Authorization header, split into its components.
     *
     * @param signedHeaders as literally presented, semicolon-joined
     * @param signature     lowercase hex
     */
    public record ParsedAuthorization(CredentialScope scope, String signedHeaders, String signature) {
    }

    /**
     * Result of a successful {@link #verify} call.
     */
    public record VerifyResult(String accessKeyId) {
    }

    /**
     * Parses "AWS4-HMAC-SHA256 Credential=..., SignedHeaders=..., Signature=...".
     *
     * @param headerValue raw value of the Authorization header.
     * @return the parsed components, or empty if the header isn't present/well-formed.
     */
    public static Optional<ParsedAuthorization> parseAuthorizationHeader(String headerValue) {
        if (headerValue == null || !headerValue.startsWith(ALGORITHM)) {
            return Optional.empty();
        }

        String rest = headerValue.substring(ALGORITHM.length()).trim();
        String credential = "", signedHeaders = "", signature = "";

        for (String part : split(rest, ',')) {
            String trimmed = part.trim();
            if (trimmed.startsWith("Credential=")) {
                credential = trimmed.substring("Credential=".length());
            } else if (trimmed.startsWith("SignedHeaders=")) {
                signedHeaders = trimmed.substring("SignedHeaders=".length());
            } else if (trimmed.startsWith("Signature=")) {
                signature = trimmed.substring("Signature=".length());
            }
        }
        if (credential.isEmpty() || signedHeaders.isEmpty() || signature.isEmpty()) {
            return Optional.empty();
        }

        List<String> scopeParts = split(credential, '/');
        if (scopeParts.size() != 5 || !scopeParts.get(4).equals(SCOPE_TERMINATOR)) {
            return Optional.empty();
        }

        CredentialScope scope = new CredentialScope(scopeParts.get(0), scopeParts.get(1), scopeParts.get(2), scopeParts.get(3));
        return Optional.of(new ParsedAuthorization(scope, signedHeaders, signature));
    }

    /**
     * Canonicalizes a raw query string: percent-decodes each name/value, re-encodes per SigV4's
     * URI-encoding rules, and sorts by name then value.
     *
     * @param rawQuery the query string as it appears on the wire (without the leading '?'), e.g.
     *                 "Param2=value2&amp;Param1=value1". Empty if the request has none.
     * @return the canonical query string, e.g. "Param1=value1&amp;Param2=value2".
     */
    public static String canonicalizeQueryString(String rawQuery) {
        if (rawQuery.isEmpty()) {
            return "";
        }

        List<String[]> params = new ArrayList<>();
        for (String pair : split(rawQuery, '&')) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String rawName = eq < 0 ? pair : pair.substring(0, eq);
            String rawValue = eq < 0 ? "" : pair.substring(eq + 1);
            params.add(new String[]{uriEncode(percentDecode(rawName), true), uriEncode(percentDecode(rawValue), true)});
        }
        params.sort((a, b) -> {
            int c = a[0].compareTo(b[0]);
            return c != 0 ? c : a[1].compareTo(b[1]);
        });

        StringBuilder out = new StringBuilder();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) {
                out.append('&');
            }
            out.append(params.get(i)[0]).append('=').append(params.get(i)[1]);
        }
        return out.toString();
    }

    /**
     * Builds the SigV4 canonical request string.
     *
     * @param method            HTTP method, e.g. "POST".
     * @param canonicalUri      URI-encoded path, e.g. "/".
     * @param canonicalQuery    canonical query string (sorted, encoded key=value&amp;...), empty if none.
     * @param headers           all request headers, keyed by lowercase name.
     * @param signedHeaderNames headers to include, already sorted (see {@link #signedHeaderNames()}).
     * @param payloadHashHex    lowercase hex SHA-256 of the request body.
     * @return the canonical request string.
     */
    public static String buildCanonicalRequest(String method, String canonicalUri, String canonicalQuery,
                                                 Map<String, String> headers, List<String> signedHeaderNames, String payloadHashHex) {
        StringBuilder canonicalHeaders = new StringBuilder();
        StringBuilder signedHeadersStr = new StringBuilder();
        for (int i = 0; i < signedHeaderNames.size(); i++) {
            String name = signedHeaderNames.get(i);
            canonicalHeaders.append(name).append(':').append(headers.getOrDefault(name, "")).append('\n');
            if (i > 0) {
                signedHeadersStr.append(';');
            }
            signedHeadersStr.append(name);
        }
        return method + "\n" + canonicalUri + "\n" + canonicalQuery + "\n" + canonicalHeaders + "\n" + signedHeadersStr + "\n" + payloadHashHex;
    }

    /**
     * Builds the SigV4 string-to-sign.
     *
     * @param amzDate                 full request timestamp, e.g. "20260818T120000Z".
     * @param credentialScope         "&lt;date&gt;/&lt;region&gt;/&lt;service&gt;/aws4_request".
     * @param canonicalRequestHashHex lowercase hex SHA-256 of the canonical request.
     */
    public static String buildStringToSign(String amzDate, String credentialScope, String canonicalRequestHashHex) {
        return ALGORITHM + "\n" + amzDate + "\n" + credentialScope + "\n" + canonicalRequestHashHex;
    }

    /**
     * Derives the SigV4 signing key via the kDate -&gt; kRegion -&gt; kService -&gt; kSigning
     * HMAC-SHA256 chain.
     *
     * @param secretAccessKey the caller's secret.
     * @param dateStamp       "YYYYMMDD".
     * @param region          e.g. "eu-central-1".
     * @param service         e.g. "sqs".
     * @return the derived signing key (raw bytes).
     */
    public static byte[] deriveSigningKey(String secretAccessKey, String dateStamp, String region, String service) {
        byte[] kDate = hmacSha256(("AWS4" + secretAccessKey).getBytes(StandardCharsets.UTF_8), dateStamp);
        byte[] kRegion = hmacSha256(kDate, region);
        byte[] kService = hmacSha256(kRegion, service);
        return hmacSha256(kService, SCOPE_TERMINATOR);
    }

    /**
     * Signs a request in place: sets x-amz-date, x-amz-content-sha256 and Authorization.
     * <p>
     * Call after every other header the signature must cover (x-euclid-target, x-euclid-action,
     * x-euclid-region, x-euclid-account-id, x-euclid-user-id, host) and the body are already set
     * on req.
     *
     * @param req             request to sign; mutated in place.
     * @param accessKeyId     the caller's access key ID.
     * @param secretAccessKey the caller's secret access key.
     * @param region          region to scope the signature to, e.g. "eu-central-1".
     * @param service         service to scope the signature to, e.g. "sqs".
     */
    public static void sign(SignableRequest req, String accessKeyId, String secretAccessKey, String region, String service) {
        Instant now = Instant.now();
        String amzDate = AMZ_DATE_FORMAT.withZone(ZoneOffset.UTC).format(now);
        String dateStamp = amzDate.substring(0, 8);

        req.header("x-amz-date", amzDate);
        req.header("x-amz-content-sha256", sha256Hex(req.body()));

        String target = req.target();
        int q = target.indexOf('?');
        String canonicalUri = q < 0 ? target : target.substring(0, q);
        String canonicalQuery = q < 0 ? "" : canonicalizeQueryString(target.substring(q + 1));

        String canonicalRequest = buildCanonicalRequest(req.method(), canonicalUri, canonicalQuery,
                req.headers(), signedHeaderNames(), req.header("x-amz-content-sha256"));

        String credentialScope = dateStamp + "/" + region + "/" + service + "/" + SCOPE_TERMINATOR;
        String stringToSign = buildStringToSign(amzDate, credentialScope, sha256Hex(canonicalRequest));

        byte[] signingKey = deriveSigningKey(secretAccessKey, dateStamp, region, service);
        String signature = toHexLower(hmacSha256(signingKey, stringToSign));

        String signedHeadersStr = String.join(";", signedHeaderNames());

        req.header("Authorization", ALGORITHM + " Credential=" + accessKeyId + "/" + credentialScope +
                ", SignedHeaders=" + signedHeadersStr + ", Signature=" + signature);
    }

    /**
     * Verifies a SigV4-signed request with the default 15-minute clock-skew tolerance.
     *
     * @see #verify(SignableRequest, Function, Duration)
     */
    public static Optional<VerifyResult> verify(SignableRequest req, Function<String, Optional<String>> lookupSecret) {
        return verify(req, lookupSecret, DEFAULT_MAX_SKEW);
    }

    /**
     * Verifies a SigV4-signed request.
     * <p>
     * Recomputes the signature from the request exactly as received and compares it
     * (constant-time) against the one presented in Authorization - any change to the signed
     * headers or body between signing and here makes this fail.
     *
     * @param req          the request to verify.
     * @param lookupSecret resolves an access key ID to its secret, or empty if unknown.
     * @param maxSkew      maximum age (in either direction) x-amz-date may have relative to now
     *                     before the request is rejected as stale/replayed.
     * @return the resolved access key ID on success, empty on any failure (missing/malformed
     * header, unknown key, stale timestamp, or signature mismatch) - callers don't get to
     * distinguish which, mirroring how JWT verification collapses failure modes into a single
     * rejection.
     */
    public static Optional<VerifyResult> verify(SignableRequest req, Function<String, Optional<String>> lookupSecret, Duration maxSkew) {
        Optional<ParsedAuthorization> parsedOpt = parseAuthorizationHeader(req.header("authorization"));
        if (parsedOpt.isEmpty()) {
            return Optional.empty();
        }
        ParsedAuthorization parsed = parsedOpt.get();

        // Fixed policy, not client-negotiated: reject anything that doesn't cover exactly the
        // headers sign() always signs - see the class comment for why this can't be a
        // client-chosen list here the way real AWS allows.
        String expectedSignedHeaders = String.join(";", signedHeaderNames());
        if (!parsed.signedHeaders().equals(expectedSignedHeaders)) {
            return Optional.empty();
        }

        Optional<String> secretOpt = lookupSecret.apply(parsed.scope().accessKeyId());
        if (secretOpt.isEmpty()) {
            return Optional.empty();
        }

        Map<String, String> headers = req.headers();
        String amzDate = headers.get("x-amz-date");
        if (amzDate == null || amzDate.length() < 8) {
            return Optional.empty();
        }
        if (!amzDate.substring(0, 8).equals(parsed.scope().dateStamp())) {
            return Optional.empty();
        }

        Optional<Instant> requestTime = parseAmzDate(amzDate);
        if (requestTime.isEmpty()) {
            return Optional.empty();
        }
        if (Duration.between(requestTime.get(), Instant.now()).abs().compareTo(maxSkew) > 0) {
            return Optional.empty();
        }

        String payloadHash = headers.get("x-amz-content-sha256");
        if (payloadHash == null || !payloadHash.equals(sha256Hex(req.body()))) {
            return Optional.empty();
        }

        String target = req.target();
        int q = target.indexOf('?');
        String canonicalUri = q < 0 ? target : target.substring(0, q);
        String canonicalQuery = q < 0 ? "" : canonicalizeQueryString(target.substring(q + 1));

        String canonicalRequest = buildCanonicalRequest(req.method(), canonicalUri, canonicalQuery,
                headers, signedHeaderNames(), payloadHash);

        String credentialScope = parsed.scope().dateStamp() + "/" + parsed.scope().region() + "/" + parsed.scope().service() + "/" + SCOPE_TERMINATOR;
        String stringToSign = buildStringToSign(amzDate, credentialScope, sha256Hex(canonicalRequest));

        byte[] signingKey = deriveSigningKey(secretOpt.get(), parsed.scope().dateStamp(), parsed.scope().region(), parsed.scope().service());
        String expectedSignature = toHexLower(hmacSha256(signingKey, stringToSign));

        if (!constantTimeEquals(expectedSignature, parsed.signature())) {
            return Optional.empty();
        }

        return Optional.of(new VerifyResult(parsed.scope().accessKeyId()));
    }

    // --- internal helpers -------------------------------------------------------------------

    private static Optional<Instant> parseAmzDate(String amzDate) {
        try {
            return Optional.of(LocalDateTime.parse(amzDate, AMZ_DATE_FORMAT).toInstant(ZoneOffset.UTC));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static List<String> split(String s, char delim) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start <= s.length()) {
            int pos = s.indexOf(delim, start);
            if (pos < 0) {
                parts.add(s.substring(start));
                break;
            }
            parts.add(s.substring(start, pos));
            start = pos + 1;
        }
        return parts;
    }

    // Whether c (a single byte, 0-255) is one of SigV4's unreserved characters (RFC 3986
    // unreserved set) - the only characters left un-percent-encoded.
    private static boolean isUnreserved(int c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                || c == '-' || c == '_' || c == '.' || c == '~';
    }

    // SigV4's URI-encoding: percent-encode everything except unreserved characters, with
    // uppercase hex digits. '/' is kept literal in the path (encodeSlash=false) but treated like
    // any other character in query keys/values (encodeSlash=true). Operates byte-wise (post-UTF-8
    // encoding) so multi-byte characters are percent-encoded per byte, as SigV4 requires.
    private static String uriEncode(byte[] value, boolean encodeSlash) {
        StringBuilder out = new StringBuilder(value.length);
        for (byte raw : value) {
            int c = raw & 0xFF;
            if (isUnreserved(c) || (c == '/' && !encodeSlash)) {
                out.append((char) c);
            } else {
                out.append('%').append(String.format(Locale.ROOT, "%02X", c));
            }
        }
        return out.toString();
    }

    private static byte[] percentDecode(String value) {
        byte[] out = new byte[value.length()];
        int len = 0;
        int i = 0;
        while (i < value.length()) {
            char c = value.charAt(i);
            if (c == '%' && i + 2 < value.length() && isHexDigit(value.charAt(i + 1)) && isHexDigit(value.charAt(i + 2))) {
                out[len++] = (byte) Integer.parseInt(value.substring(i + 1, i + 3), 16);
                i += 3;
            } else {
                out[len++] = (byte) c;
                i += 1;
            }
        }
        byte[] trimmed = new byte[len];
        System.arraycopy(out, 0, trimmed, 0, len);
        return trimmed;
    }

    private static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private static String sha256Hex(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHexLower(digest.digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static byte[] hmacSha256(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 not available", e);
        }
    }

    private static String toHexLower(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format(Locale.ROOT, "%02x", b));
        }
        return sb.toString();
    }
}
