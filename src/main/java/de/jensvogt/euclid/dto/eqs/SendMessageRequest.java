package de.jensvogt.euclid.dto.eqs;

import de.jensvogt.euclid.dto.com.Variant;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Request to send a message to a queue.
 *
 * @param ern        the ERN (Entity Resource Name) of the queue to send the message to
 * @param body       the message body
 * @param attributes       typed, user-defined message attributes
 * @param systemAttributes euclid's own envelope - a correlation id, say - passed on from the message
 *                         this one was sent in response to, so it identifies the whole chain rather
 *                         than only its first hop; kept apart from {@code attributes} the whole way
 * @param priority         the priority of the message
 */
public record SendMessageRequest(String ern, String body, Map<String, Variant> attributes,
                                 Map<String, Variant> systemAttributes, String priority) {

    /**
     * Creates a new instance of the Builder for constructing a SendMessageRequest object.
     *
     * @return a new Builder instance for constructing SendMessageRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SendMessageRequest} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN (Entity Resource Name) of the queue to send the message to.
         */
        private String ern;

        /**
         * The message body.
         */
        private String body;

        /**
         * Typed, user-defined message attributes.
         */
        private Map<String, Variant> attributes = new LinkedHashMap<>();

        /**
         * Euclid's own envelope, carried across every hop.
         */
        private Map<String, Variant> systemAttributes = new LinkedHashMap<>();

        /**
         * The priority of the message.
         */
        private String priority = "MEDIUM";

        /**
         * Sets the ERN of the queue to send the message to.
         *
         * @param ern the queue ERN
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
         * Sets the typed, user-defined message attributes.
         *
         * @param attributes the message attributes
         * @return the builder instance
         */
        public Builder attributes(Map<String, Variant> attributes) {
            this.attributes = attributes;
            return this;
        }

        /**
         * Sets euclid's own envelope for this message.
         *
         * <p>For a service in the middle of a chain: the message it received carried these in
         * {@link de.jensvogt.euclid.dto.eqs.model.Message#systemAttributes()}, and handing them on
         * here is what keeps whatever was travelling - a correlation id most of all - travelling.
         * Left empty, the chain ends at this hop.
         *
         * @param systemAttributes the system attributes to carry on
         * @return the builder instance
         */
        public Builder systemAttributes(Map<String, Variant> systemAttributes) {
            this.systemAttributes = systemAttributes;
            return this;
        }

        /**
         * Sets the priority of the message.
         *
         * @param priority the message priority
         * @return the builder instance
         */
        public Builder priority(String priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Builds and returns a new instance of SendMessageRequest using the properties set on the Builder.
         *
         * @return a new SendMessageRequest instance populated with the ERN, body, attributes, system attributes and priority values.
         */
        public SendMessageRequest build() {
            return new SendMessageRequest(ern, body, attributes, systemAttributes, priority);
        }
    }
}
