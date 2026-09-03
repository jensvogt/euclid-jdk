package de.jensvogt.euclid.dto.esm;

import java.util.List;

/**
 * Request to subscribe a target resource to a bucket's events, so an object created in that
 * bucket is announced to the target - a queue or a topic, depending on {@code type}.
 *
 * @param sourceErn   the ERN of the bucket whose events are subscribed to
 * @param type        the target resource type, {@code "queue"} or {@code "topic"}
 * @param targetErn   the ERN of the queue or topic the events are delivered to
 * @param eventTypes  which object events to deliver, or empty for all of them
 * @param prefix      only deliver objects whose key starts with this, or empty for the whole bucket
 * @param directories whether directory markers count as objects worth delivering
 */
public record SubscribeRequest(String sourceErn, String type, String targetErn, List<String> eventTypes,
                               String prefix, boolean directories) {

    /**
     * Creates a new instance of the Builder for constructing a SubscribeRequest object.
     *
     * @return a new Builder instance for constructing SubscribeRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SubscribeRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN of the bucket whose events are subscribed to.
         */
        private String sourceErn;

        /**
         * The target resource type, {@code "queue"} or {@code "topic"}.
         */
        private String type;

        /**
         * The ERN of the queue or topic the events are delivered to.
         */
        private String targetErn;

        /**
         * Which object events to deliver, empty for all of them.
         */
        private List<String> eventTypes = List.of();

        /**
         * Only deliver objects whose key starts with this, empty for the whole bucket.
         */
        private String prefix = "";

        /**
         * Whether directory markers count as objects worth delivering.
         */
        private boolean directories;

        /**
         * Sets the ERN of the bucket whose events are subscribed to.
         *
         * @param sourceErn the ERN of the bucket whose events are subscribed to
         * @return the builder instance
         */
        public Builder sourceErn(String sourceErn) {
            this.sourceErn = sourceErn;
            return this;
        }

        /**
         * Sets the target resource type, {@code "queue"} or {@code "topic"}.
         *
         * @param type the target resource type, {@code "queue"} or {@code "topic"}
         * @return the builder instance
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the ERN of the queue or topic the events are delivered to.
         *
         * @param targetErn the ERN of the queue or topic the events are delivered to
         * @return the builder instance
         */
        public Builder targetErn(String targetErn) {
            this.targetErn = targetErn;
            return this;
        }

        /**
         * Sets which object events to deliver.
         *
         * @param eventTypes the object events to deliver, or empty for all of them
         * @return the builder instance
         */
        public Builder eventTypes(List<String> eventTypes) {
            this.eventTypes = eventTypes == null ? List.of() : eventTypes;
            return this;
        }

        /**
         * Sets the key prefix objects must start with to be delivered.
         *
         * @param prefix the key prefix, or empty for the whole bucket
         * @return the builder instance
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix == null ? "" : prefix;
            return this;
        }

        /**
         * Sets whether directory markers count as objects worth delivering.
         *
         * @param directories whether directory markers are delivered too
         * @return the builder instance
         */
        public Builder directories(boolean directories) {
            this.directories = directories;
            return this;
        }

        /**
         * Builds and returns a new instance of SubscribeRequest using the properties set on the Builder.
         *
         * @return a new SubscribeRequest instance.
         */
        public SubscribeRequest build() {
            return new SubscribeRequest(sourceErn, type, targetErn, eventTypes, prefix, directories);
        }
    }
}
