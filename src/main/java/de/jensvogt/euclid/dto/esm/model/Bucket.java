package de.jensvogt.euclid.dto.esm.model;

import java.util.Map;

/**
 * Mirrors {@code Euclid::Dto::ESM::Bucket} from the Euclid server.
 *
 * @param owner            the user ID that owns the bucket
 * @param name             the bucket's name
 * @param ern              the bucket's ERN
 * @param size             total size in bytes of all objects currently in the bucket
 * @param objects          number of objects currently in the bucket
 * @param directories      number of directory keys in the bucket, which {@code objects} leaves out
 * @param tags             the bucket's user-defined tags, keyed by tag key
 * @param encrypted        whether objects put into the bucket are encrypted at rest
 * @param encryptionKeyErn the EKM key they are encrypted with, empty when the bucket is not encrypted
 * @param internal         whether the bucket is hidden from ordinary listings - see
 *                         {@link de.jensvogt.euclid.module.esm.EuclidEsm#setBucketInternal}
 * @param priority         the priority the notifications this bucket sends are given, empty when it
 *                         sets none. Nothing about the bucket depends on it - see
 *                         {@link de.jensvogt.euclid.module.esm.EuclidEsm#setBucketPriority}
 * @param stopped          whether the bucket's events are being held rather than delivered - see
 *                         {@link de.jensvogt.euclid.module.esm.EuclidEsm#stopBucket}
 * @param created          creation timestamp
 * @param modified         last-modified timestamp
 */
public record Bucket(String owner, String name, String ern, long size, long objects, long directories,
                     Map<String, String> tags, boolean encrypted, String encryptionKeyErn, boolean internal,
                     String priority, boolean stopped, String created, String modified) {
}
