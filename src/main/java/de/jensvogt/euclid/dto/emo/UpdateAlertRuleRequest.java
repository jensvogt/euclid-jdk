package de.jensvogt.euclid.dto.emo;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

/**
 * Request to change an existing threshold rule, one field at a time if wanted.
 * <p>
 * Only the fields actually set are sent, and the server only touches the fields it receives, so a
 * caller can move one threshold without resending the whole rule. That is why every field but
 * {@code ruleId} is a boxed type: null means "leave alone", not "clear".
 * <p>
 * Removing a threshold is the one change that needs a null on the wire - the server reads an
 * explicit {@code "warning": null} as "remove the warning line", and an absent one as "keep it" -
 * so it is asked for with {@link Builder#removeWarning()} and {@link Builder#removeCritical()}
 * rather than by passing null, and carried in {@link #removeWarning()} and
 * {@link #removeCritical()}, which are not serialized themselves.
 *
 * @param ruleId             the rule to change; the only required field
 * @param name               the metric to judge instead
 * @param labels             the label selector, replacing the whole current one; an empty map
 *                           judges every series of the metric
 * @param statistic          {@code "value"}, {@code "max"} or {@code "min"}
 * @param comparison         {@code "above"} or {@code "below"}
 * @param warning            the new warning threshold
 * @param critical           the new critical threshold
 * @param forPeriods         periods past a threshold before the state is entered, 1-1000
 * @param clearPeriods       periods back inside before it is left, 1-1000
 * @param noDataAfterPeriods periods without data before NO_DATA, 0-1000; 0 never
 * @param enabled            {@code true} to evaluate the rule, {@code false} to stop, clearing what
 *                           it raised
 * @param description        what the rule is for
 * @param removeWarning      whether to remove the warning threshold
 * @param removeCritical     whether to remove the critical threshold
 */
public record UpdateAlertRuleRequest(String ruleId, String name, Map<String, String> labels, String statistic,
                                     String comparison, Double warning, Double critical, Long forPeriods,
                                     Long clearPeriods, Long noDataAfterPeriods, Boolean enabled,
                                     String description, @JsonIgnore boolean removeWarning,
                                     @JsonIgnore boolean removeCritical) {

    /**
     * Creates a new instance of the Builder for constructing an UpdateAlertRuleRequest object.
     *
     * @return a new Builder instance for constructing UpdateAlertRuleRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link UpdateAlertRuleRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The rule to change; the only required field.
         */
        private String ruleId;

        /**
         * The metric to judge instead.
         */
        private String name;

        /**
         * The label selector, replacing the whole current one.
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
         * The new warning threshold.
         */
        private Double warning;

        /**
         * The new critical threshold.
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
         * Whether the rule is evaluated.
         */
        private Boolean enabled;

        /**
         * What the rule is for.
         */
        private String description;

        /**
         * Whether the warning threshold is to be removed.
         */
        private boolean removeWarning;

        /**
         * Whether the critical threshold is to be removed.
         */
        private boolean removeCritical;

        /**
         * Sets the rule to change; the only required field.
         *
         * @param ruleId the rule to change
         * @return the builder instance
         */
        public Builder ruleId(String ruleId) {
            this.ruleId = ruleId;
            return this;
        }

        /**
         * Points the rule at another metric. Ends the series it was judging: each one not OK is
         * cleared with reason {@code rule-changed}.
         *
         * @param name the metric to judge instead
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Replaces the whole label selector. Like a changed metric, this ends the series the rule
         * was judging.
         *
         * @param labels label name to required value; an empty map judges every series
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
         * Sets the warning threshold, undoing an earlier {@link #removeWarning()}.
         *
         * @param warning the new warning threshold
         * @return the builder instance
         */
        public Builder warning(Double warning) {
            this.warning = warning;
            this.removeWarning = false;
            return this;
        }

        /**
         * Removes the warning threshold, undoing an earlier {@link #warning(Double)}.
         *
         * @return the builder instance
         */
        public Builder removeWarning() {
            this.warning = null;
            this.removeWarning = true;
            return this;
        }

        /**
         * Sets the critical threshold, undoing an earlier {@link #removeCritical()}.
         *
         * @param critical the new critical threshold
         * @return the builder instance
         */
        public Builder critical(Double critical) {
            this.critical = critical;
            this.removeCritical = false;
            return this;
        }

        /**
         * Removes the critical threshold, undoing an earlier {@link #critical(Double)}.
         *
         * @return the builder instance
         */
        public Builder removeCritical() {
            this.critical = null;
            this.removeCritical = true;
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
         * Enables or disables the rule. Disabling clears every series it raised, with reason
         * {@code rule-disabled}.
         *
         * @param enabled {@code true} to evaluate the rule, {@code false} to stop
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
         * Builds and returns a new instance of UpdateAlertRuleRequest using the properties set on the Builder.
         *
         * @return a new UpdateAlertRuleRequest instance.
         */
        public UpdateAlertRuleRequest build() {
            return new UpdateAlertRuleRequest(ruleId, name, labels, statistic, comparison, warning, critical,
                    forPeriods, clearPeriods, noDataAfterPeriods, enabled, description, removeWarning,
                    removeCritical);
        }
    }
}
