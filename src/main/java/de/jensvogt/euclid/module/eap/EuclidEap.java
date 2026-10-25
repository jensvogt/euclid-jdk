package de.jensvogt.euclid.module.eap;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jensvogt.euclid.auth.CredentialsFileTokens;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.auth.SigningScheme;
import de.jensvogt.euclid.auth.SigningSchemeSelectable;
import de.jensvogt.euclid.auth.TokenRefreshable;
import de.jensvogt.euclid.dto.eap.ApplicationRequest;
import de.jensvogt.euclid.dto.eap.ApplyInfrastructureResponse;
import de.jensvogt.euclid.dto.eap.CopyApplicationRequest;
import de.jensvogt.euclid.dto.eap.CreateApplicationRequest;
import de.jensvogt.euclid.dto.eap.ListApplicationsRequest;
import de.jensvogt.euclid.dto.eap.ListNodesResponse;
import de.jensvogt.euclid.dto.eap.RedeployApplicationRequest;
import de.jensvogt.euclid.dto.eap.ScaleApplicationRequest;
import de.jensvogt.euclid.dto.eap.RestartApplicationResponse;
import de.jensvogt.euclid.dto.eap.SetLogLevelRequest;
import de.jensvogt.euclid.dto.eap.EgressLimitResponse;
import de.jensvogt.euclid.dto.eap.EgressPermitResponse;
import de.jensvogt.euclid.dto.eap.ListEgressSitesResponse;
import de.jensvogt.euclid.dto.eap.RebalanceApplicationResponse;
import de.jensvogt.euclid.dto.eap.SetLogLevelResponse;
import de.jensvogt.euclid.dto.eap.UpdateApplicationRequest;
import de.jensvogt.euclid.dto.eap.model.Application;
import de.jensvogt.euclid.dto.eap.model.ApplicationEndpoint;
import de.jensvogt.euclid.dto.eap.model.ApplicationType;
import de.jensvogt.euclid.dto.eap.model.EgressLimits;
import de.jensvogt.euclid.dto.eap.model.EgressSite;
import de.jensvogt.euclid.dto.eap.model.KeptResource;
import de.jensvogt.euclid.dto.eap.model.Node;
import de.jensvogt.euclid.dto.eap.model.NodeApplication;
import de.jensvogt.euclid.dto.eap.model.Platform;
import de.jensvogt.euclid.dto.eap.model.UpdatedResource;
import de.jensvogt.euclid.exception.EuclidServiceException;
import de.jensvogt.euclid.http.EuclidHttpClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * EAP (application platform) operations for an authenticated
 * {@link de.jensvogt.euclid.module.eam.EuclidSession}. Mirrors euclid-cli's {@code EapCli}.
 * <p>
 * An application is an artifact stored in an ESM bucket that euclid runs as a pool of processes.
 * Every action here decides which code euclid executes and under whose identity it does so - about
 * as privileged as this system gets - so the server requires administrator rights for all of them
 * and answers HTTP 403 otherwise.
 * <p>
 * Deploying resolves names eagerly: the bucket, the artifact within it, and every bucket or queue
 * named as a resource grant must already exist, so a typo is a rejected deployment rather than an
 * application that starts and is then denied everything. Those names resolve in the account and
 * namespace being deployed into - the same pair the application's technical principal is granted
 * and runs in - so a name means the deployer's own bucket or queue rather than whoever else in the
 * installation has one called that.
 * <p>
 * Starting and stopping only record intent. {@link #startApplication} and {@link #stopApplication}
 * set {@code desiredState}; euclid-mgr's reconciler is what launches or tears down the processes,
 * so {@link Application#state()} can lag {@link Application#desiredState()} briefly after either.
 * {@link #restartApplication} records intent too, without touching {@code desiredState} at all.
 */
public final class EuclidEap implements TokenRefreshable, SigningSchemeSelectable {

    /**
     * A singleton instance of {@code ObjectMapper} from the Jackson library used for
     * serializing Java objects to JSON and deserializing JSON to Java objects.
     * <p>
     * Configured to leave null fields out of the serialized request entirely rather than writing
     * them as {@code null}. EAP distinguishes an absent field from a present one:
     * update-application only touches the fields it receives, and sending {@code "arguments": null}
     * would be read as an empty list and silently clear the application's argument list rather than
     * leave it alone.
     */
    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    /**
     * The Euclid service every request from this class is addressed to, sent as the
     * {@code x-euclid-target} header.
     */
    private static final String TARGET = "eap";

    /**
     * The base URL of the Euclid server this instance talks to.
     */
    private final String baseUrl;

    /**
     * Supplies the bearer token for each request, used when no SigV4 access key is configured.
     *
     * <p>A supplier rather than a string so that a token which expires can be replaced without
     * rebuilding the client - see {@link TokenRefreshable#token(Supplier)}. A client built inside an
     * application euclid deployed follows the credentials file euclid rewrites; anywhere else it
     * holds a supplier returning the token it was given - see
     * {@link CredentialsFileTokens#forClient(String, String)}.
     */
    private volatile Supplier<String> token;

    /**
     * The region requests are made in.
     */
    private final String region;

    /**
     * The account requests are made on behalf of.
     */
    private final String accountId;

    /**
     * The user requests are made on behalf of.
     */
    private final String userId;

    /**
     * Public identifier of the SigV4 access key, or {@code null} to authenticate with the token.
     */
    private final String accessKeyId;

    /**
     * Secret paired with {@link #accessKeyId}, or {@code null} to authenticate with the token.
     */
    private final String secretAccessKey;

    /**
     * The namespace requests are scoped to, sent as the {@code x-euclid-namespace} header. A
     * technical principal EAP mints for an application is created in this namespace.
     */
    private final String nameSpace;

    /**
     * The scheme requests are signed with when an access key is configured.
     *
     * <p>Defaults to SigV4, which is what euclid has always accepted; a caller pointed at a server
     * that understands RFC 9421 switches it with {@link #signingScheme(SigningScheme)}. Volatile
     * because that call can come from a different thread than the requests it affects.
     */
    private volatile SigningScheme signingScheme = SigningScheme.SIGV4;

    /**
     * The HTTP client used for every request, pre-configured with this session's TLS trust.
     */
    private final EuclidHttpClient httpClient;

    /**
     * Constructs an EAP client. Normally obtained from
     * {@link de.jensvogt.euclid.module.eam.EuclidSession#eap()} rather than built directly.
     *
     * @param baseUrl         the base URL of the Euclid server
     * @param token           the bearer token issued at login
     * @param region          the region requests are made in
     * @param accountId       the account requests are made on behalf of
     * @param userId          the user requests are made on behalf of
     * @param accessKeyId     public identifier of the SigV4 access key, or {@code null} for token auth
     * @param secretAccessKey secret paired with {@code accessKeyId}, or {@code null} for token auth
     * @param caCertPath      path to an additional PEM CA certificate to trust, or {@code null}
     * @param nameSpace       the namespace requests are scoped to, or {@code null} if unscoped
     */
    public EuclidEap(String baseUrl, String token, String region, String accountId, String userId,
                     String accessKeyId, String secretAccessKey, String caCertPath, String nameSpace) {
        this.baseUrl = baseUrl;
        this.token = CredentialsFileTokens.forClient(token, userId);
        this.region = region;
        this.accountId = accountId;
        this.userId = userId;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.nameSpace = nameSpace;
        // The header factory is what lets a request whose token or signature expired in flight be
        // built again and sent once more - see EuclidHttpClient#headerFactory.
        this.httpClient = new EuclidHttpClient(caCertPath).headerFactory(this::requestHeaders);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void token(Supplier<String> token) {
        this.token = Objects.requireNonNull(token, "token supplier must not be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void signingScheme(SigningScheme signingScheme) {
        this.signingScheme = Objects.requireNonNull(signingScheme, "signing scheme must not be null");
    }

    /**
     * Deploys a new application. It is created stopped - call {@link #startApplication} to run it.
     * <p>
     * Everything named is resolved now rather than at start-up, so a bucket, artifact, granted
     * resource or user that does not exist is reported here as HTTP 404. An application ID already
     * in use <em>in this account and namespace</em> is refused with HTTP 409 - two namespaces may
     * each deploy a {@code "billing"}, and they are different applications.
     * <p>
     * Leaving {@code user} unset is the usual choice: EAP mints a technical principal for the
     * application, grants it exactly the named resources, and removes it again when the application
     * is deleted - which is what keeps an application from borrowing a person's credentials. Naming
     * an existing user instead requires that user to already have an access key to sign with.
     *
     * @param request the application definition; unset fields take the server's defaults
     * @return the stored definition, with names resolved to ERNs
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public Application createApplication(CreateApplicationRequest request)
            throws IOException, InterruptedException {
        return toApplication(post("create-application", OBJECT_MAPPER.writeValueAsString(request)));
    }

    /**
     * Changes a deployed application. Only the fields set on the request are sent, and the server
     * only touches the fields it receives, so one setting can be changed without resending the
     * whole definition.
     * <p>
     * A field that is set replaces the current value outright - passing {@code arguments}
     * substitutes the whole list rather than appending to it. Changing {@code buckets} or
     * {@code queues} also rewrites the grants on the principal the application runs as, provided
     * that principal is one EAP minted itself.
     *
     * @param request the change to apply; only {@code applicationId} is required
     * @return the stored definition after the change
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public Application updateApplication(UpdateApplicationRequest request)
            throws IOException, InterruptedException {
        return toApplication(post("update-application", OBJECT_MAPPER.writeValueAsString(request)));
    }

    /**
     * Defines the same application again in another namespace, leaving the original alone and
     * running. This is how a build is promoted - development to integration, integration to
     * production - without taking the namespace it came from out of service.
     * <p>
     * To move an application rather than copy it, set the namespace on
     * {@link #updateApplication(UpdateApplicationRequest)}: that takes the definition with it, so
     * what ran in the old namespace stops running there.
     * <p>
     * The copy runs the same artifact - bucket, key and checksum are taken as they stand - and is
     * given its own runtime name and its own technical principal with its own access key, because
     * both are installation-wide and cannot be shared. Revoking the copy's credentials therefore
     * leaves the original running. An application told to run as a named user keeps that user.
     * <p>
     * It is created stopped, whatever the original is doing: a copy that started itself would put
     * a second consumer on the target namespace's queues the moment this returned. Start it with
     * {@link #startApplication(String)} once you have looked at it.
     *
     * @param request the application to copy and where to copy it to
     * @return the stored definition of the copy
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws de.jensvogt.euclid.exception.EuclidServiceException with status 404 if the
     * application, the target namespace, or a bucket or queue the original may use has no
     * counterpart of that name in the target namespace; with status 409 if an application of that
     * name is already defined there
     */
    public Application copyApplication(CopyApplicationRequest request)
            throws IOException, InterruptedException {
        return toApplication(post("copy-application", OBJECT_MAPPER.writeValueAsString(request)));
    }

    /**
     * Changes how many instances an application runs, without restarting the ones it has.
     * <p>
     * {@link #updateApplication(UpdateApplicationRequest)} can set the same two fields, but it
     * writes the whole definition and stamps the modification date - and the manager restarts a
     * pool whose application changed since it started it. Scaling that way stops every running
     * instance and starts it again, which is the opposite of what asking for capacity means and
     * worst at the moment it is asked for.
     * <p>
     * What is set is the range the autoscaler works within, not a count. The manager scales toward
     * it on its next reconcile; nothing is started or stopped by this call. Setting both bounds to
     * the same number pins the pool at that size - {@code ScaleApplicationRequest.Builder.instances}
     * is the shorthand for it.
     *
     * @param request the application to scale and the bounds to set; a bound left null is left alone
     * @return the stored definition after the change
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws de.jensvogt.euclid.exception.EuclidServiceException with status 404 if the
     * application does not exist; with status 400 if neither bound was given, if a floor of zero
     * was asked for - stop the application instead - or if the bounds would leave a floor above a
     * ceiling, which is checked against the bound as it will stand rather than as it is
     */
    public Application scaleApplication(ScaleApplicationRequest request)
            throws IOException, InterruptedException {
        return toApplication(post("scale-application", OBJECT_MAPPER.writeValueAsString(request)));
    }

    /**
     * Deploys a new build of an application, restarting its instances onto it.
     * <p>
     * The artifact has to be in the application's bucket already - upload it with
     * {@code EuclidEsm} first - and this is what makes it the deployed one: EAP records the new
     * version and the artifact's checksum, and euclid-mgr reads the changed definition as a new
     * revision and restarts the pool within a few seconds. An application that is stopped stays
     * stopped and comes up on the new build when it is next started.
     * <p>
     * Refused unless it is genuinely a new build - a version that differs from the one deployed,
     * carrying bytes that differ too. {@link #updateApplication(UpdateApplicationRequest)} deploys
     * either on purpose.
     *
     * @param request the build to deploy; only {@code applicationId} is required
     * @return the stored definition, carrying the new version and checksum
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws de.jensvogt.euclid.exception.EuclidServiceException with status 409 if the version or
     * the artifact is the one already deployed
     */
    public Application redeployApplication(RedeployApplicationRequest request)
            throws IOException, InterruptedException {
        return toApplication(post("redeploy-application", OBJECT_MAPPER.writeValueAsString(request)));
    }

    /**
     * Lists every deployed application.
     *
     * @return the deployed applications
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public List<Application> listApplications() throws IOException, InterruptedException {
        return listApplications("");
    }

    /**
     * Lists the applications whose ID starts with the given prefix.
     *
     * @param prefix only applications whose ID starts with this prefix are returned; empty lists
     *               them all
     * @return the matching applications
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public List<Application> listApplications(String prefix) throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(ListApplicationsRequest.builder().prefix(prefix).build());
        JsonNode root = post("list-applications", body);

        List<Application> applications = new ArrayList<>();
        JsonNode applicationsNode = root.get("applications");
        if (applicationsNode != null && applicationsNode.isArray()) {
            for (JsonNode applicationNode : applicationsNode) {
                applications.add(toApplication(applicationNode));
            }
        }
        return applications;
    }

    /**
     * Retrieves a single application by ID.
     *
     * @param applicationId the ID of the application
     * @return the application's definition
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public Application getApplication(String applicationId) throws IOException, InterruptedException {
        return toApplication(post("get-application", applicationBody(applicationId)));
    }

    /**
     * Deletes an application's definition, which is also what stops it: the reconciler runs whatever
     * is defined and RUNNING, so a definition that no longer exists is torn down on its next tick.
     * <p>
     * A technical principal EAP minted for this application goes with it - a credential outliving
     * the thing it was issued to is exactly the orphan that arrangement exists to avoid. A user
     * named at deployment is left alone.
     *
     * @param applicationId the ID of the application to delete
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public void deleteApplication(String applicationId) throws IOException, InterruptedException {
        post("delete-application", applicationBody(applicationId));
    }

    /**
     * Records that an application should be running. euclid-mgr's reconciler is what starts the
     * processes, so the returned {@link Application#state()} may still read {@code "STOPPED"} with
     * {@link Application#instances()} at zero even though {@link Application#desiredState()} is now
     * {@code "RUNNING"}.
     *
     * @param applicationId the ID of the application to start
     * @return the stored definition, with its new desired state
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public Application startApplication(String applicationId) throws IOException, InterruptedException {
        return toApplication(post("start-application", applicationBody(applicationId)));
    }

    /**
     * Records that an application should be stopped, leaving its definition in place. The reconciler
     * tears the processes down; see {@link #startApplication} on the lag between desired and
     * observed state.
     *
     * @param applicationId the ID of the application to stop
     * @return the stored definition, with its new desired state
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public Application stopApplication(String applicationId) throws IOException, InterruptedException {
        return toApplication(post("stop-application", applicationBody(applicationId)));
    }

    /**
     * Records that a running application's instances should be started again, without changing
     * anything about it.
     * <p>
     * euclid-mgr stops the whole pool on its next reconcile and starts it straight back up from
     * the current definition - the same thing it does after a redeploy, with nothing new to pick
     * up. The artifact, the environment and the credentials all come back as they were, so this is
     * for an instance that has to do its startup again rather than a way to deploy anything; see
     * {@link #redeployApplication} for that.
     * <p>
     * Deliberately not {@link #stopApplication} followed by {@link #startApplication}: between
     * those two the application's desired state is {@code "STOPPED"}, so a caller that fails in
     * between leaves it down. Here the desired state stays {@code "RUNNING"} throughout, and an
     * application that is already stopped is refused with HTTP 400 rather than started.
     *
     * @param applicationId the ID of the application to restart
     * @return what was recorded, and how many instances the manager is about to cycle
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public RestartApplicationResponse restartApplication(String applicationId) throws IOException, InterruptedException {
        return toRestartApplicationResponse(post("restart-application", applicationBody(applicationId)));
    }

    /**
     * Asks the manager to spread an application's instances over the nodes again: onto a node that
     * joined since they were placed, off one that is busier than the others.
     * <p>
     * The manager does this by itself after an instance goes away; this is for every other time - a
     * new node, or a pool that ended up lopsided and was never scaled since. Like
     * {@link #restartApplication}, it only records the request. The manager then moves one instance
     * per pass, each move withdrawing a slot and placing its replacement, until no move would help -
     * so the pool is one instance short while a move is under way, never more, and one that is
     * already balanced is left exactly as it is. Done even where automatic rebalancing is turned off
     * with {@code euclid.modules.eap.rebalance}.
     *
     * @param applicationId the ID of the application to rebalance
     * @return that the request was recorded, and how many instances there are to spread
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws de.jensvogt.euclid.exception.EuclidServiceException with status 404 if the
     * application does not exist; with status 400 for a JOB, since moving a run would start it again
     */
    public RebalanceApplicationResponse rebalanceApplication(String applicationId) throws IOException, InterruptedException {
        JsonNode node = post("rebalance-application", applicationBody(applicationId));
        return new RebalanceApplicationResponse(textOrNull(node, "applicationId"),
                node.path("rebalancing").asBoolean(false), node.path("instances").asLong(0));
    }

    /**
     * Makes the installation match the application's own infrastructure declaration.
     * <p>
     * The declaration is a file the application has already stored beside its artifact -
     * {@code <applicationId>.euclid.json} in the bucket it deploys from - which names the queues,
     * topics and buckets it owns and the ones belonging to others that it reaches. This applies it:
     * creates what is missing, brings declared settings of what exists into line, grants the access
     * it asks for, and <b>deletes what this application owned and the declaration no longer
     * names</b> - but only when empty: a queue with messages, a bucket with objects or a topic with
     * subscriptions is kept and listed in {@link ApplyInfrastructureResponse#kept()}.
     * <p>
     * Applying is idempotent and changes nothing about the running instances - the modification date
     * is deliberately not stamped, so euclid-mgr does not read it as a new revision and cycle the
     * pool. It happens on its own whenever the application is created, updated or redeployed; this
     * is for reconciling without a deploy.
     * <p>
     * An application with no declaration stored is answered rather than refused, with
     * {@code declared} false. A declaration that cannot be applied is an error: one that names a
     * resource belonging to another application, or claims one it does not own, is refused with
     * HTTP 400 rather than partly applied.
     *
     * @param applicationId the ID of the application, in the session's namespace
     * @return what was created, removed, granted and revoked
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ApplyInfrastructureResponse applyInfrastructure(String applicationId) throws IOException, InterruptedException {
        return applyInfrastructure(applicationId, true);
    }

    /**
     * Applies the application's stored declaration, choosing whether owned objects it no longer
     * declares are removed.
     *
     * @param applicationId the ID of the application, in the session's namespace
     * @param prune         true to remove owned objects the declaration no longer names, when they
     *                      are empty; false to keep them all, listed in
     *                      {@link ApplyInfrastructureResponse#kept()}
     * @return what was created, adopted, updated, removed, kept, granted and revoked
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ApplyInfrastructureResponse applyInfrastructure(String applicationId, boolean prune)
            throws IOException, InterruptedException {
        return toApplyInfrastructureResponse(post("apply-infrastructure", infrastructureBody(applicationId, null, prune)));
    }

    /**
     * Works out what {@link #applyInfrastructure(String)} would do with the stored declaration, and
     * changes nothing.
     *
     * @param applicationId the ID of the application, in the session's namespace
     * @return what would be created, adopted, updated, removed, kept, granted and revoked
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ApplyInfrastructureResponse planInfrastructure(String applicationId) throws IOException, InterruptedException {
        return planInfrastructure(applicationId, null, true);
    }

    /**
     * Works out what a declaration would do for an application, and changes nothing - what
     * {@code euclid-cli eap apply --dry-run} does.
     * <p>
     * A plan is an action of its own rather than a flag on apply-infrastructure: an EAP from before
     * plans existed ignores a field it does not know and would apply for real, removals included,
     * where an action it does not know it refuses.
     * <p>
     * A declaration handed over is only planned with, never stored. To apply it, store it beside the
     * artifact as {@code <applicationId>.euclid.json} - {@code EuclidEsm.putObject} into the
     * application's bucket - and call {@link #applyInfrastructure(String)}; every later create,
     * update and redeploy applies that stored one again.
     *
     * @param applicationId   the ID of the application, in the session's namespace
     * @param declarationJson the declaration to plan with, as JSON, or {@code null} for the stored
     *                        one. One that names a different application is refused
     * @param prune           whether owned objects no longer declared would be removed
     * @return what would be created, adopted, updated, removed, kept, granted and revoked
     * @throws IOException if an I/O error occurs during the operation, or the declaration is not JSON
     * @throws InterruptedException if the operation is interrupted
     */
    public ApplyInfrastructureResponse planInfrastructure(String applicationId, String declarationJson, boolean prune)
            throws IOException, InterruptedException {
        return toApplyInfrastructureResponse(post("plan-infrastructure", infrastructureBody(applicationId, declarationJson, prune)));
    }

    /**
     * Makes the installation match a platform declaration: the shared objects of the namespace no
     * application owns - the bucket an FTP server writes into, a topic several services publish to.
     * <p>
     * Applied by the same engine as an application's declaration, with three differences: it is
     * handed over rather than read from beside an artifact, since a platform has none; it owns and
     * never uses, since it has no principal to grant to; and what it owns is tagged
     * {@code platform:<name>}. Applications reach those objects with {@code uses}.
     *
     * @param name            the platform's name; {@code "platform"} when {@code null} or empty
     * @param declarationJson the declaration, as JSON. One that names an application, or lists
     *                        {@code uses}, is refused with HTTP 400
     * @param prune           whether owned objects no longer declared are removed, when empty
     * @return what was created, adopted, updated, removed and kept
     * @throws IOException if an I/O error occurs during the operation, or the declaration is not JSON
     * @throws InterruptedException if the operation is interrupted
     */
    public ApplyInfrastructureResponse applyPlatform(String name, String declarationJson, boolean prune)
            throws IOException, InterruptedException {
        return toApplyInfrastructureResponse(post("apply-platform", platformBody(name, declarationJson, prune)));
    }

    /**
     * Works out what {@link #applyPlatform} would do, and changes nothing.
     *
     * @param name            the platform's name; {@code "platform"} when {@code null} or empty
     * @param declarationJson the declaration, as JSON
     * @param prune           whether owned objects no longer declared would be removed
     * @return what would be created, adopted, updated, removed and kept
     * @throws IOException if an I/O error occurs during the operation, or the declaration is not JSON
     * @throws InterruptedException if the operation is interrupted
     */
    public ApplyInfrastructureResponse planPlatform(String name, String declarationJson, boolean prune)
            throws IOException, InterruptedException {
        return toApplyInfrastructureResponse(post("plan-platform", platformBody(name, declarationJson, prune)));
    }

    /**
     * Reads a platform declaration: what it owns, and the declaration last applied.
     *
     * @param name the platform's name; {@code "platform"} when {@code null} or empty
     * @return the platform
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws de.jensvogt.euclid.exception.EuclidServiceException with status 404 if no platform of
     * that name was applied in the session's namespace
     */
    public Platform getPlatform(String name) throws IOException, InterruptedException {
        return toPlatform(post("get-platform", OBJECT_MAPPER.writeValueAsString(Map.of("name", platformName(name)))));
    }

    /**
     * Lists the platform declarations applied in the session's namespace.
     *
     * @return the platforms
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public List<Platform> listPlatforms() throws IOException, InterruptedException {
        List<Platform> platforms = new ArrayList<>();
        JsonNode node = post("list-platforms", "{}").get("platforms");
        if (node != null && node.isArray()) {
            for (JsonNode entry : node) {
                platforms.add(toPlatform(entry));
            }
        }
        return platforms;
    }

    /**
     * Overrides the level an application logs at, without redeploying or restarting it.
     * <p>
     * An unrecognised level is refused with HTTP 400 rather than defaulted: "warnign" quietly
     * meaning "info" is an application logging more than somebody asked for, and quietly meaning
     * "off" is silence nobody asked for at all.
     *
     * @param applicationId the ID of the application
     * @param level         one of {@code "trace"}, {@code "debug"}, {@code "info"},
     *                      {@code "warning"}, {@code "error"}, {@code "fatal"} or {@code "off"}
     * @return the level the application now logs at and the channel it was applied to
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public SetLogLevelResponse setLogLevel(String applicationId, String level)
            throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(
                SetLogLevelRequest.builder().applicationId(applicationId).level(level).build());
        return toSetLogLevelResponse(post("set-log-level", body));
    }

    /**
     * Lists the worker nodes registered to this account, and whether each is live.
     * <p>
     * A node is a machine euclid may place application instances on. It registers itself and then
     * renews a lease, and {@link Node#live()} is derived from whether that lease is still current -
     * so a machine that was switched off reads as not live without anything having to notice.
     * <p>
     * {@link Node#applications()} is {@code null} on every entry here: what a node is running is
     * the one thing not on the node record, so a listing does not pay for it. Use
     * {@link #getNode(String)} for that.
     *
     * @return the registered nodes
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ListNodesResponse listNodes() throws IOException, InterruptedException {
        JsonNode root = post("list-nodes", "{}");
        List<Node> nodes = toNodeList(root.get("nodes"));
        return new ListNodesResponse(nodes, root.path("total").asLong(nodes.size()));
    }

    /**
     * Reads one worker node's registration, together with what it is running.
     * <p>
     * This is the only action that reports {@link Node#applications()} - the slots the node holds
     * per application and how many of them are actually serving. Both counts, because a node
     * holding four slots and running none is exactly the state worth seeing.
     *
     * @param node the node's name
     * @return the node
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws de.jensvogt.euclid.exception.EuclidServiceException with status 404 if no node of
     * that name is registered to this account
     */
    public Node getNode(String node) throws IOException, InterruptedException {
        return toNode(post("get-node", OBJECT_MAPPER.writeValueAsString(Map.of("node", node))));
    }

    /**
     * Removes a worker node's registration, which frees its name for another principal. Requires
     * administrator privileges - unlike the other node actions, and for exactly that reason: a
     * worker able to delete another's registration could re-register under its name and be handed
     * its assignments and credentials.
     * <p>
     * This does not stop a node. Leases are left alone, so a worker that is still running finds
     * itself unregistered on its next renewal and simply registers again - draining the node and
     * then stopping the worker is how one is taken out of service.
     *
     * @param node the node's name
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws de.jensvogt.euclid.exception.EuclidServiceException with status 403 for a
     * non-administrator, or 404 if no node of that name is registered
     */
    public void deleteNode(String node) throws IOException, InterruptedException {
        post("delete-node", OBJECT_MAPPER.writeValueAsString(Map.of("node", node)));
    }

    /**
     * Converts a JsonNode holding an array of nodes into a list of them.
     *
     * @param nodesNode the JsonNode representing the array of nodes
     * @return the parsed nodes, or an empty list if the node is null or not an array
     */
    private static List<Node> toNodeList(JsonNode nodesNode) {
        List<Node> nodes = new ArrayList<>();
        if (nodesNode != null && nodesNode.isArray()) {
            for (JsonNode entry : nodesNode) {
                nodes.add(toNode(entry));
            }
        }
        return nodes;
    }

    /**
     * Tells euclid how loaded this instance is, which is what the autoscaler grows and shrinks the
     * pool on.
     *
     * <p>Called by an application about itself, on its own timer. The instance id is the one euclid
     * handed the process in {@code EUCLID_INSTANCE_ID}; a report that cannot say which pool slot it
     * came from is refused rather than attributed to the wrong one.
     *
     * <p>An application deployed without a user of its own runs as {@code app-<runtimeName>}, which
     * already says which pool it is, so nothing else is needed. One deployed with a named user has
     * to pass the {@code applicationId} overload, and must be the identity that application runs
     * as - an application that could report another's load could make somebody else's pool grow to
     * its ceiling or shrink to its floor.
     *
     * <p><strong>This is not a metric.</strong> Push the same numbers to
     * {@code EuclidEmo.pushMetrics} as well if you want the history: that path aggregates into
     * five-minute buckets, which is right for a graph and five minutes too slow for a control loop.
     *
     * @param instanceId  this instance, from {@code EUCLID_INSTANCE_ID}
     * @param utilisation how busy, 0-100; clamped to that range by the server
     * @param backlog     how much work is waiting that this instance has not started
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public void reportLoad(String instanceId, double utilisation, long backlog)
            throws IOException, InterruptedException {
        reportLoad(instanceId, utilisation, backlog, "");
    }

    /**
     * Reports load for an application the caller runs as but is not named after.
     *
     * @param instanceId    this instance, from {@code EUCLID_INSTANCE_ID}
     * @param utilisation   how busy, 0-100
     * @param backlog       how much work is waiting
     * @param applicationId the application being reported for; the caller must be the identity it
     *                      runs as. Empty when the caller is the application's own principal
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public void reportLoad(String instanceId, double utilisation, long backlog, String applicationId)
            throws IOException, InterruptedException {
        reportLoad(instanceId, utilisation, backlog, 0, applicationId);
    }

    /**
     * The same, saying as well how much work this instance has started and not finished.
     *
     * <p>That last number is not a load signal - it says "do not stop me", not "start another one".
     * Scale-down passes over an instance reporting any, because work in flight is work a second
     * instance cannot take over: stopping the instance abandons it and it has to be done again.
     * A listener part-way through a message is the case this exists for.
     *
     * @param instanceId    this instance, from {@code EUCLID_INSTANCE_ID}
     * @param utilisation   how busy, 0-100
     * @param backlog       how much work is waiting
     * @param active        how much work has been started and not finished, now. A gauge rather
     *                      than a counter - the newest figure is the whole answer
     * @param applicationId the application being reported for; the caller must be the identity it
     *                      runs as. Empty when the caller is the application's own principal
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public void reportLoad(String instanceId, double utilisation, long backlog, long active, String applicationId)
            throws IOException, InterruptedException {
        String body = OBJECT_MAPPER.writeValueAsString(Map.of(
                "instanceId", instanceId,
                "utilisation", utilisation,
                "backlog", backlog,
                "active", active,
                "applicationId", applicationId));
        post("report-load", body);
    }

    /**
     * Asks for one request's worth of an outside site for one application: a token from its bucket
     * and a slot among its requests to the site in flight, shared by all of the application's
     * instances.
     *
     * <p>Never waits. If the answer is not {@link EgressPermitResponse#granted() granted}, it says
     * how long to wait before asking again - see {@link EuclidEgressLimiter}, which does that.
     *
     * @param applicationId the application asking - its {@code EUCLID_APPLICATION_ID}; the caller
     *                      must be the identity it runs as. Each application has its own limits
     * @param site         the host name to be called, e.g. {@code downloads.example.com}
     * @param leaseSeconds how long the slot is held if it is not released first - longer than the
     *                     request can take, since a request outliving its lease no longer counts
     * @return the permit, or how long to wait
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public EgressPermitResponse acquireEgressPermit(String applicationId, String site, long leaseSeconds)
            throws IOException, InterruptedException {
        JsonNode node = post("acquire-egress-permit", OBJECT_MAPPER.writeValueAsString(Map.of(
                "applicationId", applicationId, "site", site, "leaseSeconds", leaseSeconds)));
        return new EgressPermitResponse(textOrNull(node, "site"), node.path("granted").asBoolean(false),
                textOrNull(node, "permitId"), textOrNull(node, "expiresAt"), textOrNull(node, "reason"),
                node.path("retryAfterMs").asLong(0));
    }

    /**
     * Gives back a permit's slot, once the request it was for has finished.
     *
     * @param applicationId the application the permit was granted to
     * @param site     the site the permit is for
     * @param permitId the permit, from {@link #acquireEgressPermit(String, String, long)}
     * @return true if it was still held; false if its lease had already run out, which is not an
     *         error - the slot is free either way
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public boolean releaseEgressPermit(String applicationId, String site, String permitId)
            throws IOException, InterruptedException {
        JsonNode node = post("release-egress-permit", OBJECT_MAPPER.writeValueAsString(Map.of(
                "applicationId", applicationId, "site", site, "permitId", permitId)));
        return node.path("released").asBoolean(false);
    }

    /**
     * Reports that a site refused a request - a 429 or 503 - so that no instance of the application
     * calls it again until {@code retryAfterSeconds} have passed.
     *
     * @param applicationId     the application the site refused
     * @param site              the site that refused
     * @param retryAfterSeconds how long it asked to be left alone (its {@code Retry-After}); euclid
     *                          caps one report at an hour
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public void reportEgressThrottled(String applicationId, String site, long retryAfterSeconds)
            throws IOException, InterruptedException {
        post("report-egress-throttled", OBJECT_MAPPER.writeValueAsString(Map.of(
                "applicationId", applicationId, "site", site, "retryAfterSeconds", retryAfterSeconds)));
    }

    /**
     * Lists every application's outside sites in the session's namespace: what each is allowed, and
     * how it stands now.
     *
     * @return the default limits and every site
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ListEgressSitesResponse listEgressSites() throws IOException, InterruptedException {
        return listEgressSites(null);
    }

    /**
     * Lists one application's outside sites: what each is allowed, and how it stands now - tokens
     * left, requests in flight, and whether the site asked to be left alone.
     *
     * @param applicationId the application, or {@code null} for every application in the namespace
     * @return the default limits and the application's sites
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ListEgressSitesResponse listEgressSites(String applicationId) throws IOException, InterruptedException {
        String body = applicationId == null || applicationId.isEmpty() ? "{}" : applicationBody(applicationId);
        JsonNode node = post("list-egress-sites", body);
        List<EgressSite> sites = new ArrayList<>();
        JsonNode sitesNode = node.get("sites");
        if (sitesNode != null && sitesNode.isArray()) {
            for (JsonNode entry : sitesNode) {
                sites.add(new EgressSite(textOrNull(entry, "applicationId"), textOrNull(entry, "site"),
                        toEgressLimits(entry.get("limits")), entry.path("limitsSet").asBoolean(false),
                        entry.path("tokens").asDouble(0), entry.path("inFlight").asLong(0),
                        emptyToNull(textOrNull(entry, "blockedUntil")), emptyToNull(textOrNull(entry, "lastUsed"))));
            }
        }
        return new ListEgressSitesResponse(toEgressLimits(node.get("defaultLimits")), sites);
    }

    /**
     * Sets how fast one application may call one outside site, shared by all of its instances.
     * <p>
     * With site {@code "*"} it sets the application's default, for every site of it that has no
     * limit of its own; without either, {@code euclid.modules.eap.egress.default} applies. A limit
     * left {@code null} keeps the value the site is held to now. Stored, so it applies to every EAP
     * instance at once and survives a restart.
     *
     * @param applicationId     the application
     * @param site              the host name, or {@code "*"} for the application's default
     * @param requestsPerSecond how fast the site's bucket refills, or {@code null} to leave it
     * @param burst             how many tokens the bucket holds, at least 1, or {@code null} to leave it
     * @param concurrent        how many requests may be in flight at once, 0 for no limit, or
     *                          {@code null} to leave it
     * @return the limits the site is held to from now on
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws de.jensvogt.euclid.exception.EuclidServiceException with status 400 if none of the
     * three limits is given
     */
    public EgressLimitResponse setEgressLimit(String applicationId, String site, Double requestsPerSecond,
                                              Double burst, Long concurrent) throws IOException, InterruptedException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("applicationId", applicationId);
        body.put("site", site);
        if (requestsPerSecond != null) {
            body.put("requestsPerSecond", requestsPerSecond);
        }
        if (burst != null) {
            body.put("burst", burst);
        }
        if (concurrent != null) {
            body.put("concurrent", concurrent);
        }
        return toEgressLimitResponse(post("set-egress-limit", OBJECT_MAPPER.writeValueAsString(body)));
    }

    /**
     * Takes back a limit set with {@link #setEgressLimit}, so the site is held to the application's
     * {@code "*"} default again, or to {@code euclid.modules.eap.egress.default} if it has none.
     * <p>
     * The site's state - tokens, slots in flight, a block - stays: forgetting the slots in flight
     * would let the site be overrun the moment this returns.
     *
     * @param applicationId the application
     * @param site          the host name, or {@code "*"} for the application's default
     * @return the limits the site is held to from now on
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws de.jensvogt.euclid.exception.EuclidServiceException with status 404 if the site has
     * no limit of its own
     */
    public EgressLimitResponse deleteEgressLimit(String applicationId, String site) throws IOException, InterruptedException {
        return toEgressLimitResponse(post("delete-egress-limit", OBJECT_MAPPER.writeValueAsString(
                Map.of("applicationId", applicationId, "site", site))));
    }

    /**
     * Forgets one site of one application: its limit, its bucket, its slots, when it was last
     * called. Where {@link #deleteEgressLimit} takes back a decision and leaves the site listed,
     * this leaves nothing.
     * <p>
     * Not a ban. A site is listed because the application asked for a permit to call it, so one it
     * still calls is back on its next request. This is for a site that is finished with - a host
     * that was decommissioned, a limit set on a name that turned out to be a typo.
     *
     * @param applicationId the application
     * @param site          the host name, or {@code "*"} for the application's default
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     * @throws de.jensvogt.euclid.exception.EuclidServiceException with status 404 if the
     * application has no state for the site
     */
    public void deleteEgressSite(String applicationId, String site) throws IOException, InterruptedException {
        post("delete-egress-site", OBJECT_MAPPER.writeValueAsString(Map.of("applicationId", applicationId, "site", site)));
    }

    /**
     * Takes back a level set with {@link #setLogLevel(String, String)}, putting the application back
     * under whatever its channel is configured with.
     * <p>
     * Sends the same action with an empty level, which is how an override is withdrawn rather than
     * merely changed - there is no level name that means "whatever was configured".
     *
     * @param applicationId the ID of the application
     * @return the response, whose {@code logLevel} is empty now the configured default applies
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public SetLogLevelResponse resetLogLevel(String applicationId) throws IOException, InterruptedException {
        return setLogLevel(applicationId, "");
    }

    /**
     * Builds a {@link SetLogLevelResponse} from the JSON set-log-level answers with.
     *
     * @param node the JSON object describing the applied level
     * @return the parsed response
     */
    private static SetLogLevelResponse toSetLogLevelResponse(JsonNode node) {
        return SetLogLevelResponse.builder().applicationId(textOrNull(node, "applicationId"))
                .logLevel(textOrNull(node, "logLevel")).channel(textOrNull(node, "channel")).build();
    }

    /**
     * Builds a {@link RestartApplicationResponse} from the restart-application answer.
     *
     * @param node the response JSON
     * @return the parsed response
     */
    private static RestartApplicationResponse toRestartApplicationResponse(JsonNode node) {
        return RestartApplicationResponse.builder().applicationId(textOrNull(node, "applicationId"))
                .restarting(node.path("restarting").asBoolean(false))
                .instances(node.path("instances").asLong(0)).build();
    }

    /**
     * Builds an {@link ApplyInfrastructureResponse} from the apply-infrastructure answer.
     *
     * @param node the response JSON
     * @return the parsed response
     */
    private static ApplyInfrastructureResponse toApplyInfrastructureResponse(JsonNode node) {
        List<UpdatedResource> updated = new ArrayList<>();
        for (JsonNode entry : node.path("updated")) {
            updated.add(new UpdatedResource(textOrNull(entry, "ern"), toStringList(entry.get("changes"))));
        }
        List<KeptResource> kept = new ArrayList<>();
        for (JsonNode entry : node.path("kept")) {
            kept.add(new KeptResource(textOrNull(entry, "ern"), textOrNull(entry, "reason")));
        }
        return ApplyInfrastructureResponse.builder().applicationId(textOrNull(node, "applicationId"))
                .platform(textOrNull(node, "platform"))
                .declared(node.path("declared").asBoolean(false))
                .dryRun(node.path("dryRun").asBoolean(false))
                .created(toStringList(node.path("created")))
                .adopted(toStringList(node.path("adopted")))
                .updated(updated)
                .deleted(toStringList(node.path("deleted")))
                .kept(kept)
                .granted(toStringList(node.path("granted")))
                .revoked(toStringList(node.path("revoked"))).build();
    }

    /**
     * Builds the request body for apply-infrastructure and plan-infrastructure.
     *
     * @param applicationId   the ID of the application
     * @param declarationJson a declaration to plan with, or {@code null} for the stored one
     * @param prune           whether owned objects no longer declared are removed
     * @return the serialized request body
     * @throws IOException if the declaration is not JSON
     */
    private static String infrastructureBody(String applicationId, String declarationJson, boolean prune) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("applicationId", applicationId);
        body.put("prune", prune);
        if (declarationJson != null) {
            // Embedded as an object rather than a string: EAP reads the field as the declaration
            // itself.
            body.put("declaration", OBJECT_MAPPER.readTree(declarationJson));
        }
        return OBJECT_MAPPER.writeValueAsString(body);
    }

    /**
     * Builds the request body for apply-platform and plan-platform.
     *
     * @param name            the platform's name
     * @param declarationJson the declaration, as JSON
     * @param prune           whether owned objects no longer declared are removed
     * @return the serialized request body
     * @throws IOException if the declaration is not JSON
     */
    private static String platformBody(String name, String declarationJson, boolean prune) throws IOException {
        Objects.requireNonNull(declarationJson, "a platform declaration is required");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", platformName(name));
        body.put("prune", prune);
        body.put("declaration", OBJECT_MAPPER.readTree(declarationJson));
        return OBJECT_MAPPER.writeValueAsString(body);
    }

    /**
     * The name a platform goes by when none is given, as the CLI and the server both default it.
     *
     * @param name the name given, possibly {@code null} or empty
     * @return the name, or {@code "platform"}
     */
    private static String platformName(String name) {
        return name == null || name.isEmpty() ? "platform" : name;
    }

    /**
     * Builds a {@link Platform} from the JSON get-platform and list-platforms answer with.
     *
     * @param node the JSON object describing the platform
     * @return the parsed platform
     */
    private static Platform toPlatform(JsonNode node) {
        JsonNode declaration = node.get("declaration");
        return new Platform(textOrNull(node, "name"), textOrNull(node, "namespace"), toStringList(node.get("owns")),
                declaration == null || declaration.isNull() ? null : declaration.toString(),
                textOrNull(node, "modified"));
    }

    /**
     * Builds {@link EgressLimits} from the limits object the egress actions answer with.
     *
     * @param node the JSON object describing the limits
     * @return the parsed limits, all zero if the node is absent
     */
    private static EgressLimits toEgressLimits(JsonNode node) {
        if (node == null || !node.isObject()) {
            return new EgressLimits(0, 0, 0);
        }
        return new EgressLimits(node.path("requestsPerSecond").asDouble(0), node.path("burst").asDouble(0),
                node.path("concurrent").asLong(0));
    }

    /**
     * Builds an {@link EgressLimitResponse} from the set-egress-limit and delete-egress-limit answer.
     *
     * @param node the response JSON
     * @return the parsed response
     */
    private static EgressLimitResponse toEgressLimitResponse(JsonNode node) {
        return new EgressLimitResponse(textOrNull(node, "applicationId"), textOrNull(node, "site"),
                toEgressLimits(node.get("limits")));
    }

    /**
     * Builds the request body for the four actions that name nothing but an application.
     *
     * @param applicationId the ID of the application the action applies to
     * @return the serialized request body
     * @throws IOException if the request cannot be serialized
     */
    private static String applicationBody(String applicationId) throws IOException {
        return OBJECT_MAPPER.writeValueAsString(ApplicationRequest.builder().applicationId(applicationId).build());
    }

    /**
     * Posts one of EAP's actions and parses the response body, since every one of them takes a JSON
     * request and answers with JSON.
     *
     * @param action the EAP action to post
     * @param body the JSON request body
     * @return the parsed response body
     * @throws IOException if an I/O error occurs during the request
     * @throws InterruptedException if the operation is interrupted while waiting for the response
     */
    private JsonNode post(String action, String body) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, TARGET, action,
                requestHeaders(action, body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException(TARGET, action, response.statusCode(), response.body());
        }

        return OBJECT_MAPPER.readTree(response.body());
    }

    /**
     * Builds an {@link Application} from the definition JSON every EAP action but list-applications
     * and delete-application answers with.
     *
     * @param node the JSON object describing the application
     * @return the parsed application
     */
    private static Application toApplication(JsonNode node) {
        return new Application(
                textOrNull(node, "applicationId"),
                textOrNull(node, "runtimeName"),
                textOrNull(node, "ern"),
                textOrNull(node, "accountId"),
                textOrNull(node, "namespace"),
                textOrNull(node, "region"),
                textOrNull(node, "runtime"),
                textOrNull(node, "bucketErn"),
                textOrNull(node, "artifactKey"),
                textOrNull(node, "version"),
                textOrNull(node, "md5Sum"),
                textOrNull(node, "command"),
                toStringList(node.get("arguments")),
                toStringMap(node.get("environment")),
                toStringList(node.get("resources")),
                textOrNull(node, "userId"),
                node.path("userExists").asBoolean(false),
                textOrNull(node, "logLevel"),
                node.path("minInstances").asLong(0),
                node.path("maxInstances").asLong(0),
                node.path("readyTimeoutMs").asLong(0),
                toStringMap(node.get("nodeLabels")),
                ApplicationType.fromWireValue(textOrNull(node, "type")),
                textOrNull(node, "schedule"),
                emptyToNull(textOrNull(node, "nextRunAt")),
                textOrNull(node, "desiredState"),
                textOrNull(node, "state"),
                node.path("instances").asLong(0),
                toEndpointList(node.get("endpoints")),
                textOrNull(node, "created"),
                textOrNull(node, "modified"));
    }

    /**
     * Converts a JsonNode holding an array of endpoints into a list of them.
     *
     * @param endpointsNode the JsonNode representing the array of endpoints
     * @return the parsed endpoints, or an empty list if the node is null or not an array
     */
    /**
     * Maps an empty string to {@code null}, for the fields the server reports as "" rather than
     * omitting - a job with no schedule reports an empty {@code nextRunAt}.
     *
     * @param value the value to map
     * @return the value, or {@code null} if it was null or empty
     */
    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    /**
     * Builds a {@link Node} from the node JSON the node actions answer with.
     *
     * @param node the JSON object describing the node
     * @return the parsed node, or {@code null} if the node is null or not an object
     */
    private static Node toNode(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        // Absent rather than empty for a listing, which does not report what a node is running at
        // all - an empty list there would read as "this node is holding nothing".
        JsonNode applications = node.get("applications");
        return new Node(
                textOrNull(node, "name"),
                textOrNull(node, "address"),
                textOrNull(node, "principal"),
                toStringMap(node.get("labels")),
                node.path("cpuCount").asLong(0),
                textOrNull(node, "version"),
                textOrNull(node, "os"),
                textOrNull(node, "arch"),
                node.path("drained").asBoolean(false),
                node.path("live").asBoolean(false),
                textOrNull(node, "lastSeen"),
                applications == null ? null : toNodeApplicationList(applications));
    }

    /**
     * Converts a JsonNode holding an array of a node's applications into a list of them.
     *
     * @param applicationsNode the JsonNode representing the array
     * @return the parsed entries, or an empty list if the node is not an array
     */
    private static List<NodeApplication> toNodeApplicationList(JsonNode applicationsNode) {
        List<NodeApplication> applications = new ArrayList<>();
        if (applicationsNode.isArray()) {
            for (JsonNode entry : applicationsNode) {
                applications.add(new NodeApplication(
                        textOrNull(entry, "applicationId"),
                        textOrNull(entry, "runtimeName"),
                        textOrNull(entry, "namespace"),
                        textOrNull(entry, "runtime"),
                        entry.path("instances").asLong(0),
                        entry.path("running").asLong(0)));
            }
        }
        return applications;
    }

    private static List<ApplicationEndpoint> toEndpointList(JsonNode endpointsNode) {
        List<ApplicationEndpoint> endpoints = new ArrayList<>();
        if (endpointsNode != null && endpointsNode.isArray()) {
            for (JsonNode endpointNode : endpointsNode) {
                endpoints.add(new ApplicationEndpoint(
                        textOrNull(endpointNode, "instanceId"),
                        endpointNode.path("pid").asInt(-1),
                        endpointNode.path("httpPort").asInt(0)));
            }
        }
        return endpoints;
    }

    /**
     * Converts a JsonNode holding an array of strings into a list.
     *
     * @param node the JsonNode to convert
     * @return the list of strings, or an empty list if the node is null or not an array
     */
    private static List<String> toStringList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode element : node) {
                values.add(element.asText());
            }
        }
        return values;
    }

    /**
     * Converts a JsonNode holding a JSON object of strings into a string-to-string map.
     *
     * @param node the JsonNode to convert
     * @return a map of the node's fields, or an empty map if the node is null or not an object
     */
    private static Map<String, String> toStringMap(JsonNode node) {
        Map<String, String> map = new LinkedHashMap<>();
        if (node != null && node.isObject()) {
            node.fields().forEachRemaining(entry -> map.put(entry.getKey(), entry.getValue().asText()));
        }
        return map;
    }

    /**
     * Generates a map of HTTP request headers for a specified action and request body.
     * The headers include content type, region, account ID, user ID, and
     * authentication information. If AWS credentials are available, the headers
     * are signed with the client's configured {@link SigningScheme} - SigV4 unless
     * {@link #signingScheme(SigningScheme)} says otherwise; without an access key, a Bearer token is
     * used and nothing is signed.
     *
     * @param action the action being performed by the request.
     * @param body the body of the request to be included for signing.
     * @return a map of HTTP headers constructed for the request.
     */
    private Map<String, String> requestHeaders(String action, String body) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        if (region != null) {
            headers.put("x-euclid-region", region);
        }
        if (accountId != null) {
            headers.put("x-euclid-account-id", accountId);
        }
        if (userId != null) {
            headers.put("x-euclid-user-id", userId);
        }
        if (nameSpace != null && !nameSpace.isEmpty()) {
            headers.put("x-euclid-namespace", nameSpace);
        }

        if (accessKeyId != null && !accessKeyId.isEmpty() && secretAccessKey != null && !secretAccessKey.isEmpty()) {
            SignableRequest signable = new SignableRequest("POST", "/");
            headers.forEach(signable::header);
            signable.header("host", hostHeader());
            signable.header("x-euclid-target", TARGET);
            signable.header("x-euclid-action", action);
            signable.body(body);
            signSignatureHeaders(signable, TARGET, headers);
        } else {
            headers.put("Authorization", "Bearer " + token.get());
        }
        return headers;
    }

    /**
     * Signs {@code signable} with the configured scheme and copies the headers it produced onto the
     * outgoing request.
     * <p>
     * Which headers those are is the scheme's business rather than this method's: SigV4 signs into
     * Authorization alongside two {@code x-amz-*} headers, RFC 9421 into Signature and
     * Signature-Input alongside Content-Digest. The scheme is read once into a local so that a
     * {@link #signingScheme(SigningScheme)} call arriving mid-request cannot sign with one scheme
     * and then copy the header names of the other.
     *
     * @param signable the request to sign, with every header the signature covers and the body
     *                 already set on it
     * @param service  the service to scope the signature to
     * @param headers  the outgoing headers, which the signature headers are added to in place
     */
    private void signSignatureHeaders(SignableRequest signable, String service, Map<String, String> headers) {
        signable.scheme(URI.create(baseUrl).getScheme());
        SigningScheme scheme = signingScheme;
        scheme.sign(signable, accessKeyId, secretAccessKey, region, service);
        for (String header : scheme.signatureHeaderNames()) {
            headers.put(header, signable.header(header));
        }
    }

    /**
     * Builds the {@code host} header value the request signature is computed over, including the port
     * when the base URL names one.
     *
     * @return the host header value
     */
    private String hostHeader() {
        URI uri = URI.create(baseUrl);
        int port = uri.getPort();
        return port == -1 ? uri.getHost() : uri.getHost() + ":" + port;
    }

    /**
     * Reads a text field from a JSON node, tolerating both an absent field and an explicit null.
     *
     * @param node the node to read from
     * @param field the field name
     * @return the field's text value, or {@code null} if it is absent or null
     */
    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
