package de.jensvogt.euclid.auth;

/**
 * A client whose request-signing scheme can be chosen after it was built.
 *
 * <p>Which scheme is right is a property of the server a client happens to be pointed at, not of
 * the call being made, and during a migration it changes per deployment and per service. Rebuilding
 * the client - or threading the choice through every constructor and through
 * {@link de.jensvogt.euclid.module.eam.EuclidSession} - would make that a compile-time decision;
 * setting it on the client the session just handed out keeps it a deployment one, the same way
 * {@link TokenRefreshable} keeps the token a deployment concern.
 *
 * <p>The choice only takes effect for a client configured with an access key. One authenticating
 * with a bearer token has nothing to sign and ignores it.
 */
public interface SigningSchemeSelectable {

    /**
     * Signs subsequent requests with {@code scheme} rather than the default {@link
     * SigningScheme#SIGV4}.
     *
     * <p>Takes effect from the next request; requests already in flight keep the scheme they were
     * signed with. Safe to call from any thread.
     *
     * @param scheme the scheme to sign with, never {@code null}
     */
    void signingScheme(SigningScheme scheme);
}
