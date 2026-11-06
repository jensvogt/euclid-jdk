package de.jensvogt.euclid.dto.emo;

/**
 * Request to list recorded alert state changes, newest first, optionally narrowed.
 * <p>
 * Every field is optional and left out of the request when unset; the server then returns the 100
 * most recent changes of every rule in the account.
 *
 * @param ruleId only this rule's changes
 * @param from   only changes at or after this time, ISO 8601, e.g. {@code 2026-11-06T08:00:00Z}
 * @param to     only changes up to this time, ISO 8601
 * @param limit  how many changes at most; the server defaults to 100
 */
public record ListAlertHistoryRequest(String ruleId, String from, String to, Long limit) {

    /**
     * Creates a new instance of the Builder for constructing a ListAlertHistoryRequest object.
     *
     * @return a new Builder instance for constructing ListAlertHistoryRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListAlertHistoryRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Only this rule's changes.
         */
        private String ruleId;

        /**
         * Lower time bound, ISO 8601.
         */
        private String from;

        /**
         * Upper time bound, ISO 8601.
         */
        private String to;

        /**
         * How many changes at most.
         */
        private Long limit;

        /**
         * Sets the rule whose changes alone are listed.
         *
         * @param ruleId only this rule's changes
         * @return the builder instance
         */
        public Builder ruleId(String ruleId) {
            this.ruleId = ruleId;
            return this;
        }

        /**
         * Sets the lower time bound.
         *
         * @param from ISO 8601 timestamp
         * @return the builder instance
         */
        public Builder from(String from) {
            this.from = from;
            return this;
        }

        /**
         * Sets the upper time bound.
         *
         * @param to ISO 8601 timestamp
         * @return the builder instance
         */
        public Builder to(String to) {
            this.to = to;
            return this;
        }

        /**
         * Sets how many changes are returned at most.
         *
         * @param limit the maximum number of changes
         * @return the builder instance
         */
        public Builder limit(Long limit) {
            this.limit = limit;
            return this;
        }

        /**
         * Builds and returns a new instance of ListAlertHistoryRequest using the properties set on the Builder.
         *
         * @return a new ListAlertHistoryRequest instance.
         */
        public ListAlertHistoryRequest build() {
            return new ListAlertHistoryRequest(ruleId, from, to, limit);
        }
    }
}
