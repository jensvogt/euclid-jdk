package de.jensvogt.euclid.dto.ets.model;

import java.util.List;

/**
 * Mirrors the transfer server definition {@code EtsServer.cpp}'s {@code toJson()} returns: an FTP or
 * SFTP listener fronting an ESM bucket. Clients authenticate with their EAM credentials and are
 * admitted if {@code userIds} names them or they belong to one of {@code userGroups}; whatever they
 * upload becomes an object in the bucket.
 *
 * @param serverId     name identifying the server, unique within its account and namespace - two
 *                     namespaces may each define an {@code "inbound"}, and they are different
 *                     servers
 * @param runtimeName  the name euclid runs this server under, issued once when it is defined and
 *                     held for as long as it exists. What a server is <em>called</em> is scoped to
 *                     an account and a namespace, but what euclid makes of it is not: a process
 *                     pool, a module row, a unix socket and a log channel are installation-wide, so
 *                     they are all built from this instead. It is what to look for in a module
 *                     list, in {@code ps} output or in a log channel, and what the
 *                     {@code --transfer-server} argument names
 * @param ern          the server's ERN
 * @param accountId    the account the server belongs to
 * @param namespace    the namespace the server belongs to, empty for one at the account root
 * @param region       the region the server runs in
 * @param protocol     transfer protocol, {@code "FTP"} or {@code "SFTP"}
 * @param address      address the server binds to
 * @param port         TCP port the server listens on
 * @param bucketName   name of the ESM bucket the server fronts
 * @param bucketErn    ERN of that bucket, resolved when the server was defined
 * @param homeDirectory key prefix template each client's session is rooted at, or empty for the
 *                     bucket root. Expanded per session with {@code {user}} standing for the EAM
 *                     user ID that logged in, so {@code "{user}"} gives every client its own corner
 *                     of the bucket the way an FTP server's per-user home always has, while empty
 *                     leaves all of them sharing one flat key space. A template rather than a
 *                     per-user mapping because a server admits users by group as well as by name,
 *                     and a map could not name a user who has not logged in yet
 * @param userIds      EAM user IDs allowed to log in
 * @param userGroups   EAM user groups whose members may log in
 * @param directories  directories every session should find under its home, created at login and
 *                     relative to the home prefix - so {@code "incoming/mix"} under a
 *                     {@code "{user}"} home is {@code "jvo/incoming/mix/"} for one client and
 *                     {@code "oju/incoming/mix/"} for another. Intermediate levels are created too,
 *                     and empty creates nothing. A transfer workflow expects a shape - an inbox to
 *                     deliver into, somewhere feedback comes back - and a client cannot be asked to
 *                     create it, since it delivers into a folder that has to be there already
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
public record TransferServer(String serverId, String runtimeName, String ern, String accountId, String namespace,
                             String region, String protocol,
                             String address, long port, String bucketName, String bucketErn, String homeDirectory,
                             List<String> userIds,
                             List<String> userGroups, List<String> directories, String desiredState, String state,
                             String hostKey, long pasvMin,
                             long pasvMax, String created, String modified) {
}
