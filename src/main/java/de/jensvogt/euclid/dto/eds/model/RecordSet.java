package de.jensvogt.euclid.dto.eds.model;

import java.util.List;

/**
 * Mirrors one stored record set ({@code Euclid::Database::Entity::EDS::Record} on the Euclid
 * server): every value of one type at one name, with the values as they were put.
 *
 * @param zone     the apex of the zone the set belongs to
 * @param name     the fully qualified owner name, lowercased, e.g. {@code "api.corp.example"} or {@code "*.dev.corp.example"}
 * @param type     the type mnemonic, e.g. {@code "A"}
 * @param ttl      TTL in seconds; 0 means the zone's
 * @param values   the values, in zone-file presentation form
 * @param created  ISO8601 timestamp of when the set was first written
 * @param modified ISO8601 timestamp of the set's last write
 */
public record RecordSet(String zone, String name, String type, long ttl, List<String> values,
                        String created, String modified) {

    /**
     * Copies {@code values} so the record stays immutable.
     */
    public RecordSet {
        values = values == null ? List.of() : List.copyOf(values);
    }
}
