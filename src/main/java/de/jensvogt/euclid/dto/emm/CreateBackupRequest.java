package de.jensvogt.euclid.dto.emm;

/**
 * Request to start a backup now rather than at the next scheduled time.
 *
 * @param full {@code true} to take the bulk data too for this backup, {@code false} to take the
 *             configuration only, or {@code null} - left out of the request - to let
 *             {@code euclid.modules.emm.backup.full} decide. Ignored when a backup passphrase is
 *             configured
 */
public record CreateBackupRequest(Boolean full) {
}
