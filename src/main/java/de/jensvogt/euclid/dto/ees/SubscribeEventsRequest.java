package de.jensvogt.euclid.dto.ees;

import java.util.List;
import java.util.Map;

/**
 * Request to register a durable external subscription, or to update its filter.
 *
 * @param name       the subscriber name events are claimed under
 * @param eventTypes event types to receive, e.g. {@code "esm.object.modified"}
 * @param filter     exact-match key/value pairs an event payload must satisfy; empty receives every event of these types
 */
public record SubscribeEventsRequest(String name, List<String> eventTypes, Map<String, Object> filter) {

    /**
     * Creates a new instance of the Builder for constructing a SubscribeEventsRequest object.
     *
     * @return a new Builder instance for constructing SubscribeEventsRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SubscribeEventsRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The subscriber name events are claimed under.
         */
        private String name;

        /**
         * Event types to receive, e.g. {@code "esm.object.modified"}.
         */
        private List<String> eventTypes;

        /**
         * Exact-match key/value pairs an event payload must satisfy; empty receives every event of these types.
         */
        private Map<String, Object> filter;

        /**
         * Sets the subscriber name events are claimed under.
         *
         * @param name the subscriber name events are claimed under
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets event types to receive, e.g. {@code "esm.object.modified"}.
         *
         * @param eventTypes event types to receive, e.g. {@code "esm.object.modified"}
         * @return the builder instance
         */
        public Builder eventTypes(List<String> eventTypes) {
            this.eventTypes = eventTypes;
            return this;
        }

        /**
         * Sets exact-match key/value pairs an event payload must satisfy; empty receives every event of these types.
         *
         * @param filter exact-match key/value pairs an event payload must satisfy; empty receives every event of these types
         * @return the builder instance
         */
        public Builder filter(Map<String, Object> filter) {
            this.filter = filter;
            return this;
        }

        /**
         * Builds and returns a new instance of SubscribeEventsRequest using the properties set on the Builder.
         *
         * @return a new SubscribeEventsRequest instance.
         */
        public SubscribeEventsRequest build() {
            return new SubscribeEventsRequest(name, eventTypes, filter);
        }
    }
}
