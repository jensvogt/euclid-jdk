package de.jensvogt.euclid.dto.emo.model;

import java.util.Map;

/**
 * Mirrors one item of EMO's {@code list} response: one stored row of one series - what a graph
 * draws a point from.
 * <p>
 * A row is not a sample but a bucket: every sample of the series that arrived during one period of
 * the row's {@link #resolution()} folded into a mean, a minimum, a maximum and a count. That is why
 * a short spike is only visible in {@link #maxValue()}, and why the count is there at all - an
 * average across rows has to weight each by its samples, or quiet buckets count as much as busy
 * ones.
 *
 * @param name       the metric, e.g. {@code system-load-per-core}
 * @param labels     the dimensions identifying the series, possibly empty for a metric with a single
 *                   series
 * @param labelName  the first dimension's name, flattened out beside the map for readers written
 *                   before it; empty when there are none. For a one-dimensional metric it says
 *                   exactly what {@code labels} says
 * @param labelValue that dimension's value, or empty
 * @param value      the bucket's mean - or, for a {@code RATE}, its sum
 * @param minValue   the smallest sample in the bucket
 * @param maxValue   the largest sample in the bucket
 * @param samples    how many samples the bucket was made of
 * @param type       how the values are read over time, {@code "GAUGE"} or {@code "RATE"}
 * @param resolution the tier the row is from, {@code "RAW"}, {@code "HOUR"} or {@code "DAY"}
 * @param timestamp  the start of the bucket, ISO 8601
 */
public record MetricDataPoint(String name, Map<String, String> labels, String labelName, String labelValue,
                              double value, double minValue, double maxValue, long samples, String type,
                              String resolution, String timestamp) {

    /**
     * Copies the labels and turns null into an empty map, so the record is immutable and its labels
     * are never null however it was parsed.
     */
    public MetricDataPoint {
        labels = labels == null ? Map.of() : Map.copyOf(labels);
    }
}
