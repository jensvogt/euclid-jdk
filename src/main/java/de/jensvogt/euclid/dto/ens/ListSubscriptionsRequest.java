package de.jensvogt.euclid.dto.ens;

/**
 * Request to list the subscriptions of a topic.
 *
 * @param topicErn ERN of the topic whose subscriptions are listed
 */
public record ListSubscriptionsRequest(String topicErn) {

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
         * The ERN of the topic whose subscriptions are listed.
         */
        private String topicErn;

        /**
         * Sets the ERN of the topic whose subscriptions are listed.
         *
         * @param topicErn the topic ERN
         * @return the builder instance
         */
        public Builder topicErn(String topicErn) {
            this.topicErn = topicErn;
            return this;
        }

        /**
         * Builds and returns a new instance of ListSubscriptionsRequest using the properties set on the Builder.
         *
         * @return a new ListSubscriptionsRequest instance.
         */
        public ListSubscriptionsRequest build() {
            return new ListSubscriptionsRequest(topicErn);
        }
    }
}
