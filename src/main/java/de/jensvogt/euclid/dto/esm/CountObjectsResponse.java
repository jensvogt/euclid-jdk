package de.jensvogt.euclid.dto.esm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * How many objects a bucket holds under a prefix, counted at the moment of asking.
 *
 * <p>Not to be confused with {@link GetObjectCountResponse}, which carries the bucket's stored
 * running total instead. This one is the answer to a query: exact, and narrowable to a prefix.
 *
 * @param ern                the ERN of the bucket the count belongs to
 * @param prefix             the prefix the count was taken under, echoed back because a bare
 *                           number does not say what it counted
 * @param includeDirectories whether the markers that stand for directories were counted
 * @param count              the number of matching objects
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CountObjectsResponse(String ern, String prefix, boolean includeDirectories, long count) {
}
