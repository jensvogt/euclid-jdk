package de.jensvogt.euclid.dto.eap;

/**
 * The answer to drain-node: which node, and whether it is now draining.
 *
 * <p>Nothing has moved when this is answered. A drained node keeps running what it has and keeps
 * renewing; it is only refused <em>new</em> assignments, so its instances leave as they are
 * replaced rather than all at once.
 *
 * @param node    the node's name
 * @param drained {@code true} if the node is now refused new assignments, {@code false} if it was
 *                put back into service
 */
public record DrainNodeResponse(String node, boolean drained) {
}
