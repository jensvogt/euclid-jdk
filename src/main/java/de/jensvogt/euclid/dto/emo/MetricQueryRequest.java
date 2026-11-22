package de.jensvogt.euclid.dto.emo;

import java.util.Map;

/**
 * Request naming which of EMO's stored rows to read, shared by
 * {@link de.jensvogt.euclid.module.emo.EuclidEmo#listMetricData(MetricQueryRequest)} (action
 * {@code list}) and {@link de.jensvogt.euclid.module.emo.EuclidEmo#averageMetric(MetricQueryRequest)}
 * (action {@code average}), because the server parses both with the same filter.
 * <p>
 * Every field is optional and left out of the request when unset - which is why they are boxed -
 * so the server applies its own defaults: every metric, the {@code RAW} tier, no time bounds, and
 * for {@code list} the 100 newest rows. An empty request is therefore valid, but on a busy
 * installation it is mostly a way of seeing the last few seconds of everything; name a metric.
 * <p>
 * The dimensions can be narrowed two ways. {@code labels} is the map form: every entry must match,
 * and a row may carry more dimensions than were asked for, so {@code {"area": "heap"}} finds the
 * heap series whatever else labels them. {@code labelName} and {@code labelValue} are the older
 * one-pair form, kept because every one-dimensional metric euclid records about itself is still
 * read that way; given together they are one more entry of the map, {@code labelName} alone
 * finds the rows that carry that dimension at all, and {@code labelValue} alone finds the rows
 * where any dimension has that value.
 *
 * @param name       only this metric, e.g. {@code system-load-per-core}; unset reads every metric
 * @param labels     only rows carrying every one of these dimensions with this value
 * @param labelName  only rows carrying this dimension - with {@code labelValue}, carrying it with
 *                   that value
 * @param labelValue only rows where some dimension - {@code labelName}, if given - has this value
 * @param resolution the tier to read: {@code "RAW"} (one row per averaging period, five minutes as
 *                   shipped), {@code "HOUR"} or {@code "DAY"}. The server defaults to {@code RAW}
 *                   and quietly reads {@code RAW} for a name it does not know, rather than
 *                   refusing - so a typo here shows up as the wrong tier, not as an error
 * @param from       only rows at or after this time, ISO 8601, e.g. {@code 2026-11-06T08:00:00Z}
 * @param to         only rows before this time (exclusive), ISO 8601
 * @param limit      how many rows {@code list} returns at most, newest first; the server defaults to
 *                   100, and 0 or less means no limit. {@code average} ignores it and always reads
 *                   every matching row
 */
public record MetricQueryRequest(String name, Map<String, String> labels, String labelName, String labelValue,
                                 String resolution, String from, String to, Long limit) {

    /**
     * Copies the labels so the request is immutable. A null map stays null, so that an unset
     * selector is left out of the request rather than sent as an empty object.
     */
    public MetricQueryRequest {
        labels = labels == null ? null : Map.copyOf(labels);
    }

    /**
     * Creates a new instance of the Builder for constructing a MetricQueryRequest object.
     *
     * @return a new Builder instance for constructing MetricQueryRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link MetricQueryRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Only this metric.
         */
        private String name;

        /**
         * Only rows carrying every one of these dimensions.
         */
        private Map<String, String> labels;

        /**
         * Only rows carrying this dimension.
         */
        private String labelName;

        /**
         * Only rows where a dimension has this value.
         */
        private String labelValue;

        /**
         * The tier to read.
         */
        private String resolution;

        /**
         * Lower time bound, ISO 8601, inclusive.
         */
        private String from;

        /**
         * Upper time bound, ISO 8601, exclusive.
         */
        private String to;

        /**
         * How many rows {@code list} returns at most.
         */
        private Long limit;

        /**
         * Sets the metric whose rows alone are read.
         *
         * @param name the metric name
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the dimensions every row read must carry.
         *
         * @param labels the dimensions and their values
         * @return the builder instance
         */
        public Builder labels(Map<String, String> labels) {
            this.labels = labels;
            return this;
        }

        /**
         * Sets the dimension every row read must carry, in the older one-pair form.
         *
         * @param labelName the dimension name
         * @return the builder instance
         */
        public Builder labelName(String labelName) {
            this.labelName = labelName;
            return this;
        }

        /**
         * Sets the value a dimension of every row read must have, in the older one-pair form.
         *
         * @param labelValue the dimension value
         * @return the builder instance
         */
        public Builder labelValue(String labelValue) {
            this.labelValue = labelValue;
            return this;
        }

        /**
         * Sets the tier to read.
         *
         * @param resolution {@code "RAW"}, {@code "HOUR"} or {@code "DAY"}
         * @return the builder instance
         */
        public Builder resolution(String resolution) {
            this.resolution = resolution;
            return this;
        }

        /**
         * Sets the lower time bound, inclusive.
         *
         * @param from ISO 8601 timestamp
         * @return the builder instance
         */
        public Builder from(String from) {
            this.from = from;
            return this;
        }

        /**
         * Sets the upper time bound, exclusive.
         *
         * @param to ISO 8601 timestamp
         * @return the builder instance
         */
        public Builder to(String to) {
            this.to = to;
            return this;
        }

        /**
         * Sets how many rows {@code list} returns at most.
         *
         * @param limit the maximum number of rows; 0 or less for no limit
         * @return the builder instance
         */
        public Builder limit(Long limit) {
            this.limit = limit;
            return this;
        }

        /**
         * Builds and returns a new instance of MetricQueryRequest using the properties set on the Builder.
         *
         * @return a new MetricQueryRequest instance.
         */
        public MetricQueryRequest build() {
            return new MetricQueryRequest(name, labels, labelName, labelValue, resolution, from, to, limit);
        }
    }
}
