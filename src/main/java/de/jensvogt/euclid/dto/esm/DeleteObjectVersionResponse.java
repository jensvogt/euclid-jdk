package de.jensvogt.euclid.dto.esm;

/**
 * The answer to deleting one version of an object.
 *
 * @param ern              the object's ERN
 * @param versionId        the version that was deleted
 * @param currentVersionId the version the key holds now - the one promoted in its place when the
 *                         deleted version was the current one; empty when the key has no current
 *                         object
 */
public record DeleteObjectVersionResponse(String ern, String versionId, String currentVersionId) {
}
