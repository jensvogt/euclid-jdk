package de.jensvogt.euclid.dto.ead;

/**
 * Request to remove audit entries older than a given age.
 *
 * <p>The age is required and must be positive: a purge with no window would remove the whole
 * trail, and EAD refuses one with HTTP 400 rather than let that happen by an argument being left
 * out.
 *
 * @param olderThanDays remove entries older than this many days
 */
public record PurgeEventsRequest(long olderThanDays) {
}
