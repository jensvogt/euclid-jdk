package de.jensvogt.euclid.dto.esm.model;

/**
 * One version of an object, as list-object-versions reports it.
 *
 * @param versionId      the version's ID; empty for an object stored before versioning was turned
 *                       on, until it is replaced
 * @param latest         whether this is the current object
 * @param size           its size in bytes
 * @param md5Sum         MD5 of its bytes
 * @param contentType    its content type
 * @param status         its status, e.g. {@code COMPLETED}
 * @param lifecycleState {@code STANDARD} (online) or {@code ARCHIVED}
 * @param owner          the user who stored it
 * @param modified       when it was stored (ISO 8601)
 */
public record ObjectVersion(String versionId, boolean latest, long size, String md5Sum, String contentType,
                            String status, String lifecycleState, String owner, String modified) {
}
