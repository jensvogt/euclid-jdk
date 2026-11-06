package de.jensvogt.euclid.dto.esm;

/**
 * The answer to set-bucket-versioning.
 *
 * @param ern        the bucket's ERN
 * @param name       the bucket's name
 * @param versioning whether the bucket keeps replaced objects as versions now
 */
public record SetBucketVersioningResponse(String ern, String name, boolean versioning) {
}
