package de.jensvogt.euclid.dto.eds;

import de.jensvogt.euclid.dto.eds.model.RecordSet;

import java.util.List;

/**
 * Response carrying a page of a zone's record sets.
 *
 * @param records the record sets on the requested page
 * @param total   how many sets match the filters, across every page
 */
public record ListRecordsResponse(List<RecordSet> records, long total) {
}
