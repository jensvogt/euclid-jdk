package de.jensvogt.euclid.dto.emm;

/**
 * Response to create-backup, which answers as soon as the backup has started (HTTP 202) rather
 * than when it is done.
 *
 * @param name      the archive name the backup writes, in the backup directory and in every
 *                  account's and namespace's directory under it
 * @param directory the backup directory, on the host EMM runs on
 * @param startedAt ISO8601 timestamp the backup started
 * @param full      whether this backup takes the bulk data
 * @param encrypted whether it is sealed with the configured passphrase
 */
public record CreateBackupResponse(String name, String directory, String startedAt, boolean full,
                                   boolean encrypted) {
}
