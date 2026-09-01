package de.jensvogt.euclid.dto.ets.model;

import java.util.List;

/**
 * Mirrors the transfer server definition {@code EtsServer.cpp}'s {@code toJson()} returns: an FTP or
 * SFTP listener fronting an ESM bucket. Clients authenticate with their EAM credentials and are
 * admitted if {@code userIds} names them or they belong to one of {@code userGroups}; whatever they
 * upload becomes an object in the bucket.
 *
 * @param serverId     name identifying the server, unique across the installation
 * @param ern          the server's ERN
 * @param accountId    the account the server belongs to
 * @param region       the region the server runs in
 * @param protocol     transfer protocol, {@code "FTP"} or {@code "SFTP"}
 * @param address      address the server binds to
 * @param port         TCP port the server listens on
 * @param bucketName   name of the ESM bucket the server fronts
 * @param bucketErn    ERN of that bucket, resolved when the server was defined
 * @param userIds      EAM user IDs allowed to log in
 * @param userGroups   EAM user groups whose members may log in
 * @param desiredState what the server should be doing, {@code "RUNNING"} or {@code "STOPPED"} -
 *                     recorded intent, which euclid-mgr's reconciler acts on
 * @param state        what the server is observed to be doing, read live from the manager's
 *                     published instances rather than stored, so it can lag {@code desiredState}
 *                     until the reconciler catches up
 * @param hostKey      SFTP only: private SSH host key file, generated on first start if absent
 * @param pasvMin      FTP only: lowest passive data port
 * @param pasvMax      FTP only: highest passive data port
 * @param created      creation timestamp
 * @param modified     last-modified timestamp
 */
public record TransferServer(String serverId, String ern, String accountId, String region, String protocol,
                             String address, long port, String bucketName, String bucketErn, List<String> userIds,
                             List<String> userGroups, String desiredState, String state, String hostKey, long pasvMin,
                             long pasvMax, String created, String modified) {
}
