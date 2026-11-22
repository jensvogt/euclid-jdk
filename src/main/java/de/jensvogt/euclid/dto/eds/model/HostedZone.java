package de.jensvogt.euclid.dto.eds.model;

/**
 * Mirrors one zone EDS answers for ({@code Euclid::Database::Entity::EDS::HostedZone} on the Euclid
 * server), as {@code get-zone}, {@code list-zones} and {@code create-zone} describe it.
 *
 * <p>The SOA and NS records every zone answers with are made by the server, not stored, so they are
 * not counted in {@link #recordCount()}.
 *
 * @param name        the zone's apex, lowercased and without a trailing dot
 * @param ern         the zone's ERN, {@code ern:eds:<region>:<account>:<namespace>:zone:<name>} -
 *                    what grants are written against
 * @param namespace   the namespace that owns the zone
 * @param ttl         TTL in seconds of records that name none, and of negative answers
 * @param description free text
 * @param recordCount how many record sets the zone holds; 0 on the answer to {@code create-zone}
 * @param serial      the SOA serial: the time of the zone's last change in seconds since the epoch
 * @param created     ISO8601 timestamp of when the zone was created
 * @param modified    ISO8601 timestamp of the zone's last change, its own or a record's
 */
public record HostedZone(String name, String ern, String namespace, long ttl, String description,
                         long recordCount, long serial, String created, String modified) {
}
