package de.jensvogt.euclid.dto.esm;

/**
 * The answer to set-bucket-versioning.
 *
 * @param ern        the bucket's ERN
 * @param name       the bucket's name
 * @param versioning  whether the bucket keeps replaced objects as versions now
 * @param maxVersions the most non-current versions one key keeps now, 0 for every one. Reported
 *                    whichever of the two settings the request changed, since the server stores
 *                    them together and either one alone does not say what the bucket will do
 */
public record SetBucketVersioningResponse(String ern, String name, boolean versioning, long maxVersions) {
}
