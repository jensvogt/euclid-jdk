package de.jensvogt.euclid.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * RFC 9421 HTTP Message Signatures, the scheme meant to take over from {@link SigV4}.
 * <p>
 * Same credentials, same threat model, standard wire format: the caller still presents an access
 * key ID and its secret, and the signature is still an HMAC-SHA256 over a canonical rendering of
 * the request, but what gets rendered and how it is carried is the IETF's rather than AWS's. The
 * signature travels in {@code Signature} and {@code Signature-Input} rather than
 * {@code Authorization}, which is what makes the two schemes able to coexist during a migration:
 * {@link SigV4} owns {@code Authorization}, this owns its own headers, and a server can accept
 * either without having to guess which one a request meant to use.
 * <p>
 * <b>Algorithm.</b> Only {@code hmac-sha256} is implemented, because that is the algorithm that
 * takes euclid's existing access-key/secret pairs unchanged - migrating is a change of wire format,
 * not of credential store. The HMAC key is the secret's UTF-8 bytes directly, as RFC 9421 §3.3.3
 * specifies; there is no {@link SigV4#deriveSigningKey key derivation chain} to reproduce, so any
 * conforming implementation on the other end interoperates. Adding an asymmetric algorithm later is
 * a matter of another branch in {@link #sign} and {@link #verify}, as {@code alg} is already
 * carried and checked.
 * <p>
 * <b>Covered components.</b> RFC 9421 lets a signer choose what its signature covers and tell the
 * verifier in {@code Signature-Input}. Taken literally that would let a request decide how little
 * of itself to authenticate, so this class does not take it literally: {@link #verify} derives the
 * component list the request <em>must</em> have covered from the request itself
 * ({@link #coveredComponents}) and rejects any signature covering anything else. Stripping
 * {@code x-euclid-user-id}, or adding one the signer never saw, changes that derived list and fails
 * before the HMAC is even computed - the same fixed-policy stance {@link SigV4} takes with its
 * {@code SignedHeaders} list, arrived at differently because RFC 9421's component list is genuinely
 * variable where euclid's headers are optional.
 * <p>
 * <b>Replay.</b> Each signature carries a {@code created} timestamp, checked here against a skew
 * window, and a random {@code nonce}, which is not checked here - single-use enforcement needs
 * state across requests that a static method has no business holding. A server that wants it should
 * remember the nonces it has seen for the length of the skew window.
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc9421.html">RFC 9421</a>
 * @see <a href="https://www.rfc-editor.org/rfc/rfc9530.html">RFC 9530 (Content-Digest)</a>
 */
public final class Rfc9421 {

    /**
     * The only signature algorithm this class implements, as it appears in the {@code alg}
     * parameter.
     */
    public static final String ALGORITHM = "hmac-sha256";

    /**
     * The label {@link #sign} gives its signature in the {@code Signature} and
     * {@code Signature-Input} dictionaries. Verification accepts whatever single label it is given,
     * so a peer using a different one still interoperates.
     */
    private static final String LABEL = "sig1";

    /**
     * Value of the {@code tag} parameter (RFC 9421 §2.3), naming the application a signature was
     * created for. Binding it stops a signature made for some other service that happens to share
     * euclid's credentials from being replayed at euclid.
     */
    private static final String TAG = "euclid";

    /**
     * How far {@code created} may sit from the verifier's clock, in either direction, before a
     * request is rejected as stale or replayed. Matches {@link SigV4}'s window.
     */
    private static final Duration DEFAULT_MAX_SKEW = Duration.ofMinutes(15);

    /**
     * Components every euclid signature covers, in signing order.
     * <p>
     * The derived components pin the request line and host, {@code content-digest} pins the body,
     * and the two {@code x-euclid-*} headers pin what the request is asking for - which for euclid
     * lives in headers rather than in the URI, so a signature that left them out would authenticate
     * a request without authenticating what it does.
     */
    private static final List<String> REQUIRED_COMPONENTS = List.of(
            "@method", "@authority", "@path", "@query", "content-digest",
            "x-euclid-action", "x-euclid-target");

    /**
     * Headers covered whenever the request carries them, in signing order.
     * <p>
     * These are the routing headers a client sets or leaves out depending on how it was
     * configured. RFC 9421 forbids signing a field that is not there, so they cannot simply join
     * {@link #REQUIRED_COMPONENTS} the way {@link SigV4} handles an absent header by signing an
     * empty value; instead each is covered exactly when present, and {@link #verify} reconstructs
     * the same decision from the received message.
     */
    private static final List<String> OPTIONAL_COMPONENTS = List.of(
            "x-euclid-account-id", "x-euclid-namespace", "x-euclid-region", "x-euclid-user-id");

    /**
     * Headers {@link #sign} writes onto a request, in the order it writes them.
     */
    private static final List<String> SIGNATURE_HEADER_NAMES = List.of("Content-Digest", "Signature-Input", "Signature");

    private static final SecureRandom RANDOM = new SecureRandom();

    private Rfc9421() {
    }

    /**
     * The components every signature covers regardless of the request.
     *
     * @return the required component identifiers, lowercase, in signing order.
     */
    public static List<String> requiredComponents() {
        return REQUIRED_COMPONENTS;
    }

    /**
     * The headers covered only when the request carries them.
     *
     * @return the optional component identifiers, lowercase, in signing order.
     */
    public static List<String> optionalComponents() {
        return OPTIONAL_COMPONENTS;
    }

    /**
     * The headers {@link #sign} adds to a request, for callers that copy the signed headers onto
     * some other request object afterwards.
     *
     * @return the header names, in the case they should be sent in.
     */
    public static List<String> signatureHeaderNames() {
        return SIGNATURE_HEADER_NAMES;
    }

    /**
     * The parameters of one signature, as carried in {@code Signature-Input}.
     *
     * @param components the covered component identifiers, in the order they are signed in
     * @param created    when the signature was created
     * @param expires    when it stops being valid, or {@code null} if it carries no expiry
     * @param keyId      identifies the key that signed, i.e. euclid's access key ID
     * @param algorithm  the {@code alg} parameter, or {@code null} if the signature omits it
     * @param nonce      the signature's nonce, or {@code null} if it carries none
     * @param tag        the application tag, or {@code null} if the signature carries none
     * @param raw        the parameters exactly as received, which is what the signature is computed
     *                   over - re-serializing these fields would not reliably reproduce it
     */
    public record SignatureParams(List<String> components, Instant created, Instant expires, String keyId,
                                  String algorithm, String nonce, String tag, String raw) {
    }

    /**
     * A signature and its parameters, paired by their shared dictionary label.
     *
     * @param label     the dictionary key both headers used
     * @param params    the parsed {@code Signature-Input} value
     * @param signature the raw signature bytes from {@code Signature}
     */
    public record ParsedSignature(String label, SignatureParams params, byte[] signature) {
    }

    /**
     * Result of a successful {@link #verify} call.
     *
     * @param keyId the access key ID whose signature was verified
     */
    public record VerifyResult(String keyId) {
    }

    /**
     * Computes an RFC 9530 {@code Content-Digest} value for a body.
     *
     * @param body the request body; empty for a request that has none.
     * @return the header value, e.g. {@code sha-256=:X48E9qOokqqrvdts8nOJRJN3OWDUoyWxBf7kbu9DBPE=:}.
     */
    public static String contentDigest(String body) {
        return "sha-256=:" + Base64.getEncoder().encodeToString(sha256(body)) + ":";
    }

    /**
     * The components a signature over this request must cover: every required component, plus each
     * optional header the request actually carries.
     * <p>
     * Both sides call this - the signer to decide what to cover, the verifier to decide what it
     * insists was covered - which is what makes adding or removing a routing header detectable
     * rather than merely unsigned.
     *
     * @param req the request being signed or verified.
     * @return the component identifiers, in signing order.
     */
    public static List<String> coveredComponents(SignableRequest req) {
        List<String> components = new ArrayList<>(REQUIRED_COMPONENTS);
        for (String name : OPTIONAL_COMPONENTS) {
            if (req.headers().containsKey(name)) {
                components.add(name);
            }
        }
        return components;
    }

    /**
     * Builds the RFC 9421 §2.5 signature base: one line per covered component, then the
     * {@code @signature-params} line, which carries no trailing newline.
     *
     * @param req             the request the component values are read from.
     * @param components      the covered component identifiers, in signing order.
     * @param signatureParams the serialized signature parameters, i.e. the {@code Signature-Input}
     *                        value with its label stripped.
     * @return the signature base, or empty if any covered component is absent from the request -
     * RFC 9421 §2.1 forbids signing over a field that is not there.
     */
    public static Optional<String> signatureBase(SignableRequest req, List<String> components, String signatureParams) {
        StringBuilder base = new StringBuilder();
        for (String component : components) {
            Optional<String> value = componentValue(req, component);
            if (value.isEmpty()) {
                return Optional.empty();
            }
            base.append('"').append(component).append("\": ").append(value.get()).append('\n');
        }
        base.append("\"@signature-params\": ").append(signatureParams);
        return Optional.of(base.toString());
    }

    /**
     * Serializes the {@code Signature-Input} value for a set of covered components.
     *
     * @param components the covered component identifiers, in signing order.
     * @param created    when the signature was created; serialized as a Unix timestamp.
     * @param keyId      the access key ID doing the signing.
     * @param nonce      a value unique to this signature, for replay detection.
     * @return the serialized parameters, without a dictionary label.
     */
    public static String serializeSignatureParams(List<String> components, Instant created, String keyId, String nonce) {
        StringBuilder params = new StringBuilder("(");
        for (int i = 0; i < components.size(); i++) {
            if (i > 0) {
                params.append(' ');
            }
            params.append(quote(components.get(i)));
        }
        return params.append(')')
                .append(";created=").append(created.getEpochSecond())
                .append(";keyid=").append(quote(keyId))
                .append(";alg=").append(quote(ALGORITHM))
                .append(";nonce=").append(quote(nonce))
                .append(";tag=").append(quote(TAG))
                .toString();
    }

    /**
     * Signs a request in place: sets Content-Digest, Signature-Input and Signature.
     * <p>
     * Call after every header the signature must cover (x-euclid-target, x-euclid-action, the
     * optional routing headers, host) and the body are already set on req. Unlike
     * {@link SigV4#sign} this leaves Authorization alone, so a request can carry a bearer token and
     * a signature at once if a deployment wants both during a migration.
     *
     * @param req             request to sign; mutated in place.
     * @param accessKeyId     the caller's access key ID, sent as {@code keyid}.
     * @param secretAccessKey the caller's secret access key, used as the HMAC key.
     * @throws IllegalArgumentException if a component that must be covered is missing from req,
     *                                  which for a client means the caller forgot to set host or a
     *                                  routing header before signing.
     */
    public static void sign(SignableRequest req, String accessKeyId, String secretAccessKey) {
        req.header("Content-Digest", contentDigest(req.body()));

        List<String> components = coveredComponents(req);
        String params = serializeSignatureParams(components, Instant.now(), accessKeyId, newNonce());
        String base = signatureBase(req, components, params).orElseThrow(() ->
                new IllegalArgumentException("request is missing a header that must be signed: " + components));

        byte[] signature = hmacSha256(secretAccessKey.getBytes(StandardCharsets.UTF_8), base);
        req.header("Signature-Input", LABEL + "=" + params);
        req.header("Signature", LABEL + "=:" + Base64.getEncoder().encodeToString(signature) + ":");
    }

    /**
     * Verifies an RFC 9421-signed request with the default 15-minute clock-skew tolerance.
     *
     * @param req          the request to verify.
     * @param lookupSecret resolves an access key ID to its secret, or empty if unknown.
     * @return the resolved access key ID on success, empty on any failure.
     * @see #verify(SignableRequest, Function, Duration)
     */
    public static Optional<VerifyResult> verify(SignableRequest req, Function<String, Optional<String>> lookupSecret) {
        return verify(req, lookupSecret, DEFAULT_MAX_SKEW);
    }

    /**
     * Verifies an RFC 9421-signed request.
     * <p>
     * Rebuilds the signature base from the request exactly as received - including the signature
     * parameters verbatim, since re-serializing them would not reproduce another implementation's
     * byte-for-byte choices - and compares the HMAC (constant-time) against the one presented in
     * Signature.
     *
     * @param req          the request to verify.
     * @param lookupSecret resolves an access key ID to its secret, or empty if unknown.
     * @param maxSkew      maximum age (in either direction) {@code created} may have relative to
     *                     now before the request is rejected as stale/replayed.
     * @return the resolved access key ID on success, empty on any failure (missing/malformed
     * headers, an unexpected algorithm or tag, a covered-component list that is not the one this
     * request requires, unknown key, stale timestamp, body that does not match Content-Digest, or
     * signature mismatch) - callers don't get to distinguish which, as with {@link SigV4#verify}.
     */
    public static Optional<VerifyResult> verify(SignableRequest req, Function<String, Optional<String>> lookupSecret, Duration maxSkew) {
        Optional<ParsedSignature> parsedOpt = parseSignature(req.header("signature-input"), req.header("signature"));
        if (parsedOpt.isEmpty()) {
            return Optional.empty();
        }
        SignatureParams params = parsedOpt.get().params();

        // Only hmac-sha256 exists here, so an unexpected alg is a rejection rather than a branch;
        // this is also what keeps a signature from being reinterpreted under a weaker algorithm.
        if (!ALGORITHM.equals(params.algorithm()) || !TAG.equals(params.tag())) {
            return Optional.empty();
        }

        // Fixed policy, not client-negotiated - see the class comment. Order is left to the signer
        // (the base is built in the order received), but the set is not negotiable, and the size
        // check keeps a repeated component from padding the set out to the expected one.
        List<String> expected = coveredComponents(req);
        if (params.components().size() != expected.size()
                || !new HashSet<>(params.components()).equals(new HashSet<>(expected))) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        if (params.created() == null || Duration.between(params.created(), now).abs().compareTo(maxSkew) > 0) {
            return Optional.empty();
        }
        if (params.expires() != null && params.expires().isBefore(now)) {
            return Optional.empty();
        }

        if (!contentDigest(req.body()).equals(req.header("content-digest"))) {
            return Optional.empty();
        }

        Optional<String> secretOpt = lookupSecret.apply(params.keyId());
        if (secretOpt.isEmpty()) {
            return Optional.empty();
        }

        Optional<String> base = signatureBase(req, params.components(), params.raw());
        if (base.isEmpty()) {
            return Optional.empty();
        }

        byte[] expectedSignature = hmacSha256(secretOpt.get().getBytes(StandardCharsets.UTF_8), base.get());
        if (!MessageDigest.isEqual(expectedSignature, parsedOpt.get().signature())) {
            return Optional.empty();
        }

        return Optional.of(new VerifyResult(params.keyId()));
    }

    /**
     * Parses a matching pair of {@code Signature-Input} and {@code Signature} header values.
     * <p>
     * Both must hold exactly one signature under the same label. RFC 9421 allows several - a proxy
     * adding its own alongside the client's - but euclid has no use for more than one, and refusing
     * to choose among them means never verifying the wrong one.
     *
     * @param signatureInputHeader raw value of the Signature-Input header.
     * @param signatureHeader      raw value of the Signature header.
     * @return the parsed pair, or empty if either header is absent, malformed, or holds anything
     * other than a single signature.
     */
    public static Optional<ParsedSignature> parseSignature(String signatureInputHeader, String signatureHeader) {
        Optional<String[]> inputMember = singleDictionaryMember(signatureInputHeader);
        Optional<String[]> signatureMember = singleDictionaryMember(signatureHeader);
        if (inputMember.isEmpty() || signatureMember.isEmpty()) {
            return Optional.empty();
        }
        if (!inputMember.get()[0].equals(signatureMember.get()[0])) {
            return Optional.empty();
        }

        Optional<SignatureParams> params = parseSignatureParams(inputMember.get()[1]);
        Optional<byte[]> signature = parseByteSequence(signatureMember.get()[1]);
        if (params.isEmpty() || signature.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ParsedSignature(inputMember.get()[0], params.get(), signature.get()));
    }

    /**
     * Parses a {@code Signature-Input} value: an inner list of component identifiers followed by
     * the signature parameters.
     *
     * @param value the dictionary member's value, without its label.
     * @return the parsed parameters, or empty if the value is not well-formed.
     */
    public static Optional<SignatureParams> parseSignatureParams(String value) {
        if (value.isEmpty() || value.charAt(0) != '(') {
            return Optional.empty();
        }

        List<String> components = new ArrayList<>();
        int pos = 1;
        while (true) {
            if (pos >= value.length()) {
                return Optional.empty();
            }
            char c = value.charAt(pos);
            if (c == ')') {
                pos++;
                break;
            }
            if (c == ' ') {
                pos++;
                continue;
            }
            if (c != '"') {
                return Optional.empty();
            }
            int end = endOfQuotedString(value, pos);
            if (end < 0) {
                return Optional.empty();
            }
            // A component with its own parameters ("x";req and friends) means something this
            // class does not implement, so it is rejected rather than silently read as plain.
            if (end + 1 < value.length() && value.charAt(end + 1) != ' ' && value.charAt(end + 1) != ')') {
                return Optional.empty();
            }
            components.add(unquote(value.substring(pos, end + 1)));
            pos = end + 1;
        }
        if (components.isEmpty()) {
            return Optional.empty();
        }

        Optional<Map<String, Object>> params = parseParameters(value.substring(pos));
        if (params.isEmpty()) {
            return Optional.empty();
        }
        Map<String, Object> parsed = params.get();

        Object keyId = parsed.get("keyid");
        if (!(keyId instanceof String)) {
            return Optional.empty();
        }
        Optional<Instant> created = timestamp(parsed.get("created"));
        Optional<Instant> expires = timestamp(parsed.get("expires"));
        if (parsed.containsKey("created") && created.isEmpty()) {
            return Optional.empty();
        }
        if (parsed.containsKey("expires") && expires.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new SignatureParams(List.copyOf(components), created.orElse(null), expires.orElse(null),
                (String) keyId, stringOrNull(parsed.get("alg")), stringOrNull(parsed.get("nonce")),
                stringOrNull(parsed.get("tag")), value));
    }

    // --- internal helpers -------------------------------------------------------------------

    /**
     * Resolves one covered component to the value that goes in the signature base: the derived
     * values of RFC 9421 §2.2 for the {@code @}-prefixed ones, the header value otherwise.
     * Empty when the request cannot supply it.
     */
    private static Optional<String> componentValue(SignableRequest req, String component) {
        if (!component.startsWith("@")) {
            Map<String, String> headers = req.headers();
            return headers.containsKey(component) ? Optional.of(headers.get(component)) : Optional.empty();
        }

        String target = req.target();
        int query = target.indexOf('?');
        return switch (component) {
            case "@method" -> Optional.of(req.method().toUpperCase(Locale.ROOT));
            case "@authority" -> authority(req);
            case "@scheme" -> Optional.of(req.scheme());
            // §2.2.6/§2.2.7: the path is "/" when empty, and the query keeps its leading "?" and
            // is that character alone when the request has none.
            case "@path" -> {
                String path = query < 0 ? target : target.substring(0, query);
                yield Optional.of(path.isEmpty() ? "/" : path);
            }
            case "@query" -> Optional.of(query < 0 ? "?" : "?" + target.substring(query + 1));
            case "@request-target" -> Optional.of(target);
            case "@target-uri" -> authority(req).map(host -> req.scheme() + "://" + host + target);
            default -> Optional.empty();
        };
    }

    /**
     * The {@code @authority} value: the host header lowercased, with the port dropped when it is
     * the default for the request's scheme, as RFC 9421 §2.2.3 requires.
     */
    private static Optional<String> authority(SignableRequest req) {
        String host = req.header("host").toLowerCase(Locale.ROOT);
        if (host.isEmpty()) {
            return Optional.empty();
        }
        String defaultPort = "https".equals(req.scheme()) ? ":443" : "http".equals(req.scheme()) ? ":80" : null;
        if (defaultPort != null && host.endsWith(defaultPort)) {
            host = host.substring(0, host.length() - defaultPort.length());
        }
        return Optional.of(host);
    }

    /**
     * Splits a structured-field dictionary that must hold exactly one member into its label and
     * raw value, respecting quoting and nesting so a comma inside a component name or a signature
     * cannot be mistaken for a member separator.
     */
    private static Optional<String[]> singleDictionaryMember(String header) {
        if (header == null || header.isEmpty()) {
            return Optional.empty();
        }

        int equals = -1;
        boolean inQuotes = false;
        int depth = 0;
        for (int i = 0; i < header.length(); i++) {
            char c = header.charAt(i);
            if (inQuotes) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    inQuotes = false;
                }
                continue;
            }
            switch (c) {
                case '"' -> inQuotes = true;
                case '(' -> depth++;
                case ')' -> depth--;
                case '=' -> {
                    if (equals < 0) {
                        equals = i;
                    }
                }
                case ',' -> {
                    if (depth == 0) {
                        return Optional.empty();
                    }
                }
                default -> {
                }
            }
        }
        if (equals <= 0 || equals == header.length() - 1 || inQuotes || depth != 0) {
            return Optional.empty();
        }

        String label = header.substring(0, equals).trim();
        if (label.isEmpty() || !label.chars().allMatch(Rfc9421::isTokenChar)) {
            return Optional.empty();
        }
        return Optional.of(new String[]{label, header.substring(equals + 1).trim()});
    }

    /**
     * Parses a run of {@code ;name=value} parameters. Values may be strings, integers, tokens or
     * booleans; parameters this class does not know about are kept rather than rejected, since
     * whatever is here was chosen by the signer and is covered by the signature.
     */
    private static Optional<Map<String, Object>> parseParameters(String input) {
        Map<String, Object> params = new LinkedHashMap<>();
        int pos = 0;
        while (pos < input.length()) {
            if (input.charAt(pos) != ';') {
                return Optional.empty();
            }
            pos++;
            int nameEnd = pos;
            while (nameEnd < input.length() && isTokenChar(input.charAt(nameEnd))) {
                nameEnd++;
            }
            if (nameEnd == pos) {
                return Optional.empty();
            }
            String name = input.substring(pos, nameEnd);
            pos = nameEnd;

            if (pos >= input.length() || input.charAt(pos) != '=') {
                // A bare parameter is shorthand for "true" in structured fields.
                params.put(name, Boolean.TRUE);
                continue;
            }
            pos++;
            if (pos >= input.length()) {
                return Optional.empty();
            }

            char c = input.charAt(pos);
            if (c == '"') {
                int end = endOfQuotedString(input, pos);
                if (end < 0) {
                    return Optional.empty();
                }
                params.put(name, unquote(input.substring(pos, end + 1)));
                pos = end + 1;
            } else if (c == '?') {
                if (pos + 1 >= input.length() || (input.charAt(pos + 1) != '0' && input.charAt(pos + 1) != '1')) {
                    return Optional.empty();
                }
                params.put(name, input.charAt(pos + 1) == '1');
                pos += 2;
            } else {
                int end = pos;
                while (end < input.length() && input.charAt(end) != ';') {
                    end++;
                }
                String raw = input.substring(pos, end);
                if (raw.isEmpty()) {
                    return Optional.empty();
                }
                params.put(name, asIntegerOrToken(raw));
                pos = end;
            }
        }
        return Optional.of(params);
    }

    /**
     * Parses a structured-field byte sequence, {@code :} + base64 + {@code :}.
     */
    private static Optional<byte[]> parseByteSequence(String value) {
        if (value.length() < 2 || value.charAt(0) != ':' || value.charAt(value.length() - 1) != ':') {
            return Optional.empty();
        }
        try {
            return Optional.of(Base64.getDecoder().decode(value.substring(1, value.length() - 1)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Index of the closing quote of the string starting at {@code start}, or -1 if it is unterminated.
     */
    private static int endOfQuotedString(String value, int start) {
        for (int i = start + 1; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '"') {
                return i;
            }
        }
        return -1;
    }

    private static String quote(String value) {
        return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    private static String unquote(String quoted) {
        return quoted.substring(1, quoted.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static Object asIntegerOrToken(String raw) {
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException e) {
            return raw;
        }
    }

    private static Optional<Instant> timestamp(Object value) {
        return value instanceof Long seconds ? Optional.of(Instant.ofEpochSecond(seconds)) : Optional.empty();
    }

    private static String stringOrNull(Object value) {
        return value instanceof String text ? text : null;
    }

    private static boolean isTokenChar(int c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                || c == '_' || c == '-' || c == '.' || c == '*';
    }

    private static String newNonce() {
        byte[] nonce = new byte[16];
        RANDOM.nextBytes(nonce);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(nonce);
    }

    private static byte[] sha256(String data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data.getBytes(StandardCharsets.UTF_8));
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
}
