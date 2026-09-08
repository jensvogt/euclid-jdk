package de.jensvogt.euclid.dto.eqs.model;

/**
 * One queue a redrive moved messages back to, and how many it moved there.
 *
 * <p>A dead letter queue can be shared by several source queues, in which case a redrive answers
 * with one of these per queue it returned messages to.
 *
 * @param queueErn the ERN (Entity Resource Name) of the queue the messages went back to
 * @param messages the number of messages moved to that queue
 */
public record RedriveTarget(String queueErn, long messages) {
}
