package de.jensvogt.euclid.dto.emo;

/**
 * Request to list where the series threshold rules judge stand now, optionally narrowed.
 * <p>
 * Every filter is optional and left out of the request when unset, which lists every series of
 * every rule in the account.
 *
 * @param ruleId only this rule's series
 * @param state  only series in this state: {@code "OK"}, {@code "WARNING"}, {@code "CRITICAL"} or
 *               {@code "NO_DATA"}
 * @param active {@code true} for only the series that are not OK - what is wrong right now
 */
public record ListAlertsRequest(String ruleId, String state, Boolean active) {

    /**
     * Creates a new instance of the Builder for constructing a ListAlertsRequest object.
     *
     * @return a new Builder instance for constructing ListAlertsRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListAlertsRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Only this rule's series.
         */
        private String ruleId;

        /**
         * Only series in this state.
         */
        private String state;

        /**
         * Only series that are not OK.
         */
        private Boolean active;

        /**
         * Sets the rule whose series alone are listed.
         *
         * @param ruleId only this rule's series
         * @return the builder instance
         */
        public Builder ruleId(String ruleId) {
            this.ruleId = ruleId;
            return this;
        }

        /**
         * Sets the state the listed series must be in.
         *
         * @param state {@code "OK"}, {@code "WARNING"}, {@code "CRITICAL"} or {@code "NO_DATA"}
         * @return the builder instance
         */
        public Builder state(String state) {
            this.state = state;
            return this;
        }

        /**
         * Sets whether only series that are not OK are listed.
         *
         * @param active {@code true} for only the series that are not OK
         * @return the builder instance
         */
        public Builder active(Boolean active) {
            this.active = active;
            return this;
        }

        /**
         * Builds and returns a new instance of ListAlertsRequest using the properties set on the Builder.
         *
         * @return a new ListAlertsRequest instance.
         */
        public ListAlertsRequest build() {
            return new ListAlertsRequest(ruleId, state, active);
        }
    }
}
