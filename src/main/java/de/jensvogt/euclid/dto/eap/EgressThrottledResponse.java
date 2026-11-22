package de.jensvogt.euclid.dto.eap;

/**
 * The answer to report-egress-throttled: until when every instance of the application now leaves
 * the site alone.
 *
 * <p>Worth reading rather than assuming {@code now + retryAfterSeconds}: euclid caps one report at
 * an hour and floors it at a second, and a block that already ran later than the one reported is
 * kept rather than shortened.
 *
 * @param applicationId the application the site refused
 * @param site          the site, as euclid stores it (lower-cased host)
 * @param blockedUntil  until when no permit for the site is granted (ISO 8601)
 */
public record EgressThrottledResponse(String applicationId, String site, String blockedUntil) {
}
