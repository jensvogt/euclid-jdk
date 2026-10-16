package de.jensvogt.euclid.dto.eap;

/**
 * The answer to one request for an egress permit: either a permit, or how long to wait before
 * asking again.
 *
 * <p>Both are a successful call. Being told to wait is the limit working, not an error - which is
 * why {@link de.jensvogt.euclid.module.eap.EuclidEgressLimiter} is what most callers want: it does
 * the waiting.
 *
 * @param site         the site the permit is for, as euclid stores it (lower-cased host)
 * @param granted      true if the request may go ahead
 * @param permitId     the permit to release afterwards, if granted
 * @param expiresAt    when the permit's slot is given back anyway, if granted (ISO 8601)
 * @param reason       why not, if not: {@code blocked} (the site asked to be left alone),
 *                     {@code concurrency} (every slot taken) or {@code rate} (the bucket is empty)
 * @param retryAfterMs how long to wait before asking again, if not
 */
public record EgressPermitResponse(String site, boolean granted, String permitId, String expiresAt,
                                   String reason, long retryAfterMs) {
}
