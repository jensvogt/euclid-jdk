package de.jensvogt.euclid.dto.esm.model;

/**
 * Mirrors {@code Euclid::Dto::ESM::Subscription} from the Euclid server: a standing registration
 * that announces a bucket's object events to a queue or a topic.
 *
 * @param ern       the subscription's own ERN, which unsubscribe takes
 * @param sourceErn the ERN of the bucket whose events are subscribed to
 * @param type      the target resource type, {@code "queue"} or {@code "topic"}
 * @param targetErn the ERN of the queue or topic the events are delivered to
 * @param created   creation timestamp
 * @param modified  last-modified timestamp
 */
public record Subscription(String ern, String sourceErn, String type, String targetErn, String created,
                           String modified) {
}
