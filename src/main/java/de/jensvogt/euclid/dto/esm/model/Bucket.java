package de.jensvogt.euclid.dto.esm.model;

import java.util.Map;

/**
 * Mirrors {@code Euclid::Dto::ESM::Bucket} from the Euclid server.
 *
 * @param owner    the user ID that owns the bucket
 * @param name     the bucket's name
 * @param ern      the bucket's ERN
 * @param size     total size in bytes of all objects currently in the bucket
 * @param objects  number of objects currently in the bucket
 * @param tags     the bucket's user-defined tags, keyed by tag key
 * @param created  creation timestamp
 * @param modified last-modified timestamp
 */
public record Bucket(String owner, String name, String ern, long size, long objects, Map<String, String> tags,
                     String created, String modified) {
}
