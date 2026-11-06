package de.jensvogt.euclid.dto.esm.model;

/**
 * An object whose file on disk is not the one its record describes, as verify-objects reports it -
 * what a crash before the files were synced to disk leaves behind: a record saying COMPLETED with a
 * size and an MD5, and nothing, or nothing like it, on disk.
 *
 * @param key              the object's key
 * @param ern              the object's ERN
 * @param problem          {@code missing}, {@code empty}, {@code short}, {@code long},
 *                         {@code mismatch} (an encrypted file whose header records another length)
 *                         or {@code unreadable}
 * @param recordedSize     the size the record says the object has
 * @param fileSize         the size of the file found, 0 when there is none
 * @param expectedFileSize the size the file should have - larger than {@code recordedSize} for an
 *                         encrypted object, whose file carries a header
 * @param internalName     the file's name on disk
 * @param internalDir      the directory it is kept in
 * @param created          when the object was created (ISO 8601)
 * @param detail           why, for {@code unreadable}; otherwise {@code null}
 */
public record ObjectProblem(String key, String ern, String problem, long recordedSize, long fileSize,
                            long expectedFileSize, String internalName, String internalDir, String created,
                            String detail) {
}
