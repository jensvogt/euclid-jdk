package de.jensvogt.euclid.dto.emo;

import java.util.Map;

/**
 * One measurement pushed to EMO.
 *
 * <p>A metric is a name, its dimensions, a value and a type. The dimensions are a map because a
 * meter carries however many the application gave it - {@code {"area": "heap", "id": "G1 Eden
 * Space"}}, {@code {"method": "GET", "status": "200"}} - and EMO stores them as a map for the same
 * reason. Flattening them into one pair would mean either losing dimensions or inventing a
 * composite value that nothing can aggregate across.
 *
 * <p>euclid's own metrics are one-dimensional, and the {@code (name, labelName, labelValue, value,
 * type)} constructor says that as plainly as it used to.
 *
 * @param name   metric name, e.g. {@code application-utilisation}
 * @param labels the dimensions the value is broken down by; may be empty for a metric with a
 *               single series
 * @param value  the measurement itself
 * @param type   how the value should be read over time
 */
public record Metric(String name, Map<String, String> labels, double value, Type type) {

    /**
     * Copies the labels and turns a null map into an empty one, so that a metric is immutable and
     * its labels are never null however it was built.
     */
    public Metric {
        labels = labels == null ? Map.of() : Map.copyOf(labels);
    }

    /**
     * A metric with a single dimension, which is every metric euclid records about itself.
     *
     * @param name       metric name
     * @param labelName  dimension the value is broken down by, e.g. {@code instance}; blank for a
     *                   metric with a single series
     * @param labelValue value of that dimension, e.g. the instance id
     * @param value      the measurement
     * @param type       how the value should be read over time
     */
    public Metric(String name, String labelName, String labelValue, double value, Type type) {
        this(name,
                labelName == null || labelName.isBlank()
                        ? Map.of()
                        : Map.of(labelName, labelValue == null ? "" : labelValue),
                value, type);
    }

    /**
     * The first dimension's name in key order, or an empty string when there are none.
     *
     * <p>For callers that only ever deal in one-dimensional metrics. Anything that cares about a
     * particular dimension should read {@link #labels()} by name rather than trust the order.
     *
     * @return the first dimension's name, or an empty string
     */
    public String labelName() {
        return labels.keySet().stream().sorted().findFirst().orElse("");
    }

    /**
     * The first dimension's value, or an empty string when there are none. See {@link #labelName}.
     *
     * @return the first dimension's value, or an empty string
     */
    public String labelValue() {
        return labels.getOrDefault(labelName(), "");
    }

    /**
     * How a metric's value is to be interpreted across the samples in a bucket.
     */
    public enum Type {

        /**
         * A level measured at a moment - a percentage, a queue depth, a byte count. Averaged
         * across the samples in a bucket.
         */
        GAUGE("gauge"),

        /**
         * A quantity accumulating over time, to be read as a per-second rate.
         */
        RATE("rate");

        private final String wireName;

        Type(String wireName) {
            this.wireName = wireName;
        }

        /**
         * Returns the spelling EMO expects in the {@code type} field
         *
         * @return the spelling EMO expects in the {@code type} field
         */
        public String wireName() {
            return wireName;
        }
    }

    /**
     * A gauge with a label, which is the ordinary case.
     *
     * @param name       metric name
     * @param labelName  dimension the value is broken down by
     * @param labelValue value of that dimension
     * @param value      the measurement
     * @return the metric
     */
    public static Metric gauge(String name, String labelName, String labelValue, double value) {
        return new Metric(name, labelName, labelValue, value, Type.GAUGE);
    }

    /**
     * A gauge with however many dimensions it was measured by.
     *
     * @param name   metric name
     * @param labels the dimensions the value is broken down by
     * @param value  the measurement
     * @return the metric
     */
    public static Metric gauge(String name, Map<String, String> labels, double value) {
        return new Metric(name, labels, value, Type.GAUGE);
    }

    /**
     * A rate with however many dimensions it was measured by.
     *
     * @param name   metric name
     * @param labels the dimensions the value is broken down by
     * @param value  the measurement
     * @return the metric
     */
    public static Metric rate(String name, Map<String, String> labels, double value) {
        return new Metric(name, labels, value, Type.RATE);
    }
}
