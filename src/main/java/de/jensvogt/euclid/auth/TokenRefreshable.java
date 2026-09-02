package de.jensvogt.euclid.auth;

import java.util.function.Supplier;

/**
 * A client whose bearer token can be replaced after it was built.
 *
 * <p>A token is not a credential the caller keeps: it is issued with an expiry and has to be
 * replaced before it runs out. A client constructed with a token string holds that one string for
 * as long as it lives, which is right for a process that outlives no token - a command-line call,
 * a job that runs and exits - and wrong for a server that runs for days. Handing the client a
 * {@link Supplier} instead moves the decision to the caller: it is asked at each request, so
 * whatever the caller currently considers valid is what goes on the wire.
 *
 * <p>Applications euclid deploys are the case this exists for. Such an application is given a
 * bearer token in a file, which euclid rewrites at half the token's lifetime; the supplier reads
 * that file, and the client that never restarts keeps working. See {@code euclid-spring}'s
 * {@code CredentialsFileTokens} for that reader.
 *
 * <p>Setting a supplier does not change how a client chooses between authentication schemes: a
 * client configured with a SigV4 access key signs its requests and never consults the token at
 * all.
 */
public interface TokenRefreshable {

    /**
     * Takes the bearer token for each request from {@code token} rather than from a fixed string.
     *
     * <p>Called on every request that authenticates with a bearer token, so it should be cheap -
     * cache what it returns and renew on a schedule of its own rather than doing work per call.
     * It may be called from several threads at once.
     *
     * @param token supplies the token to present, never {@code null} itself
     */
    void token(Supplier<String> token);

    /**
     * Replaces the bearer token with a fixed one, for a caller that renews it on its own schedule
     * and pushes the result in.
     *
     * @param token the token to present from now on
     */
    default void token(String token) {
        token(() -> token);
    }
}
