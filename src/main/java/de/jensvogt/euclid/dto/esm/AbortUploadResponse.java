package de.jensvogt.euclid.dto.esm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * What an abandoned upload was, and what became of the object it was writing.
 *
 * @param uploadId      the upload that was discarded
 * @param bucketErn     the bucket it was being written to
 * @param key           the key it was being written to
 * @param parts         how many staged parts were thrown away
 * @param objectRemoved whether the object row at that key went with the upload
 *                      <p>
 *                      True for a first upload, whose row described bytes that never arrived. False
 *                      for a re-upload, where the row is the previous version - still published,
 *                      still readable, and not this upload's to delete. Worth reading rather than
 *                      assuming: "the upload is gone" and "the object is gone" are different
 *                      outcomes, and a caller cleaning up after a failure needs to know which one
 *                      they got.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AbortUploadResponse(String uploadId, String bucketErn, String key, long parts, boolean objectRemoved) {
}
