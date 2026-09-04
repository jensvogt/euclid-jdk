package de.jensvogt.euclid.dto.emo;

/**
 * One measurement pushed to EMO.
 *
 * <p>A metric is a name, at most one label, a value and a type. One label rather than a set is
 * EMO's own shape, not a simplification made here: a sample is stored under
 * {@code name/labelName/labelValue}, and that triple is what its rollups aggregate by.
 *
 * @param name       metric name, e.g. {@code application-utilisation}
 * @param labelName  dimension the value is broken down by, e.g. {@code instance}; may be empty for
 *                   a metric with a single series
 * @param labelValue value of that dimension, e.g. the instance id
 * @param value      the measurement itself
 * @param type       how the value should be read over time
 */
public record Metric(String name, String labelName, String labelValue, double value, Type type) {

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
}
