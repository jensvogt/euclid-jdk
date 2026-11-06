package de.jensvogt.euclid.dto.emm.model;

/**
 * What an import did with one collection of the document it was given.
 *
 * @param imported how many documents were upserted
 * @param failed   how many could not be written; 0 when the server reported none
 */
public record ImportedCollection(long imported, long failed) {
}
