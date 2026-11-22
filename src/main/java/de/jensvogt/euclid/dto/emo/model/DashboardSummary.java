package de.jensvogt.euclid.dto.emo.model;

/**
 * Mirrors one item of EMO's {@code list-dashboards} response: a saved dashboard by name, without
 * its layout.
 * <p>
 * The layout is left out on purpose: what lists dashboards is a chooser, and sending every panel of
 * every dashboard to draw a list of names is a waste. Fetch the one chosen with
 * {@link de.jensvogt.euclid.module.emo.EuclidEmo#getDashboard(String)}.
 *
 * @param name     the dashboard's name
 * @param created  when it was first saved, ISO 8601
 * @param modified when it was last saved, ISO 8601
 */
public record DashboardSummary(String name, String created, String modified) {
}
