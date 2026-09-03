package de.jensvogt.euclid.dto.eap.model;

import java.util.List;
import java.util.Map;

/**
 * Mirrors the application definition {@code EapServer.cpp}'s {@code toJson()} returns: an artifact
 * stored in an ESM bucket that euclid runs as a pool of processes.
 * <p>
 * Note that the names a caller deploys with are resolved to ERNs by the time they come back here -
 * a request names a bucket, an artifact and the buckets/queues to grant, and the stored definition
 * answers with {@code bucketErn}, {@code artifactKey} and {@code resources}.
 *
 * @param applicationId  name identifying the application, unique across the installation
 * @param ern            the application's ERN
 * @param accountId      the account the application belongs to
 * @param region         the region the application runs in
 * @param runtime        how the artifact is executed: {@code "JAVA"}, {@code "PYTHON"},
 *                       {@code "NODEJS"} or {@code "BINARY"}
 * @param bucketErn      ERN of the bucket holding the artifact, resolved when the application was
 *                       deployed
 * @param artifactKey    key of the artifact object within that bucket
 * @param version        version of the build currently deployed, e.g. {@code "1.4.0"} - empty for an
 *                       application defined before versions were recorded, until its next redeploy
 * @param md5Sum         MD5 of the artifact as it was when this version was deployed, which together
 *                       with the version answers "which build is running?" from the definition alone
 * @param command        command the artifact is run with
 * @param arguments      arguments passed to the command
 * @param environment    environment variables the processes are started with
 * @param resources      ERNs of the buckets and queues the application is granted access to,
 *                       resolved from the names it was deployed with
 * @param userId         the identity the application runs and signs its own calls as - either a
 *                       user named at deployment, or a technical principal EAP minted and removes
 *                       along with the application
 * @param minInstances   smallest number of processes kept running
 * @param maxInstances   largest number of processes run
 * @param readyTimeoutMs milliseconds a process is given to report ready
 * @param desiredState   what the application should be doing, {@code "RUNNING"} or {@code "STOPPED"} -
 *                       recorded intent, which euclid-mgr's reconciler acts on
 * @param state          what the application is observed to be doing, derived live from how many
 *                       instances are up, so it can lag {@code desiredState} until the reconciler
 *                       catches up
 * @param instances      number of instances currently running
 * @param created        creation timestamp
 * @param modified       last-modified timestamp
 */
public record Application(String applicationId, String ern, String accountId, String region, String runtime,
                          String bucketErn, String artifactKey, String version, String md5Sum, String command,
                          List<String> arguments,
                          Map<String, String> environment, List<String> resources, String userId, long minInstances,
                          long maxInstances, long readyTimeoutMs, String desiredState, String state, long instances,
                          String created, String modified) {
}
