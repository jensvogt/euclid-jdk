package de.jensvogt.euclid.dto.emo.model;

import java.util.Map;

/**
 * Mirrors one item of EMO's {@code list-alert-history} response: one change of state of one
 * series, as {@code EmoServer.cpp}'s {@code transitionJson()} returns it.
 * <p>
 * The same change was published on the event bus when it happened; this is the record of it that
 * stays, for {@code euclid.modules.emo.alert.history-retention} days (90 as shipped), so a change
 * nobody was subscribed to at the time can still be found.
 *
 * @param ruleId    the rule that decided the change
 * @param name      the metric the rule judges
 * @param labels    the labels identifying the series
 * @param previous  the state left: {@code "OK"}, {@code "WARNING"}, {@code "CRITICAL"} or
 *                  {@code "NO_DATA"}
 * @param state     the state entered, in the same form
 * @param severity  the state entered in the lower-case form events carry, e.g. {@code "critical"}
 * @param value     the figure that decided the change
 * @param threshold the line it was judged against, or {@code null} where none applied - a series
 *                  going NO_DATA, or one cleared because its rule was changed or deleted
 * @param statistic which figure of a period was judged, {@code "value"}, {@code "max"} or
 *                  {@code "min"}
 * @param reason    why a series was cleared other than by recovering - {@code "rule-deleted"},
 *                  {@code "rule-disabled"} or {@code "rule-changed"} - or empty
 * @param timestamp when the change happened, ISO 8601
 * @param accountId the account the rule belongs to
 * @param namespace the namespace the rule was created in, empty at the account root
 */
public record AlertTransition(String ruleId, String name, Map<String, String> labels, String previous, String state,
                              String severity, double value, Double threshold, String statistic, String reason,
                              String timestamp, String accountId, String namespace) {

    /**
     * Copies the labels and turns null into an empty map, so the record is immutable and its labels
     * are never null however it was parsed.
     */
    public AlertTransition {
        labels = labels == null ? Map.of() : Map.copyOf(labels);
    }
}
