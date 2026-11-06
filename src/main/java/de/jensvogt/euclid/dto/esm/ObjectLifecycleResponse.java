package de.jensvogt.euclid.dto.esm;

/**
 * Where one object stands in its lifecycle, as archive-object and restore-object answer.
 *
 * @param ern            the object's ERN
 * @param bucketErn      the ERN of the bucket holding it
 * @param key            the object's key
 * @param lifecycleState {@code STANDARD} (online) or {@code ARCHIVED}
 * @param archivedAt     when it was archived (ISO 8601), or {@code null} if it never was
 * @param restoredAt     when it was last restored (ISO 8601), or {@code null}
 * @param restoredUntil  until when a restored object stays online before it is archived again
 *                       (ISO 8601), or {@code null}
 * @param archiveFile    the archive its bytes are kept in, or {@code null} for an object never archived
 */
public record ObjectLifecycleResponse(String ern, String bucketErn, String key, String lifecycleState, String archivedAt,
                                      String restoredAt, String restoredUntil, String archiveFile) {
}
