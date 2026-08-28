package de.jensvogt.euclid.dto.ens;

import de.jensvogt.euclid.dto.ens.model.Topic;

import java.util.List;

/**
 * Response returned from list-topics.
 *
 * @param topics the topics
 * @param total  total number of topics
 */
public record ListTopicsResponse(List<Topic> topics, long total) {

    /**
     * Creates a new instance of the Builder for constructing a ListTopicsResponse object.
     *
     * @return a new Builder instance for constructing ListTopicsResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListTopicsResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The topics.
         */
        private List<Topic> topics;

        /**
         * The total number of topics.
         */
        private long total;

        /**
         * Sets the topics.
         *
         * @param topics the topics
         * @return the builder instance
         */
        public Builder topics(List<Topic> topics) {
            this.topics = topics;
            return this;
        }

        /**
         * Sets the total number of topics.
         *
         * @param total the total number of topics
         * @return the builder instance
         */
        public Builder total(long total) {
            this.total = total;
            return this;
        }

        /**
         * Builds and returns a new instance of ListTopicsResponse using the properties set on the Builder.
         *
         * @return a new ListTopicsResponse instance.
         */
        public ListTopicsResponse build() {
            return new ListTopicsResponse(topics, total);
        }
    }
}
