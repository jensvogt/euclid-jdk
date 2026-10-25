package de.jensvogt.euclid.dto.eap.model;

/**
 * One outside site of one application, as list-egress-sites reports it: what it is allowed and how
 * it stands now.
 *
 * <p>The {@code "*"} entry is an application's default and nothing else - nobody acquires a permit
 * for it - so it carries limits only: {@code tokens} and {@code inFlight} are 0 and
 * {@code blockedUntil} and {@code lastUsed} are {@code null}.
 *
 * @param applicationId the ID of the application
 * @param site          the host name, or {@code "*"} for the application's default
 * @param limits        the limits that apply to it, its own or inherited
 * @param limitsSet     whether those limits are the site's own rather than inherited
 * @param tokens        tokens left in its bucket, as of now
 * @param inFlight      requests holding a slot right now
 * @param blockedUntil  until when the site asked to be left alone (ISO 8601), or {@code null}
 * @param lastUsed      when a permit for it was last asked for (ISO 8601), or {@code null}
 */
public record EgressSite(String applicationId, String site, EgressLimits limits, boolean limitsSet,
                         double tokens, long inFlight, String blockedUntil, String lastUsed) {
}
