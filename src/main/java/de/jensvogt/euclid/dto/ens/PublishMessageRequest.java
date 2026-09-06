package de.jensvogt.euclid.dto.ens;

import de.jensvogt.euclid.dto.com.Variant;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Request to publish a message to a topic.
 *
 * @param ern        topic ERN
 * @param body       message body
 * @param attributes typed message attributes
 * @param priority   priority of the queue messages this publish fans out to. A topic is not
 *                   consumed from, so this says nothing about the topic itself - it is what the
 *                   messages its SQS-type subscriptions turn this one into are given, so that a
 *                   hop through a topic does not silently reset a delivery to MIDDLE
 */
public record PublishMessageRequest(String ern, String body, Map<String, Variant> attributes, String priority) {

    /**
     * Creates a new instance of the Builder for constructing a PublishMessageRequest object.
     *
     * @return a new Builder instance for constructing PublishMessageRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link PublishMessageRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The topic ERN.
         */
        private String ern;

        /**
         * The message body.
         */
        private String body;

        /**
         * The message attributes.
         */
        private Map<String, Variant> attributes = new LinkedHashMap<>();

        /**
         * The priority of the queue messages this publish fans out to.
         */
        private String priority = "MIDDLE";

        /**
         * Sets the topic ERN.
         *
         * @param ern the topic ERN
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the message body.
         *
         * @param body the message body
         * @return the builder instance
         */
        public Builder body(String body) {
            this.body = body;
            return this;
        }

        /**
         * Sets the message attributes.
         *
         * @param attributes the message attributes
         * @return the builder instance
         */
        public Builder attributes(Map<String, Variant> attributes) {
            this.attributes = attributes;
            return this;
        }

        /**
         * Sets the priority of the queue messages this publish fans out to.
         *
         * @param priority the priority
         * @return the builder instance
         */
        public Builder priority(String priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Builds and returns a new instance of PublishMessageRequest using the properties set on the Builder.
         *
         * @return a new PublishMessageRequest instance.
         */
        public PublishMessageRequest build() {
            return new PublishMessageRequest(ern, body, attributes, priority);
        }
    }
}
