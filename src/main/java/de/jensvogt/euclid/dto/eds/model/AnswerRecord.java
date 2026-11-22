package de.jensvogt.euclid.dto.eds.model;

/**
 * One resource record of an {@link Answer}, as it would go out on the wire.
 *
 * @param name  the owner name, spelled the way the question asked for it
 * @param type  the type mnemonic, e.g. {@code "A"} or {@code "SOA"}
 * @param ttl   the TTL in seconds the record is served with
 * @param value the record data in presentation form, e.g. {@code "10.1.2.3"} or {@code "10 mail.corp.example."}
 */
public record AnswerRecord(String name, String type, long ttl, String value) {
}
