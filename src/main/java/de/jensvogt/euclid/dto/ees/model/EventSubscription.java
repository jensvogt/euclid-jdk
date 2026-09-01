package de.jensvogt.euclid.dto.ees.model;

import java.util.Map;

/**
 * Mirrors {@code Euclid::Database::EventBus::Subscription}: one external subscriber's standing
 * interest in an event type.
 *
 * @param subscriber the name events are claimed under. Instances of one application share it
 *                   deliberately, so an event is processed once between them; two different
 *                   applications watching the same thing each get their own copy
 * @param eventType  the event type subscribed to, e.g. {@code "esm.object.modified"}
 * @param filter     exact-match key/value pairs an event payload must satisfy for this subscriber
 *                   to store it; empty means every event of this type
 * @param accountId  the account the subscriber belongs to - an event whose payload names a
 *                   different account is never stored for it
 * @param created    when the subscription was first registered
 * @param lastSeen   when the subscriber last claimed events
 */
public record EventSubscription(String subscriber, String eventType, Map<String, Object> filter, String accountId,
                                String created, String lastSeen) {
}
