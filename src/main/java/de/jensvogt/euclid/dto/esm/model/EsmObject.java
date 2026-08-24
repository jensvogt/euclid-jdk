package de.jensvogt.euclid.dto.esm.model;

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
 * @param created     creation timestamp
 * @param modified    last-modified timestamp
 */
public record EsmObject(String ern, String bucketErn, String key, long size, String status, String contentType,
                         String md5Sum, String created, String modified) {
}
