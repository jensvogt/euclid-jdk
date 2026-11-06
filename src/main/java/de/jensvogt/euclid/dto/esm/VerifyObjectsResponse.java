package de.jensvogt.euclid.dto.esm;

import de.jensvogt.euclid.dto.esm.model.ObjectProblem;

import java.util.List;

/**
 * One page of verify-objects: which objects of a bucket have a file on disk that is not the one
 * their record describes.
 *
 * @param bucketErn the bucket's ERN
 * @param afterKey  the key this page started after
 * @param scanned   how many objects the page looked at
 * @param checked   how many of them had their file checked
 * @param skipped   how many were not checked: objects still being uploaded, archived ones, and
 *                  directory markers
 * @param problems  every object found with a bad file
 * @param nextKey   the key the next page starts after, or empty when this was the last page
 */
public record VerifyObjectsResponse(String bucketErn, String afterKey, long scanned, long checked, long skipped,
                                    List<ObjectProblem> problems, String nextKey) {
}
