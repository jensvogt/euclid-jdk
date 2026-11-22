package de.jensvogt.euclid.dto.emo;

import de.jensvogt.euclid.dto.emo.model.DashboardSummary;

import java.util.List;

/**
 * Mirrors EMO's {@code list-dashboards} response: the caller's own saved dashboards, by name.
 * <p>
 * Only the caller's: EMO takes the owner from the authenticated identity and offers no way to name
 * another user, so there is no listing of somebody else's dashboards to ask for - which is the
 * whole of the access control on dashboards, and why these actions need no administrator.
 *
 * @param userId     the user whose dashboards these are, which is always the caller
 * @param dashboards the dashboards, without their layouts
 */
public record ListDashboardsResponse(String userId, List<DashboardSummary> dashboards) {

    /**
     * Copies the list and turns null into an empty one, so the record is immutable and its list is
     * never null however it was parsed.
     */
    public ListDashboardsResponse {
        dashboards = dashboards == null ? List.of() : List.copyOf(dashboards);
    }
}
