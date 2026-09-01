package de.jensvogt.euclid.dto.ees;

import de.jensvogt.euclid.dto.ees.model.Event;
import java.util.List;

/**
 * The events a receive-events action claimed.
 *
 * @param events the claimed events, oldest first
 * @param total  number of events claimed
 */
public record ReceiveEventsResponse(List<Event> events, long total) {

    /**
     * Creates a new instance of the Builder for constructing a ReceiveEventsResponse object.
     *
     * @return a new Builder instance for constructing ReceiveEventsResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ReceiveEventsResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The claimed events, oldest first.
         */
        private List<Event> events;

        /**
         * Number of events claimed.
         */
        private long total = 0;

        /**
         * Sets the claimed events, oldest first.
         *
         * @param events the claimed events, oldest first
         * @return the builder instance
         */
        public Builder events(List<Event> events) {
            this.events = events;
            return this;
        }

        /**
         * Sets number of events claimed.
         *
         * @param total number of events claimed
         * @return the builder instance
         */
        public Builder total(long total) {
            this.total = total;
            return this;
        }

        /**
         * Builds and returns a new instance of ReceiveEventsResponse using the properties set on the Builder.
         *
         * @return a new ReceiveEventsResponse instance.
         */
        public ReceiveEventsResponse build() {
            return new ReceiveEventsResponse(events, total);
        }
    }
}
