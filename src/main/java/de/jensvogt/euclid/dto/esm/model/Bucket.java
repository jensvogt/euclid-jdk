package de.jensvogt.euclid.dto.esm.model;

/**
 * Mirrors {@code Euclid::Dto::ESM::Bucket} from the Euclid server.
 *
 * @param region   the region the bucket lives in
 * @param owner    the user ID that owns the bucket
 * @param name     the bucket's name
 * @param ern      the bucket's ERN
 * @param size     total size in bytes of all objects currently in the bucket
 * @param objects  number of objects currently in the bucket
 * @param created  creation timestamp
 * @param modified last-modified timestamp
 */
public record Bucket(String region, String owner, String name, String ern, long size, long objects, String created,
                      String modified) {
}
