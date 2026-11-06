package de.jensvogt.euclid.dto.emm;

import de.jensvogt.euclid.dto.emm.model.ImportedCollection;
import de.jensvogt.euclid.dto.emm.model.SkippedCollection;

import java.util.List;
import java.util.Map;

/**
 * Response to import: what was written, collection by collection, and what was left alone.
 *
 * @param imported each collection written, by name, with how many documents it took
 * @param skipped  each collection left alone, with the reason
 */
public record ImportResponse(Map<String, ImportedCollection> imported, List<SkippedCollection> skipped) {
}
