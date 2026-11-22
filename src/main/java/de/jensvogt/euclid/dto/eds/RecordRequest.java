package de.jensvogt.euclid.dto.eds;

/**
 * Names one record set - the body of {@code delete-record}.
 *
 * @param zone the zone's apex
 * @param name the owner name, in any of the forms {@link PutRecordRequest#name()} accepts
 * @param type the type mnemonic, e.g. {@code "A"}
 */
public record RecordRequest(String zone, String name, String type) {
}
