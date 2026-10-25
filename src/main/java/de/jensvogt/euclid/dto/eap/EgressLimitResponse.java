package de.jensvogt.euclid.dto.eap;

import de.jensvogt.euclid.dto.eap.model.EgressLimits;

/**
 * The answer to set-egress-limit and delete-egress-limit: what the application is held to for the
 * site from now on.
 *
 * @param applicationId the ID of the application
 * @param site          the site, as euclid stores it, or {@code "*"} for the application's default
 * @param limits        the limits that apply now - after a delete, the ones inherited from the
 *                      application's {@code "*"} entry or from {@code euclid.modules.eap.egress.default}
 */
public record EgressLimitResponse(String applicationId, String site, EgressLimits limits) {
}
