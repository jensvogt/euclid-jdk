package de.jensvogt.euclid.dto.esm;

/**
 * The answer to get-object-size: one object's size, without its bytes.
 *
 * @param ern       the object's ERN
 * @param bucketErn the ERN of the bucket holding it
 * @param key       the object's key within the bucket
 * @param size      the object's size in bytes
 */
public record GetObjectSizeResponse(String ern, String bucketErn, String key, long size) {
}
