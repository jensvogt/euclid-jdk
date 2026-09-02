package de.jensvogt.euclid.auth;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Which request-signing scheme a client uses when it authenticates with an access key.
 *
 * <p>Two schemes exist because euclid is moving from one to the other, not because both are wanted
 * in the end. {@link #SIGV4} is what euclid has always spoken and stays the default, so no existing
 * caller changes behaviour; {@link #RFC9421} is the standard scheme meant to replace it. Selecting
 * one is a per-client decision - see {@link SigningSchemeSelectable} - so a deployment can move one
 * service at a time and roll back by changing a line rather than a release.
 *
 * <p>Neither scheme is consulted when a client authenticates with a bearer token: with no access
 * key there is nothing to sign with, and the token goes in Authorization as before.
 *
 * <p>The two do not collide on the wire. SigV4 puts its signature in Authorization, RFC 9421 in
 * Signature/Signature-Input, so a server can accept both at once and tell which a request used -
 * {@link #of(SignableRequest)} is that test.
 */
public enum SigningScheme {

    /**
     * AWS Signature Version 4, as {@link SigV4} implements it. The default, and what euclid's
     * server has understood from the start.
     */
    SIGV4 {
        @Override
        public void sign(SignableRequest req, String accessKeyId, String secretAccessKey, String region, String service) {
            SigV4.sign(req, accessKeyId, secretAccessKey, region, service);
        }

        @Override
        public Optional<String> verify(SignableRequest req, Function<String, Optional<String>> lookupSecret) {
            return SigV4.verify(req, lookupSecret).map(SigV4.VerifyResult::accessKeyId);
        }

        @Override
        public List<String> signatureHeaderNames() {
            return SIGV4_HEADER_NAMES;
        }
    },

    /**
     * RFC 9421 HTTP Message Signatures, as {@link Rfc9421} implements it, keyed by the same access
     * key and secret SigV4 uses.
     *
     * <p>{@code region} and {@code service} are not passed to it: SigV4 needs them to derive its
     * signing key, whereas RFC 9421 signs the {@code x-euclid-region} and {@code x-euclid-target}
     * headers that carry the same facts, so binding them twice would add a way for the two copies
     * to disagree.
     */
    RFC9421 {
        @Override
        public void sign(SignableRequest req, String accessKeyId, String secretAccessKey, String region, String service) {
            Rfc9421.sign(req, accessKeyId, secretAccessKey);
        }

        @Override
        public Optional<String> verify(SignableRequest req, Function<String, Optional<String>> lookupSecret) {
            return Rfc9421.verify(req, lookupSecret).map(Rfc9421.VerifyResult::keyId);
        }

        @Override
        public List<String> signatureHeaderNames() {
            return Rfc9421.signatureHeaderNames();
        }
    };

    /**
     * The headers {@link SigV4#sign} writes, in the case they should be sent in.
     */
    private static final List<String> SIGV4_HEADER_NAMES = List.of("x-amz-date", "x-amz-content-sha256", "Authorization");

    /**
     * Signs a request in place.
     *
     * @param req             request to sign; mutated in place. Every header the signature covers,
     *                        and the body, must already be set.
     * @param accessKeyId     the caller's access key ID.
     * @param secretAccessKey the caller's secret access key.
     * @param region          region to scope the signature to; unused by {@link #RFC9421}.
     * @param service         service to scope the signature to; unused by {@link #RFC9421}.
     */
    public abstract void sign(SignableRequest req, String accessKeyId, String secretAccessKey, String region, String service);

    /**
     * Verifies a request signed with this scheme, with the scheme's default clock-skew tolerance.
     *
     * @param req          the request to verify.
     * @param lookupSecret resolves an access key ID to its secret, or empty if unknown.
     * @return the access key ID whose signature was verified, or empty on any failure.
     */
    public abstract Optional<String> verify(SignableRequest req, Function<String, Optional<String>> lookupSecret);

    /**
     * The headers {@link #sign} adds to a request, for callers that copy them onto some other
     * request object afterwards.
     *
     * @return the header names, in the case they should be sent in.
     */
    public abstract List<String> signatureHeaderNames();

    /**
     * Identifies which scheme, if either, a received request was signed with, by looking for the
     * headers only that scheme uses.
     *
     * <p>This is a routing decision, not a verification: it says which {@link #verify} to call,
     * and nothing about whether that call will succeed.
     *
     * @param req the request as received.
     * @return the scheme the request presents a signature for, or empty if it presents none -
     * a bearer-token request, or an unsigned one.
     */
    public static Optional<SigningScheme> of(SignableRequest req) {
        if (!req.header("signature-input").isEmpty()) {
            return Optional.of(RFC9421);
        }
        if (req.header("authorization").startsWith("AWS4-HMAC-SHA256")) {
            return Optional.of(SIGV4);
        }
        return Optional.empty();
    }
}
