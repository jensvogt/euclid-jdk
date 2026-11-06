package de.jensvogt.euclid.dto.emm.model;

/**
 * One file of a backup EMM has written, as list-backups reports it.
 *
 * <p>A backup is one file per scope, all with the same name: the backup directory itself for what
 * belongs to no account, {@code <directory>/<account>/} for an account's own records and its root
 * namespace, {@code <directory>/<account>/<namespace>/} for a namespace. Each is a complete export
 * that {@link de.jensvogt.euclid.module.emm.EuclidEmm#importModules} restores on its own.
 *
 * @param name      the archive's file name, shared by every scope's file of one backup
 * @param path      the full path, on the host EMM runs on
 * @param accountId the account the file holds, or empty for the top scope
 * @param namespace the namespace the file holds, or empty for the top scope or an account's own
 * @param startedAt ISO8601 timestamp the backup started, read from the name
 * @param modified  ISO8601 timestamp the file was last written
 * @param size      the file's size in bytes
 * @param complete  {@code false} for an export that was never zipped - a backup being written right
 *                  now, or one that died part-way
 */
public record BackupFile(String name, String path, String accountId, String namespace, String startedAt,
                         String modified, long size, boolean complete) {
}
