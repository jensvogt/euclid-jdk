package de.jensvogt.euclid.dto.ead;

import de.jensvogt.euclid.dto.ead.model.AuditEvent;

import java.util.List;

/**
 * Response carrying a page of the audit trail, newest first, and how many entries match.
 *
 * <p>{@link #total()} is counted only as far as a pager can need it. An exact total means reading an
 * index entry per matching event, which on a trail of tens of millions is seconds of database time
 * spent beside a page of fifty - so EAD stops counting at a ceiling (currently 100,000) and says so
 * with {@link #totalExact()}. {@code false} means "at least this many", which is enough to know
 * there is a next page; ask {@link de.jensvogt.euclid.module.ead.EuclidEad#countEvents} for the real
 * number.
 *
 * @param events     the entries on the requested page, newest first
 * @param total      how many entries match, exactly or up to the ceiling - see {@code totalExact}
 * @param totalExact whether {@code total} is the exact number, or only a lower bound
 */
public record ListEventsResponse(List<AuditEvent> events, long total, boolean totalExact) {
}
