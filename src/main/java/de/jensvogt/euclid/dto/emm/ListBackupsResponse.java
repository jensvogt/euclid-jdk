package de.jensvogt.euclid.dto.emm;

import de.jensvogt.euclid.dto.emm.model.BackupFile;

import java.util.List;

/**
 * Response carrying the backups EMM has written, newest first, together with how the nightly
 * backup is set up to write the next one.
 *
 * <p>Answered from the backup directory rather than from a record the backup keeps, because the
 * directory is what a restore will have to work with. The directory is EMM's own: on an
 * installation whose manager runs on several hosts it lists the backups on the host EMM runs on.
 *
 * @param directory the backup directory
 * @param enabled   whether the scheduled backup is enabled
 * @param schedule  the cron expression the backup runs on, evaluated in UTC
 * @param keep      how many backups are kept before older ones are pruned
 * @param full      whether the next backup takes the bulk data too; always {@code false} when a
 *                  passphrase is configured, since an encrypted backup is built in memory
 * @param encrypted whether backups are sealed with a passphrase
 * @param backups   the backup files found, newest first
 */
public record ListBackupsResponse(String directory, boolean enabled, String schedule, long keep, boolean full,
                                  boolean encrypted, List<BackupFile> backups) {
}
