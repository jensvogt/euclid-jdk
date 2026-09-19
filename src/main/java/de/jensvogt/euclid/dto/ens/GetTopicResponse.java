package de.jensvogt.euclid.dto.ens;

import de.jensvogt.euclid.dto.ens.model.Topic;

/**
 * One topic, as a listing describes each of its own.
 * <p>
 * The same {@link Topic} a listing answer carries, rather than a shape of its own, so that what a
 * listing shows and what this shows cannot drift apart.
 *
 * @param topic the topic
 */
public record GetTopicResponse(Topic topic) {

    /**
     * Creates a new instance of the Builder for constructing a GetTopicResponse object.
     *
     * @return a new Builder instance for constructing GetTopicResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetTopicResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The topic.
         */
        private Topic topic;

        /**
         * Sets the topic.
         *
         * @param topic the topic
         * @return the builder instance
         */
        public Builder topic(Topic topic) {
            this.topic = topic;
            return this;
        }

        /**
         * Builds and returns a new instance of GetTopicResponse using the properties set on the Builder.
         *
         * @return a new GetTopicResponse instance.
         */
        public GetTopicResponse build() {
            return new GetTopicResponse(topic);
        }
    }
}
