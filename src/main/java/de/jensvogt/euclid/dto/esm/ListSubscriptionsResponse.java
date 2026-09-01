package de.jensvogt.euclid.dto.esm;

import de.jensvogt.euclid.dto.esm.model.Subscription;
import java.util.List;

/**
 * The subscriptions currently registered on a bucket.
 *
 * @param subscriptions the subscriptions on the bucket
 * @param total         the total number of subscriptions
 */
public record ListSubscriptionsResponse(List<Subscription> subscriptions, long total) {

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
         * The subscriptions on the bucket.
         */
        private List<Subscription> subscriptions;

        /**
         * The total number of subscriptions.
         */
        private long total = 0;

        /**
         * Sets the subscriptions on the bucket.
         *
         * @param subscriptions the subscriptions on the bucket
         * @return the builder instance
         */
        public Builder subscriptions(List<Subscription> subscriptions) {
            this.subscriptions = subscriptions;
            return this;
        }

        /**
         * Sets the total number of subscriptions.
         *
         * @param total the total number of subscriptions
         * @return the builder instance
         */
        public Builder total(long total) {
            this.total = total;
            return this;
        }

        /**
         * Builds and returns a new instance of ListSubscriptionsResponse using the properties set on the Builder.
         *
         * @return a new ListSubscriptionsResponse instance.
         */
        public ListSubscriptionsResponse build() {
            return new ListSubscriptionsResponse(subscriptions, total);
        }
    }
}
