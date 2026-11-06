package de.jensvogt.euclid.dto.esm;

import de.jensvogt.euclid.dto.esm.model.LifecycleRule;

import java.util.List;

/**
 * A bucket's lifecycle rules, as set-bucket-lifecycle, get-bucket-lifecycle and
 * delete-bucket-lifecycle answer with them.
 *
 * @param ern   the bucket's ERN
 * @param name  the bucket's name
 * @param rules the bucket's rules now; empty when it has none
 */
public record BucketLifecycleResponse(String ern, String name, List<LifecycleRule> rules) {
}
