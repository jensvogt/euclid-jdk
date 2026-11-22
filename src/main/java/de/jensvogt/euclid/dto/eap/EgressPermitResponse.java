package de.jensvogt.euclid.dto.eap;

import de.jensvogt.euclid.dto.eap.model.EgressLimits;

/**
 * The answer to one request for an egress permit: either a permit, or how long to wait before
 * asking again.
 *
 * <p>Both are a successful call. Being told to wait is the limit working, not an error - which is
 * why {@link de.jensvogt.euclid.module.eap.EuclidEgressLimiter} is what most callers want: it does
 * the waiting.
 *
 * @param applicationId the application the permit was asked for, as euclid resolved it
 * @param site          the site the permit is for, as euclid stores it (lower-cased host)
 * @param granted       true if the request may go ahead
 * @param permitId      the permit to release afterwards, if granted
 * @param expiresAt     when the permit's slot is given back anyway, if granted (ISO 8601)
 * @param reason        why not, if not: {@code blocked} (the site asked to be left alone),
 *                      {@code concurrency} (every slot taken) or {@code rate} (the bucket is empty)
 * @param retryAfterMs  how long to wait before asking again, if not
 * @param limits        the limits the decision was made under - the site's own, the application's
 *                      {@code "*"} default, or the installation's. Sent with every answer, granted
 *                      or not, so a caller being held back can see by what without a second call
 *                      to list-egress-sites; {@code null} only from an EAP too old to send it
 */
public record EgressPermitResponse(String applicationId, String site, boolean granted, String permitId,
                                   String expiresAt, String reason, long retryAfterMs, EgressLimits limits) {

    /**
     * Builds an answer without the application or the limits it was decided under - the shape this
     * record had before EAP reported them, kept so that code constructing one (a stubbed backend
     * for {@link de.jensvogt.euclid.module.eap.EuclidEgressLimiter} in a test, typically) goes on
     * compiling.
     *
     * @param site         the site the permit is for
     * @param granted      true if the request may go ahead
     * @param permitId     the permit to release afterwards, if granted
     * @param expiresAt    when the permit's slot is given back anyway, if granted (ISO 8601)
     * @param reason       why not, if not
     * @param retryAfterMs how long to wait before asking again, if not
     */
    public EgressPermitResponse(String site, boolean granted, String permitId, String expiresAt, String reason,
                                long retryAfterMs) {
        this(null, site, granted, permitId, expiresAt, reason, retryAfterMs, null);
    }
}
