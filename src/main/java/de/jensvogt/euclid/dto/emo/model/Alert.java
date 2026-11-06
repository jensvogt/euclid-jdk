package de.jensvogt.euclid.dto.emo.model;

import java.util.Map;

/**
 * Mirrors one item of EMO's {@code list-alerts} response: where one series a rule judges stands
 * now.
 * <p>
 * Each item carries the rule's metric, lines and description beside the state rather than only a
 * {@code ruleId}, because what reads this - a dashboard colouring a panel, a banner listing what is
 * wrong - wants to show it without a second lookup. A series the rule has not seen a row of yet is
 * not listed at all.
 *
 * @param ruleId      the rule judging the series
 * @param name        the metric the rule judges
 * @param labels      the labels identifying this series
 * @param state       where the series stands: {@code "OK"}, {@code "WARNING"}, {@code "CRITICAL"} or
 *                    {@code "NO_DATA"}
 * @param severity    the same in the lower-case form events carry: {@code "ok"}, {@code "warning"},
 *                    {@code "critical"} or {@code "nodata"}
 * @param since       when the series entered its state, ISO 8601, or empty if never recorded
 * @param value       the last figure judged
 * @param lastSeen    when a row of the series was last judged, ISO 8601, or empty if never
 * @param statistic   which figure of a period the rule judges, {@code "value"}, {@code "max"} or
 *                    {@code "min"}
 * @param comparison  which side of the thresholds is the wrong one, {@code "above"} or
 *                    {@code "below"}
 * @param warning     the rule's warning threshold, or {@code null} if it has none
 * @param critical    the rule's critical threshold, or {@code null} if it has none
 * @param description what the rule is for, in words
 * @param namespace   the namespace the rule was created in, empty at the account root
 */
public record Alert(String ruleId, String name, Map<String, String> labels, String state, String severity,
                    String since, double value, String lastSeen, String statistic, String comparison,
                    Double warning, Double critical, String description, String namespace) {

    /**
     * Copies the labels and turns null into an empty map, so the record is immutable and its labels
     * are never null however it was parsed.
     */
    public Alert {
        labels = labels == null ? Map.of() : Map.copyOf(labels);
    }
}
