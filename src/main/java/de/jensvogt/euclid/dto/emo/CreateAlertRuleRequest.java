package de.jensvogt.euclid.dto.emo;

import java.util.Map;

/**
 * Request to create a threshold rule on one metric.
 * <p>
 * Fields left unset are omitted from the request rather than sent as null, so the server applies
 * its own default for them - which is why the optional ones are boxed types. The defaults are
 * {@code "value"}, {@code "above"}, one period each way, no NO_DATA, and enabled. Only
 * {@code ruleId}, {@code name} and at least one of {@code warning} and {@code critical} are
 * required.
 *
 * @param ruleId             the rule's name, unique in the account: letters, digits, {@code -},
 *                           {@code _} and {@code .}, at most 128 characters
 * @param name               the metric to judge, as {@code list-metrics} names it
 * @param labels             only series with every one of these labels are judged; unset or empty
 *                           judges every series of the metric
 * @param statistic          which figure of a period is judged: {@code "value"} (the mean, or the
 *                           sum for a rate), {@code "max"} or {@code "min"}. A short spike only
 *                           shows in {@code max}
 * @param comparison         {@code "above"} or {@code "below"}: which side of the thresholds is the
 *                           wrong one
 * @param warning            warning threshold; below {@code critical} for {@code "above"}, above it
 *                           for {@code "below"}
 * @param critical           critical threshold
 * @param forPeriods         periods past a threshold before the state is entered, 1-1000
 * @param clearPeriods       periods back inside before it is left, 1-1000
 * @param noDataAfterPeriods periods without data before NO_DATA, 0-1000; 0 never
 * @param enabled            {@code false} to store the rule without evaluating it yet
 * @param description        what the rule is for
 */
public record CreateAlertRuleRequest(String ruleId, String name, Map<String, String> labels, String statistic,
                                     String comparison, Double warning, Double critical, Long forPeriods,
                                     Long clearPeriods, Long noDataAfterPeriods, Boolean enabled,
                                     String description) {

    /**
     * Creates a new instance of the Builder for constructing a CreateAlertRuleRequest object.
     *
     * @return a new Builder instance for constructing CreateAlertRuleRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CreateAlertRuleRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The rule's name, unique in the account.
         */
        private String ruleId;

        /**
         * The metric to judge.
         */
        private String name;

        /**
         * The label selector.
         */
        private Map<String, String> labels;

        /**
         * Which figure of a period is judged.
         */
        private String statistic;

        /**
         * Which side of the thresholds is the wrong one.
         */
        private String comparison;

        /**
         * Warning threshold.
         */
        private Double warning;

        /**
         * Critical threshold.
         */
        private Double critical;

        /**
         * Periods past a threshold before the state is entered.
         */
        private Long forPeriods;

        /**
         * Periods back inside before it is left.
         */
        private Long clearPeriods;

        /**
         * Periods without data before NO_DATA.
         */
        private Long noDataAfterPeriods;

        /**
         * Whether the rule is evaluated from the start.
         */
        private Boolean enabled;

        /**
         * What the rule is for.
         */
        private String description;

        /**
         * Sets the rule's name, unique in the account.
         *
         * @param ruleId the rule's name
         * @return the builder instance
         */
        public Builder ruleId(String ruleId) {
            this.ruleId = ruleId;
            return this;
        }

        /**
         * Sets the metric to judge.
         *
         * @param name the metric's name, e.g. {@code system-load-per-core}
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the label selector: only series carrying every one of these labels are judged.
         *
         * @param labels label name to required value
         * @return the builder instance
         */
        public Builder labels(Map<String, String> labels) {
            this.labels = labels;
            return this;
        }

        /**
         * Sets which figure of a period is judged.
         *
         * @param statistic {@code "value"}, {@code "max"} or {@code "min"}
         * @return the builder instance
         */
        public Builder statistic(String statistic) {
            this.statistic = statistic;
            return this;
        }

        /**
         * Sets which side of the thresholds is the wrong one.
         *
         * @param comparison {@code "above"} or {@code "below"}
         * @return the builder instance
         */
        public Builder comparison(String comparison) {
            this.comparison = comparison;
            return this;
        }

        /**
         * Sets the warning threshold.
         *
         * @param warning the warning threshold
         * @return the builder instance
         */
        public Builder warning(Double warning) {
            this.warning = warning;
            return this;
        }

        /**
         * Sets the critical threshold.
         *
         * @param critical the critical threshold
         * @return the builder instance
         */
        public Builder critical(Double critical) {
            this.critical = critical;
            return this;
        }

        /**
         * Sets the periods past a threshold before the state is entered.
         *
         * @param forPeriods 1-1000
         * @return the builder instance
         */
        public Builder forPeriods(Long forPeriods) {
            this.forPeriods = forPeriods;
            return this;
        }

        /**
         * Sets the periods back inside before the state is left.
         *
         * @param clearPeriods 1-1000
         * @return the builder instance
         */
        public Builder clearPeriods(Long clearPeriods) {
            this.clearPeriods = clearPeriods;
            return this;
        }

        /**
         * Sets the periods without data before a series goes NO_DATA.
         *
         * @param noDataAfterPeriods 0-1000; 0 never
         * @return the builder instance
         */
        public Builder noDataAfterPeriods(Long noDataAfterPeriods) {
            this.noDataAfterPeriods = noDataAfterPeriods;
            return this;
        }

        /**
         * Sets whether the rule is evaluated from the start.
         *
         * @param enabled {@code false} to store the rule without evaluating it yet
         * @return the builder instance
         */
        public Builder enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * Sets what the rule is for.
         *
         * @param description the description
         * @return the builder instance
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Builds and returns a new instance of CreateAlertRuleRequest using the properties set on the Builder.
         *
         * @return a new CreateAlertRuleRequest instance.
         */
        public CreateAlertRuleRequest build() {
            return new CreateAlertRuleRequest(ruleId, name, labels, statistic, comparison, warning, critical,
                    forPeriods, clearPeriods, noDataAfterPeriods, enabled, description);
        }
    }
}
