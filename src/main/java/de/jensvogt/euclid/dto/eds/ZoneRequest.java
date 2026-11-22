package de.jensvogt.euclid.dto.eds;

/**
 * Names one zone - the body of {@code get-zone} and {@code delete-zone}.
 *
 * @param name the zone's apex
 */
public record ZoneRequest(String name) {
}
