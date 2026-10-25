package de.jensvogt.euclid.dto.eap.model;

/**
 * An object an application or platform owned and no longer declares, which an apply left in place
 * rather than removing - because it still held something, or because pruning was turned off.
 *
 * @param ern    the object's ERN
 * @param reason why it was kept, e.g. that the queue still has messages
 */
public record KeptResource(String ern, String reason) {
}
