package de.jensvogt.euclid.dto.ees;

/**
 * Request to list a subscriber's subscriptions.
 *
 * @param name the subscriber name
 */
public record ListSubscriptionsRequest(String name) {

    /**
     * Creates a new instance of the Builder for constructing a ListSubscriptionsRequest object.
     *
     * @return a new Builder instance for constructing ListSubscriptionsRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListSubscriptionsRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The subscriber name.
         */
        private String name;

        /**
         * Sets the subscriber name.
         *
         * @param name the subscriber name
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Builds and returns a new instance of ListSubscriptionsRequest using the properties set on the Builder.
         *
         * @return a new ListSubscriptionsRequest instance.
         */
        public ListSubscriptionsRequest build() {
            return new ListSubscriptionsRequest(name);
        }
    }
}
