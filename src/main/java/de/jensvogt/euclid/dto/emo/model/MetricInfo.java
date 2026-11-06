package de.jensvogt.euclid.dto.emo.model;

import java.util.List;

/**
 * Mirrors one item of EMO's {@code list-metrics} response: a metric EMO has recorded recently,
 * described rather than read.
 * <p>
 * This is the answer to "what is there?" - the question no query can answer without already knowing
 * a name. The name is what {@link AlertRule#name()} takes, and the label names are what a rule's
 * label selector can match on.
 *
 * @param name       the metric's name, e.g. {@code system-load-per-core}
 * @param type       how its values are read over time, {@code "GAUGE"} or {@code "RATE"} - which
 *                   decides what a rule's {@code value} statistic means for it: the period's mean
 *                   for a gauge, its sum for a rate
 * @param series     how many distinct label combinations it has, i.e. how many series a rule
 *                   without a label selector would judge
 * @param labelNames the label names that tell its series apart, possibly empty for a metric with a
 *                   single series
 * @param lastSeen   when it last reported, ISO 8601
 */
public record MetricInfo(String name, String type, long series, List<String> labelNames, String lastSeen) {

    /**
     * Copies the label names and turns null into an empty list, so the record is immutable and its
     * list is never null however it was parsed.
     */
    public MetricInfo {
        labelNames = labelNames == null ? List.of() : List.copyOf(labelNames);
    }
}
