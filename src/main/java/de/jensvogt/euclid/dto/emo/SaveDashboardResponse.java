package de.jensvogt.euclid.dto.emo;

/**
 * Mirrors EMO's {@code save-dashboard} response: which dashboard was stored, for whom, and when.
 * <p>
 * {@code userId} is there because the owner is never part of the request - EMO always files a
 * dashboard under the user it authenticated - so this is the one place a caller signing with an
 * access key can see whose dashboard it just wrote.
 *
 * @param name     the dashboard's name
 * @param userId   the user it was saved for, which is always the caller
 * @param modified when it was saved, ISO 8601
 */
public record SaveDashboardResponse(String name, String userId, String modified) {
}
