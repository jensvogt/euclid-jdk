package de.jensvogt.euclid.dto.eap;

import de.jensvogt.euclid.dto.eap.model.EgressSite;
import de.jensvogt.euclid.dto.eap.model.EgressLimits;

import java.util.List;

/**
 * The answer to list-egress-sites.
 *
 * @param defaultLimits what a site is held to when neither it nor its application's {@code "*"}
 *                      entry has a limit of its own - {@code euclid.modules.eap.egress.default}
 * @param sites         each application's sites, sorted by application and then site
 */
public record ListEgressSitesResponse(EgressLimits defaultLimits, List<EgressSite> sites) {
}
