package de.jensvogt.euclid.dto.ees;

import java.util.List;

/**
 * Request to delete claimed events, which is what "processed" means here.
 *
 * @param name     the subscriber name the events were claimed under
 * @param eventIds IDs from the claimed envelopes
 */
public record AckEventsRequest(String name, List<String> eventIds) {

    /**
     * Creates a new instance of the Builder for constructing an AckEventsRequest object.
     *
     * @return a new Builder instance for constructing AckEventsRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link AckEventsRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The subscriber name the events were claimed under.
         */
        private String name;

        /**
         * IDs from the claimed envelopes.
         */
        private List<String> eventIds;

        /**
         * Sets the subscriber name the events were claimed under.
         *
         * @param name the subscriber name the events were claimed under
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets IDs from the claimed envelopes.
         *
         * @param eventIds IDs from the claimed envelopes
         * @return the builder instance
         */
        public Builder eventIds(List<String> eventIds) {
            this.eventIds = eventIds;
            return this;
        }

        /**
         * Builds and returns a new instance of AckEventsRequest using the properties set on the Builder.
         *
         * @return a new AckEventsRequest instance.
         */
        public AckEventsRequest build() {
            return new AckEventsRequest(name, eventIds);
        }
    }
}
