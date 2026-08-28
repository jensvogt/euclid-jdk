package de.jensvogt.euclid.dto.ens.model;

/**
 * Mirrors {@code Euclid::Dto::ENS::Subscription} from the Euclid server: a target resource
 * subscribed to receive messages published to a topic.
 *
 * @param ern       the subscription's ERN
 * @param sourceErn ERN of the topic messages are published to
 * @param type      delivery protocol, e.g. "SQS"
 * @param targetErn ERN of the delivery target; for type "SQS", an EQS queue ERN
 * @param created   creation timestamp
 * @param modified  last-modified timestamp
 */
public record Subscription(String ern, String sourceErn, String type, String targetErn, String created,
                            String modified) {
}
