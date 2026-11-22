package de.jensvogt.euclid.dto.esm.model;

import de.jensvogt.euclid.dto.com.Variant;

import java.util.Map;

/**
 * Mirrors {@code Euclid::Dto::ESM::Object} from the Euclid server (named {@code EsmObject} here
 * rather than {@code Object} to avoid shadowing {@code java.lang.Object}).
 *
 * @param ern         the object's ERN
 * @param bucketErn   ERN of the bucket the object belongs to
 * @param key         the object's key
 * @param size        size in bytes of the object
 * @param status      current object status
 * @param contentType the object's content type
 * @param md5Sum      MD5 checksum of the object's content
 * @param versionId   which version of the key this is; empty for an object stored without
 *                    versioning
 * @param encrypted   whether the object's bytes are stored encrypted at rest
 * @param attributes  the object's user-defined attributes, keyed by attribute name
 * @param lifecycleState {@code STANDARD} (online) or {@code ARCHIVED} - an archived object has to be
 *                    restored before it can be read, see
 *                    {@link de.jensvogt.euclid.module.esm.EuclidEsm#restoreObject(String, Long)}
 * @param restoredUntil until when a restored object stays online before it is archived again, or
 *                    {@code null}
 * @param created     creation timestamp
 * @param modified    last-modified timestamp
 * @param lastAccessed when the object was last used: its last read, or its last modification while
 *                    nothing has read it yet. This is the clock a lifecycle rule's
 *                    {@link LifecycleRule#archiveAfterDays()} runs against, which is why an object
 *                    that is written once and read daily is never archived. Reads are recorded
 *                    coarsely (about once an hour by default) so the busiest path the server has
 *                    does not become a write per read - good enough for a rule that decides in days,
 *                    not for an access log
 */
public record EsmObject(String ern, String bucketErn, String key, long size, String status, String contentType,
                        String md5Sum, String versionId, boolean encrypted, Map<String, Variant> attributes,
                        String lifecycleState, String restoredUntil, String created, String modified,
                        String lastAccessed) {
}
