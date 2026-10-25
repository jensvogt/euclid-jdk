package de.jensvogt.euclid.dto.eap.model;

/**
 * How fast one application may call one outside site, shared by all of its instances.
 *
 * @param requestsPerSecond how fast the site's token bucket refills
 * @param burst             how many tokens the bucket holds, and so how many requests may go out
 *                          at once after a quiet spell
 * @param concurrent        how many requests to the site may be in flight at once; 0 for no limit
 */
public record EgressLimits(double requestsPerSecond, double burst, long concurrent) {
}
