package de.jensvogt.euclid.dto.ees;

import de.jensvogt.euclid.dto.ees.model.EventSubscription;
import java.util.List;

/**
 * A subscriber's subscriptions, and how much is waiting for it.
 *
 * @param subscriptions the subscriber's subscriptions
 * @param waiting       number of events waiting for the subscriber, claimed or not
 */
public record ListSubscriptionsResponse(List<EventSubscription> subscriptions, long waiting) {

    /**
     * Creates a new instance of the Builder for constructing a ListSubscriptionsResponse object.
     *
     * @return a new Builder instance for constructing ListSubscriptionsResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListSubscriptionsResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The subscriber's subscriptions.
         */
        private List<EventSubscription> subscriptions;

        /**
         * Number of events waiting for the subscriber, claimed or not.
         */
        private long waiting = 0;

        /**
         * Sets the subscriber's subscriptions.
         *
         * @param subscriptions the subscriber's subscriptions
         * @return the builder instance
         */
        public Builder subscriptions(List<EventSubscription> subscriptions) {
            this.subscriptions = subscriptions;
            return this;
        }

        /**
         * Sets number of events waiting for the subscriber, claimed or not.
         *
         * @param waiting number of events waiting for the subscriber, claimed or not
         * @return the builder instance
         */
        public Builder waiting(long waiting) {
            this.waiting = waiting;
            return this;
        }

        /**
         * Builds and returns a new instance of ListSubscriptionsResponse using the properties set on the Builder.
         *
         * @return a new ListSubscriptionsResponse instance.
         */
        public ListSubscriptionsResponse build() {
            return new ListSubscriptionsResponse(subscriptions, waiting);
        }
    }
}
