package de.jensvogt.euclid.dto.esm;

import de.jensvogt.euclid.dto.esm.model.ObjectVersion;

import java.util.List;

/**
 * The answer to list-object-versions: every version of one key.
 *
 * @param bucketErn  the bucket's ERN
 * @param key        the object's key
 * @param ern        the object's ERN, which every version of the key shares
 * @param versioning whether the bucket has versioning on now
 * @param versions   the current object first, marked {@link ObjectVersion#latest()}, then the
 *                   non-current versions, newest first. No current one when it was deleted down to
 *                   its versions
 * @param total      how many versions there are, the current one included
 */
public record ListObjectVersionsResponse(String bucketErn, String key, String ern, boolean versioning,
                                         List<ObjectVersion> versions, long total) {
}
