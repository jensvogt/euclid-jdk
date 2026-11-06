package de.jensvogt.euclid.dto.emo.model;

import java.util.Map;

/**
 * Mirrors the threshold rule {@code EmoServer.cpp}'s {@code ruleJson()} returns: a line drawn on
 * one metric that EMO judges every series of, every period.
 * <p>
 * Each period - five minutes as shipped - EMO writes the metric's rows and judges every series the
 * rule's {@link #labels()} match. A series past {@link #warning()} or {@link #critical()} for
 * {@link #forPeriods()} periods running enters WARNING or CRITICAL; back inside for
 * {@link #clearPeriods()} it is OK again. Each change is published on the event bus as
 * {@code emo.alert.raised} or {@code emo.alert.cleared} and recorded in the alert history.
 *
 * @param ruleId             the rule's name, unique in the account
 * @param name               the metric judged
 * @param labels             the label selector: only series carrying every one of these labels
 *                           with this value are judged; empty judges every series of the metric
 * @param resolution         the tier judged, always {@code "RAW"} - a rule on an hourly or daily
 *                           tier would be a report, not an alert
 * @param statistic          which figure of a period is judged: {@code "value"} (the mean, or the
 *                           sum for a rate), {@code "max"} or {@code "min"}
 * @param comparison         which side of the thresholds is the wrong one, {@code "above"} or
 *                           {@code "below"}
 * @param warning            the warning threshold, or {@code null} if the rule has none
 * @param critical           the critical threshold, or {@code null} if the rule has none; at least
 *                           one of the two is set
 * @param forPeriods         consecutive periods past a threshold before the state is entered
 * @param clearPeriods       consecutive periods back inside before it is left - the hysteresis
 *                           that stops a metric sitting on the line from flapping
 * @param noDataAfterPeriods periods without a row before a series goes NO_DATA; 0 never
 * @param enabled            whether the rule is evaluated; a disabled rule is kept but judges
 *                           nothing
 * @param description        what the rule is for, in words
 * @param accountId          the account the rule belongs to
 * @param namespace          the namespace it was created in, empty at the account root
 * @param created            creation timestamp, ISO 8601
 * @param modified           last-modified timestamp, ISO 8601
 */
public record AlertRule(String ruleId, String name, Map<String, String> labels, String resolution, String statistic,
                        String comparison, Double warning, Double critical, long forPeriods, long clearPeriods,
                        long noDataAfterPeriods, boolean enabled, String description, String accountId,
                        String namespace, String created, String modified) {

    /**
     * Copies the labels and turns null into an empty map, so the record is immutable and its
     * selector is never null however it was parsed.
     */
    public AlertRule {
        labels = labels == null ? Map.of() : Map.copyOf(labels);
    }
}
