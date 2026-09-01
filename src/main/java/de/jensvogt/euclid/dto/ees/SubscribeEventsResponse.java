package de.jensvogt.euclid.dto.ees;

import de.jensvogt.euclid.dto.ees.model.EventSubscription;
import java.util.List;

/**
 * The subscriber's subscriptions after a subscribe-events action.
 *
 * @param subscriptions every subscription the subscriber now holds
 */
public record SubscribeEventsResponse(List<EventSubscription> subscriptions) {

    /**
     * Creates a new instance of the Builder for constructing a SubscribeEventsResponse object.
     *
     * @return a new Builder instance for constructing SubscribeEventsResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SubscribeEventsResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Every subscription the subscriber now holds.
         */
        private List<EventSubscription> subscriptions;

        /**
         * Sets every subscription the subscriber now holds.
         *
         * @param subscriptions every subscription the subscriber now holds
         * @return the builder instance
         */
        public Builder subscriptions(List<EventSubscription> subscriptions) {
            this.subscriptions = subscriptions;
            return this;
        }

        /**
         * Builds and returns a new instance of SubscribeEventsResponse using the properties set on the Builder.
         *
         * @return a new SubscribeEventsResponse instance.
         */
        public SubscribeEventsResponse build() {
            return new SubscribeEventsResponse(subscriptions);
        }
    }
}
