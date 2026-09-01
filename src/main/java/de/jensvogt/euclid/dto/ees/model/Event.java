package de.jensvogt.euclid.dto.ees.model;

import java.util.Map;

/**
 * Mirrors {@code Euclid::Database::EventEnvelope}: one event claimed from the bus.
 * <p>
 * An event stays claimed - invisible to any other claimer sharing this subscriber name - until its
 * visibility timeout runs out, and is only removed by acknowledging it. A consumer that crashes
 * mid-work therefore loses nothing: the event becomes claimable again.
 *
 * @param eventId      the ID to acknowledge this event with
 * @param eventType    the event type, e.g. {@code "esm.object.created"}
 * @param sourceModule the module that published it, e.g. {@code "esm"}
 * @param payload      the event body, whose shape depends on {@code eventType} - the same fields a
 *                     subscription filter matches against
 * @param attempts     how many times this event has been claimed, including this one; above one
 *                     means an earlier claim was never acknowledged
 * @param created      when the event was published
 */
public record Event(String eventId, String eventType, String sourceModule, Map<String, Object> payload, long attempts,
                    String created) {
}
