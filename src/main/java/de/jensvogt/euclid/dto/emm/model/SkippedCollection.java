package de.jensvogt.euclid.dto.emm.model;

/**
 * A collection an import left alone, and why.
 *
 * <p>An import file is client-supplied input crossing a trust boundary, so EMM writes only the
 * collections it knows to belong to a module and skips everything else rather than writing wherever
 * the file happens to name.
 *
 * @param collection the collection's name, as the file gave it
 * @param reason     why it was skipped: {@code "unknown collection"},
 *                   {@code "excluded by \"modules\" filter"} or {@code "not a JSON array"}
 */
public record SkippedCollection(String collection, String reason) {
}
