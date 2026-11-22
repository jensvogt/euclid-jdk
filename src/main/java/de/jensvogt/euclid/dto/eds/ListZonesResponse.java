package de.jensvogt.euclid.dto.eds;

import de.jensvogt.euclid.dto.eds.model.HostedZone;

import java.util.List;

/**
 * Response carrying a page of the caller's zones.
 *
 * <p>A zone the caller's grants do not let it read is left off the page, but {@link #total()} counts
 * every zone of the account and namespace that matches the prefix - so with a grant naming single
 * zones, the page can hold fewer than the total suggests.
 *
 * @param zones the zones on the requested page
 * @param total how many zones of the account and namespace match the prefix
 */
public record ListZonesResponse(List<HostedZone> zones, long total) {
}
