package de.jensvogt.euclid.dto.eqs;

import de.jensvogt.euclid.dto.com.Variant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Request to put several messages on one queue in a single call.
 * <p>
 * The queue is named once and the messages carry no ern of their own: a batch is one queue's worth
 * by construction, which is what lets the server resolve and authorize the queue a single time and
 * write every message in one insert. The saving over calling
 * {@link de.jensvogt.euclid.module.eqs.EuclidEqs#sendMessage(String, String, Map, String)} in a loop
 * is mostly there rather than in the round trips.
 * <p>
 * A message that cannot be sent does not stop the others - see {@link SendMessageBatchResponse},
 * which names each rejection by its position in {@code messages}. The order of this list is
 * therefore part of the contract rather than an accident of how it was built.
 *
 * @param ern      queue ERN, or a bare queue name resolved against the caller's own account and namespace
 * @param messages the messages to send, in order
 */
public record SendMessageBatchRequest(String ern, List<Entry> messages) {

    /**
     * One message within a batch - the same fields a single send takes, minus the queue.
     *
     * @param body             the message body
     * @param attributes       the sender's own attributes, returned on the received message
     * @param systemAttributes euclid's envelope, carried across every hop
     * @param priority         "LOW", "MEDIUM" or "HIGH"; null or empty takes the queue's own
     */
    public record Entry(String body, Map<String, Variant> attributes,
                        Map<String, Variant> systemAttributes, String priority) {
    }

    /**
     * Creates a new instance of the Builder for constructing a SendMessageBatchRequest object.
     *
     * @return a new Builder instance for constructing SendMessageBatchRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SendMessageBatchRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The queue every message in this batch goes to.
         */
        private String ern;

        /**
         * The messages, in the order they are to be sent.
         */
        private final List<Entry> messages = new ArrayList<>();

        /**
         * Sets the queue every message in this batch goes to.
         *
         * @param ern the queue ERN or bare name
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Adds a message carrying only a body.
         *
         * @param body the message body
         * @return the builder instance
         */
        public Builder message(String body) {
            this.messages.add(new Entry(body, Map.of(), Map.of(), ""));
            return this;
        }

        /**
         * Adds a message.
         *
         * @param body the message body
         * @param attributes the sender's own attributes
         * @param priority "LOW", "MEDIUM" or "HIGH"; empty takes the queue's own
         * @return the builder instance
         */
        public Builder message(String body, Map<String, Variant> attributes, String priority) {
            this.messages.add(new Entry(body, attributes, Map.of(), priority));
            return this;
        }

        /**
         * Adds a message already assembled.
         *
         * @param entry the message
         * @return the builder instance
         */
        public Builder message(Entry entry) {
            this.messages.add(entry);
            return this;
        }

        /**
         * Builds and returns a new instance of SendMessageBatchRequest using the properties set on the Builder.
         *
         * @return a new SendMessageBatchRequest instance.
         */
        public SendMessageBatchRequest build() {
            return new SendMessageBatchRequest(ern, List.copyOf(messages));
        }
    }
}
