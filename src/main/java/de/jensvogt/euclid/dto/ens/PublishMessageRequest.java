package de.jensvogt.euclid.dto.ens;

import de.jensvogt.euclid.dto.eqs.model.Variant;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Request to publish a message to a topic.
 *
 * @param ern        topic ERN
 * @param body       message body
 * @param attributes typed message attributes
 */
public record PublishMessageRequest(String ern, String body, Map<String, Variant> attributes) {

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
         * Builds and returns a new instance of PublishMessageRequest using the properties set on the Builder.
         *
         * @return a new PublishMessageRequest instance.
         */
        public PublishMessageRequest build() {
            return new PublishMessageRequest(ern, body, attributes);
        }
    }
}
