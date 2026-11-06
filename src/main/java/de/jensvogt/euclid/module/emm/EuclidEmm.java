package de.jensvogt.euclid.module.emm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.jensvogt.euclid.auth.CredentialsFileTokens;
import de.jensvogt.euclid.auth.SignableRequest;
import de.jensvogt.euclid.auth.SigningScheme;
import de.jensvogt.euclid.auth.SigningSchemeSelectable;
import de.jensvogt.euclid.auth.TokenRefreshable;
import de.jensvogt.euclid.dto.emm.CreateBackupRequest;
import de.jensvogt.euclid.dto.emm.CreateBackupResponse;
import de.jensvogt.euclid.dto.emm.ExportRequest;
import de.jensvogt.euclid.dto.emm.ImportRequest;
import de.jensvogt.euclid.dto.emm.ImportResponse;
import de.jensvogt.euclid.dto.emm.ListBackupsResponse;
import de.jensvogt.euclid.dto.emm.ListModulesResponse;
import de.jensvogt.euclid.dto.emm.ModuleNameRequest;
import de.jensvogt.euclid.dto.emm.ModuleStoppedResponse;
import de.jensvogt.euclid.dto.emm.RestartModuleResponse;
import de.jensvogt.euclid.dto.emm.SetInstancesRequest;
import de.jensvogt.euclid.dto.emm.SetInstancesResponse;
import de.jensvogt.euclid.dto.emm.SetLogLevelRequest;
import de.jensvogt.euclid.dto.emm.SetLogLevelResponse;
import de.jensvogt.euclid.dto.emm.SetThreadsRequest;
import de.jensvogt.euclid.dto.emm.SetThreadsResponse;
import de.jensvogt.euclid.dto.emm.model.BackupFile;
import de.jensvogt.euclid.dto.emm.model.ImportedCollection;
import de.jensvogt.euclid.dto.emm.model.ManagedModule;
import de.jensvogt.euclid.dto.emm.model.ModuleInstance;
import de.jensvogt.euclid.dto.emm.model.SkippedCollection;
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
 * EMM (module manager) operations for an authenticated
 * {@link de.jensvogt.euclid.module.eam.EuclidSession}: the process pools of every euclid module,
 * their limits and desired state, and the backup, export and import of the installation's data.
 * <p>
 * Every EMM action is administrator-only. It exposes and changes every module's live process pool
 * and reads and writes raw database collections, so no role can grant it - EMM is one of the
 * modules the role gate refuses to bind, and it checks membership of the administrator group
 * itself on every request. A session whose {@code isAdmin()} is false gets HTTP 403
 * ({@code "administrator privileges required"}) from every method here.
 * <p>
 * The control actions record desired state rather than acting directly: EMM runs in its own process
 * and the manager picks up what was recorded on its next reconcile, within a few seconds. So a
 * response describes the pool as it stood when the request was answered, not after the change has
 * been carried out.
 */
public final class EuclidEmm implements TokenRefreshable, SigningSchemeSelectable {

    /**
     * A singleton instance of {@code ObjectMapper} from the Jackson library used for
     * serializing Java objects to JSON and deserializing JSON to Java objects.
     * <p>
     * Configured to leave null fields out of the serialized request entirely rather than writing
     * them as {@code null}. EMM reads an absent field as "leave it as it is" - a set-instances
     * request without a floor keeps the floor in force, a create-backup without {@code full} lets
     * the configuration decide - and a JSON null is not an absent field.
     */
    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    /**
     * The Euclid service every request from this class is addressed to, sent as the
     * {@code x-euclid-target} header.
     */
    private static final String TARGET = "emm";

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
     * The namespace requests are scoped to, sent as the {@code x-euclid-namespace} header. EMM works
     * on the whole installation rather than on a namespace, but the header is sent as on every other
     * module so the request is authenticated against the same scope.
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
     * Constructs an EMM client. Normally obtained from
     * {@link de.jensvogt.euclid.module.eam.EuclidSession#emm()} rather than built directly.
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
    public EuclidEmm(String baseUrl, String token, String region, String accountId, String userId,
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

    // ── Modules ──────────────────────────────────────────────────────────────

    /**
     * Lists every module known to the manager, as persisted in its module repository, with each
     * one's configuration, desired state and current process pool.
     * <p>
     * Reports what the control actions will and will not accept - {@link ManagedModule#core()} and
     * {@link ManagedModule#desiredStopped()} - so a caller can find out before asking rather than by
     * being refused.
     *
     * @return a {@code ListModulesResponse} carrying the modules and their count
     * @throws EuclidServiceException with HTTP 401 if the session is not authenticated, or 403 if
     *                                the caller is not an administrator
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ListModulesResponse listModules() throws IOException, InterruptedException {
        JsonNode root = post("list-modules", "{}");
        List<ManagedModule> modules = new ArrayList<>();
        JsonNode modulesNode = root.get("modules");
        if (modulesNode != null && modulesNode.isArray()) {
            for (JsonNode moduleNode : modulesNode) {
                modules.add(toModule(moduleNode));
            }
        }
        return new ListModulesResponse(modules, root.path("total").asLong(modules.size()));
    }

    /**
     * Sets a module's floor and ceiling for the autoscaler.
     *
     * @param name         the module, e.g. {@code "esm"}
     * @param minInstances the floor kept running at all times, 0 or more
     * @param maxInstances the ceiling the autoscaler may scale up to, 1 or more
     * @return the limits now in force and the pool as it stands
     * @throws EuclidServiceException with the statuses {@link #setInstances(SetInstancesRequest)} lists
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public SetInstancesResponse setInstances(String name, int minInstances, int maxInstances)
            throws IOException, InterruptedException {
        return setInstances(SetInstancesRequest.builder().name(name).minInstances(minInstances)
                .maxInstances(maxInstances).build());
    }

    /**
     * Sets the instance limits the autoscaler works within for one module. Either limit may be
     * left {@code null} to keep the one in force.
     * <p>
     * Recorded rather than applied: the manager picks the limits up on its next reconcile, within a
     * few seconds. Raising the floor starts instances up to it immediately; lowering the ceiling
     * lets the pool shrink through the ordinary scale-down rather than stopping an instance in the
     * middle of a request. The limits are persisted and applied over what {@code euclid.json}
     * configures, so they survive a manager restart and stay in force until they are set again.
     * The pair is checked against whatever the other limit will end up being, so it can never be
     * left crossed.
     *
     * @param request the module and the limits to set
     * @return the limits now in force and the pool as it stands
     * @throws EuclidServiceException with HTTP 400 if the name is missing, neither limit is given, a
     *                                limit is out of range or the floor would exceed the ceiling;
     *                                401 if the session is not authenticated; 403 if the caller is
     *                                not an administrator; 404 if no such module is known
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public SetInstancesResponse setInstances(SetInstancesRequest request) throws IOException, InterruptedException {
        JsonNode root = post("set-instances", OBJECT_MAPPER.writeValueAsString(request));
        return new SetInstancesResponse(textOrNull(root, "name"), root.path("minInstances").asLong(),
                root.path("maxInstances").asLong(), root.path("runningInstances").asLong());
    }

    /**
     * Sets how many worker threads each of a module's processes serves requests with.
     * <p>
     * The companion to {@link #setInstances}: instances are how many processes of a module run,
     * threads how many requests one of them can be in the middle of. It matters most for the modules
     * whose busiest action deliberately waits - EES's receive-events, EQS's receive-messages - where
     * too few threads queue requests that are not waiting behind the ones that are. A thread count
     * is fixed when a process starts, so the manager applies it by restarting the module's instances
     * one per reconcile tick; a module running a single instance is briefly unavailable while that
     * one restarts. The setting is persisted and takes precedence over
     * {@code euclid.modules.<module>.threads}.
     *
     * @param name    the module, e.g. {@code "eqs"}
     * @param threads the number of worker threads, 1 to 256
     * @return the count recorded and how many instances still have to be cycled
     * @throws EuclidServiceException with HTTP 400 if the name or thread count is missing or the
     *                                count is outside 1 to 256 - refused rather than silently
     *                                clamped; 401 if the session is not authenticated; 403 if the
     *                                caller is not an administrator; 404 if no such module is known
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public SetThreadsResponse setThreads(String name, int threads) throws IOException, InterruptedException {
        JsonNode root = post("set-threads", OBJECT_MAPPER.writeValueAsString(new SetThreadsRequest(name, threads)));
        return new SetThreadsResponse(textOrNull(root, "name"), root.path("threads").asLong(),
                root.path("runningInstances").asLong());
    }

    /**
     * Sets the level the manager logs a module's own output at, on the module's channel
     * {@code "module.<name>"}. Takes effect on the next reconcile, without restarting the module or
     * interrupting a request.
     * <p>
     * It decides what the manager passes on rather than what the module produces: a line the module
     * wrote to standard output arrives as information and one written to standard error as an
     * error, so {@code "error"} keeps what went wrong and {@code "off"} silences the module in the
     * manager's log entirely. The level is stored with the module and survives a restart. A module
     * name in the wrong case is accepted as its lower-case spelling.
     *
     * @param name  the module, e.g. {@code "esm"}
     * @param level {@code "trace"}, {@code "debug"}, {@code "info"}, {@code "warning"},
     *              {@code "error"}, {@code "fatal"} or {@code "off"}; {@code null}, empty or
     *              {@code "default"} takes the setting back, leaving the module under
     *              {@code euclid.logging.channels} - mirroring euclid-cli
     * @return the module, the canonical level recorded and the channel it applies to
     * @throws EuclidServiceException with HTTP 400 if the name is missing or the level is not one of
     *                                the above; 401 if the session is not authenticated; 403 if the
     *                                caller is not an administrator; 404 if no such module is known
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public SetLogLevelResponse setLogLevel(String name, String level) throws IOException, InterruptedException {
        String effective = level == null || "default".equals(level) ? "" : level;
        JsonNode root = post("set-log-level", OBJECT_MAPPER.writeValueAsString(new SetLogLevelRequest(name, effective)));
        return new SetLogLevelResponse(textOrNull(root, "name"), textOrNull(root, "logLevel"),
                textOrNull(root, "channel"));
    }

    /**
     * Stops a module and keeps it stopped.
     * <p>
     * This records desired state rather than issuing a one-off stop: an instance that is merely
     * stopped is one the manager brings straight back, whereas a module stopped here stays down
     * through crashes, scale events and manager restarts until {@link #startModule} brings it back.
     * Requests routed to it fail while it is down, and modules declaring it as a dependency will not
     * start. Only euclid's own modules can be stopped this way - applications and transfer servers
     * have their desired state in EAP and ETS - and EMM cannot stop itself. Stopping a module that is
     * already stopped changes nothing and is not an error.
     *
     * @param name the module to stop, e.g. {@code "ftp"}
     * @return the recorded state and the pool as it stood before the manager acted
     * @throws EuclidServiceException with HTTP 400 if the name is missing, names {@code "emm"}, or
     *                                names a module that is not a core euclid module; 401 if the
     *                                session is not authenticated; 403 if the caller is not an
     *                                administrator; 404 if no such module is known
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ModuleStoppedResponse stopModule(String name) throws IOException, InterruptedException {
        return toStoppedResponse(post("stop-module", moduleNameBody(name)));
    }

    /**
     * Lets a module that was stopped with {@link #stopModule} run again. Its instances are started
     * on the manager's next reconcile, back up to the module's minimum, after which the autoscaler
     * takes over. Starting a module that was never stopped changes nothing and is not an error.
     *
     * @param name the module to start again
     * @return the recorded state and the pool as it stood before the manager acted
     * @throws EuclidServiceException with HTTP 400 if the name is missing or names a module that is
     *                                not a core euclid module; 401 if the session is not
     *                                authenticated; 403 if the caller is not an administrator; 404 if
     *                                no such module is known
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ModuleStoppedResponse startModule(String name) throws IOException, InterruptedException {
        return toStoppedResponse(post("start-module", moduleNameBody(name)));
    }

    /**
     * Restarts every instance of a module, one per reconcile tick, so the rest of the pool keeps
     * serving while the manager works through them.
     * <p>
     * Nothing about the module changes: this is for picking up a new binary after an upgrade,
     * clearing state a process has accumulated, or making a module read its configuration again.
     * Unlike {@link #stopModule} it records no desired state, so it works for applications and
     * transfer servers too. Asking twice restarts twice.
     *
     * @param name the module to restart, e.g. {@code "esm"}
     * @return the module and how many instances will be cycled
     * @throws EuclidServiceException with HTTP 400 if the name is missing or the module is stopped -
     *                                use {@link #startModule} for that; 401 if the session is not
     *                                authenticated; 403 if the caller is not an administrator; 404 if
     *                                no such module is known
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public RestartModuleResponse restartModule(String name) throws IOException, InterruptedException {
        JsonNode root = post("restart-module", moduleNameBody(name));
        return new RestartModuleResponse(textOrNull(root, "name"), root.path("runningInstances").asLong());
    }

    // ── Backups ──────────────────────────────────────────────────────────────

    /**
     * Lists the backups EMM has written to its backup directory, newest first, together with how
     * the nightly backup is set up.
     * <p>
     * An entry whose {@link BackupFile#complete()} is false is an export that was never zipped: a
     * backup running right now, or one that died part-way. The directory is the one on the host EMM
     * runs on.
     *
     * @return the backup configuration and the backup files found
     * @throws EuclidServiceException with HTTP 401 if the session is not authenticated, 403 if the
     *                                caller is not an administrator, or 500 if the backup directory
     *                                cannot be read
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ListBackupsResponse listBackups() throws IOException, InterruptedException {
        JsonNode root = post("list-backups", "{}");
        List<BackupFile> backups = new ArrayList<>();
        JsonNode backupsNode = root.get("backups");
        if (backupsNode != null && backupsNode.isArray()) {
            for (JsonNode node : backupsNode) {
                backups.add(new BackupFile(textOrNull(node, "name"), textOrNull(node, "path"),
                        textOrNull(node, "accountId"), textOrNull(node, "namespace"),
                        textOrNull(node, "startedAt"), textOrNull(node, "modified"),
                        node.path("size").asLong(), node.path("complete").asBoolean()));
            }
        }
        return new ListBackupsResponse(textOrNull(root, "directory"), root.path("enabled").asBoolean(),
                textOrNull(root, "schedule"), root.path("keep").asLong(), root.path("full").asBoolean(),
                root.path("encrypted").asBoolean(), backups);
    }

    /**
     * Starts a backup now, letting {@code euclid.modules.emm.backup.full} decide whether it takes
     * the bulk data.
     *
     * @return the name of the archive the backup will write, and how
     * @throws EuclidServiceException with the statuses {@link #createBackup(Boolean)} lists
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public CreateBackupResponse createBackup() throws IOException, InterruptedException {
        return createBackup(null);
    }

    /**
     * Starts a backup now instead of at the next scheduled time - written to the configured backup
     * directory, named, encrypted and pruned exactly as the nightly one is.
     * <p>
     * Answered as soon as the backup has started (HTTP 202), not when it is done: a full backup reads
     * every message and object record there is, which takes longer than any client should hold a
     * request open. {@link #listBackups()} shows the archive as incomplete while it is written and
     * complete once it is. {@code full} is ignored when a backup passphrase is configured: an
     * encrypted backup is built in memory, and the bulk data is what does not fit.
     *
     * @param full {@code true} to take the bulk data for this backup, {@code false} for the
     *             configuration only, or {@code null} to let the configuration decide
     * @return the name of the archive the backup will write, and how
     * @throws EuclidServiceException with HTTP 401 if the session is not authenticated; 403 if the
     *                                caller is not an administrator; 409 if a backup is already
     *                                being written; 500 if the backup cannot be started
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public CreateBackupResponse createBackup(Boolean full) throws IOException, InterruptedException {
        JsonNode root = post("create-backup", OBJECT_MAPPER.writeValueAsString(new CreateBackupRequest(full)));
        return new CreateBackupResponse(textOrNull(root, "name"), textOrNull(root, "directory"),
                textOrNull(root, "startedAt"), root.path("full").asBoolean(), root.path("encrypted").asBoolean());
    }

    // ── Export and import ────────────────────────────────────────────────────

    /**
     * Exports the top-level resource collections of the named modules, unencrypted.
     *
     * @param modules the modules to export, e.g. {@code "esm", "eqs"}
     * @return the archive, as JSON text {@link #importModules(String)} accepts
     * @throws EuclidServiceException with the statuses {@link #exportModules(ExportRequest)} lists
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public String exportModules(String... modules) throws IOException, InterruptedException {
        return exportModules(ExportRequest.builder().modules(modules).build());
    }

    /**
     * Exports modules' MongoDB collections and returns the archive - the library equivalent of
     * {@code euclid-cli emm export}, which writes this same document to a local file. Writing it
     * anywhere is the caller's choice; nothing here touches the filesystem.
     * <p>
     * An unencrypted archive is the server's answer exactly as it came back - one request, its body
     * returned untouched. A sealed one is assembled as euclid-cli assembles it, so the two produce
     * interchangeable files: when modules are named, each is exported in a request of its own and
     * every request after the first carries back the salt the first was answered with, so that every
     * frame opens with the same key; the result is the first response's envelope without its payload,
     * naming every module the server exported, with each response's payload in {@code "frames"}.
     * <p>
     * The archive holds everything the collections hold - access-key secrets among it, and with
     * {@code ekm} the key material - so treat an unencrypted one like the database it came from.
     * The passphrase travels to the server to be used and is not kept there; there is no recovery
     * for an archive whose passphrase is lost.
     *
     * @param request which modules to export, how much of them, and the optional passphrase
     * @return the archive, as JSON text {@link #importModules(ImportRequest)} accepts
     * @throws IllegalArgumentException if not exactly one of {@code all} and {@code modules} is given
     * @throws EuclidServiceException with HTTP 400 if a module is unknown, {@code ekm} is asked for
     *                                without a passphrase, or no key can be derived; 401 if the
     *                                session is not authenticated; 403 if the caller is not an
     *                                administrator
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public String exportModules(ExportRequest request) throws IOException, InterruptedException {
        List<String> modules = request.modules() == null ? List.of() : request.modules();
        if (request.all() == !modules.isEmpty()) {
            throw new IllegalArgumentException("exactly one of all or modules is required");
        }
        String passphrase = request.passphrase() == null ? "" : request.passphrase();

        if (passphrase.isEmpty()) {
            ObjectNode body = exportBody(request.all(), modules, request.full());
            return postForBody("export", OBJECT_MAPPER.writeValueAsString(body));
        }

        // One request per named module, or a single one for "all" - mirroring euclid-cli, so a
        // sealed archive made here and one made there are the same file.
        List<List<String>> requests = new ArrayList<>();
        if (request.all()) {
            requests.add(List.of());
        } else {
            for (String module : modules) {
                requests.add(List.of(module));
            }
        }

        ArrayNode frames = OBJECT_MAPPER.createArrayNode();
        ArrayNode exported = OBJECT_MAPPER.createArrayNode();
        ObjectNode envelope = null;
        String salt = null;
        for (List<String> requestModules : requests) {
            ObjectNode body = exportBody(request.all(), requestModules, request.full());
            body.put("passphrase", passphrase);
            if (salt != null) {
                body.put("salt", salt);
            }
            JsonNode response = post("export", OBJECT_MAPPER.writeValueAsString(body));
            frames.add(response.path("data"));
            // Which modules the archive holds is the server's answer, not the request's: with "all"
            // the caller named none, and an archive that cannot say what is in it would have to be
            // opened to find out.
            for (JsonNode name : response.path("modules")) {
                exported.add(name);
            }
            if (salt == null) {
                salt = response.path("salt").asText();
                envelope = response.deepCopy();
            }
        }

        envelope.remove("data");
        envelope.set("modules", exported);
        envelope.set("frames", frames);
        return OBJECT_MAPPER.writeValueAsString(envelope);
    }

    /**
     * Imports an unencrypted archive in full.
     *
     * @param document the archive's JSON text, as {@link #exportModules} returned it
     * @return what was imported and what was skipped
     * @throws IllegalArgumentException for the reasons {@link #importModules(ImportRequest)} lists
     * @throws EuclidServiceException with the statuses {@link #importModules(ImportRequest)} lists
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ImportResponse importModules(String document) throws IOException, InterruptedException {
        return importModules(ImportRequest.builder().document(document).build());
    }

    /**
     * Imports an archive back into MongoDB - the library equivalent of {@code euclid-cli emm import},
     * taking the document rather than reading it from a file.
     * <p>
     * Each document is upserted by its {@code _id}, replacing the whole document rather than merging
     * fields, so the result matches the export exactly. Only collections EMM knows to belong to a
     * module are written; anything else is reported in {@link ImportResponse#skipped()}. A sealed
     * archive is authenticated as it is opened, so a wrong passphrase and an altered file fail the
     * same way - and nothing is written when they do.
     * <p>
     * The archive is checked here before it is sent, as euclid-cli checks it: a passphrase is
     * required for a sealed archive and refused for a plain one, since a passphrase that has no use
     * is a sign the wrong file was picked up.
     *
     * @param request the archive, the optional subset of its modules, and the passphrase if sealed
     * @return what was imported and what was skipped
     * @throws IllegalArgumentException if the document is not a JSON object with
     *                                  {@code "collections"} or {@code "frames"}, if it is sealed and
     *                                  no passphrase is given, or if it is plain and one is
     * @throws EuclidServiceException with HTTP 400 if the archive is malformed or cannot be opened
     *                                with the passphrase; 401 if the session is not authenticated;
     *                                403 if the caller is not an administrator
     * @throws IOException if an I/O error occurs during the operation
     * @throws InterruptedException if the operation is interrupted
     */
    public ImportResponse importModules(ImportRequest request) throws IOException, InterruptedException {
        JsonNode parsed;
        try {
            parsed = request.document() == null ? null : OBJECT_MAPPER.readTree(request.document());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("the document is not valid JSON: " + e.getOriginalMessage(), e);
        }
        if (parsed == null || !parsed.isObject()) {
            throw new IllegalArgumentException("the document is not a JSON object - not an emm export?");
        }
        ObjectNode document = (ObjectNode) parsed;
        boolean sealed = document.has("frames");
        if (!sealed && !document.has("collections")) {
            throw new IllegalArgumentException(
                    "the document has neither a \"collections\" object nor \"frames\" - not an emm export?");
        }

        String passphrase = request.passphrase() == null ? "" : request.passphrase();
        if (sealed) {
            if (passphrase.isEmpty()) {
                throw new IllegalArgumentException("the document is encrypted - a passphrase is required to open it");
            }
            document.put("passphrase", passphrase);
        } else if (!passphrase.isEmpty()) {
            throw new IllegalArgumentException("the document is not encrypted - no passphrase is needed");
        }

        if (request.modules() != null && !request.modules().isEmpty()) {
            ArrayNode modules = document.putArray("modules");
            request.modules().forEach(modules::add);
        }

        JsonNode root = post("import", OBJECT_MAPPER.writeValueAsString(document));

        Map<String, ImportedCollection> imported = new LinkedHashMap<>();
        JsonNode importedNode = root.get("imported");
        if (importedNode != null && importedNode.isObject()) {
            importedNode.fields().forEachRemaining(entry -> imported.put(entry.getKey(),
                    new ImportedCollection(entry.getValue().path("imported").asLong(),
                            entry.getValue().path("failed").asLong(0))));
        }
        List<SkippedCollection> skipped = new ArrayList<>();
        JsonNode skippedNode = root.get("skipped");
        if (skippedNode != null && skippedNode.isArray()) {
            for (JsonNode node : skippedNode) {
                skipped.add(new SkippedCollection(textOrNull(node, "collection"), textOrNull(node, "reason")));
            }
        }
        return new ImportResponse(imported, skipped);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Builds the part of an export request every request of one export shares.
     *
     * @param all     whether every module is exported
     * @param modules the modules this request exports, ignored when {@code all} is set
     * @param full    whether the bulk child data is included
     * @return the request body, to which a passphrase and salt may still be added
     */
    private static ObjectNode exportBody(boolean all, List<String> modules, boolean full) {
        ObjectNode body = OBJECT_MAPPER.createObjectNode();
        body.put("full", full);
        if (all) {
            body.put("all", true);
        } else {
            ArrayNode array = body.putArray("modules");
            modules.forEach(array::add);
        }
        return body;
    }

    /**
     * Builds the request body for the three actions that name nothing but a module.
     *
     * @param name the module the action applies to
     * @return the serialized request body
     * @throws IOException if the request cannot be serialized
     */
    private static String moduleNameBody(String name) throws IOException {
        return OBJECT_MAPPER.writeValueAsString(new ModuleNameRequest(name));
    }

    /**
     * Builds the answer stop-module and start-module share.
     *
     * @param root the parsed response body
     * @return the parsed response
     */
    private static ModuleStoppedResponse toStoppedResponse(JsonNode root) {
        return new ModuleStoppedResponse(textOrNull(root, "name"), root.path("stopped").asBoolean(),
                root.path("runningInstances").asLong());
    }

    /**
     * Builds a {@link ManagedModule} from one entry of a list-modules response.
     *
     * @param node the JSON object describing the module
     * @return the parsed module
     */
    private static ManagedModule toModule(JsonNode node) {
        List<ModuleInstance> instances = new ArrayList<>();
        JsonNode instancesNode = node.get("instances");
        if (instancesNode != null && instancesNode.isArray()) {
            for (JsonNode i : instancesNode) {
                instances.add(new ModuleInstance(
                        textOrNull(i, "instanceId"),
                        i.path("pid").asLong(-1),
                        textOrNull(i, "host"),
                        textOrNull(i, "state"),
                        textOrNull(i, "socketPath"),
                        i.path("httpPort").asLong(),
                        i.path("restartCount").asLong(),
                        i.path("utilisation").asDouble(-1),
                        i.path("backlog").asLong(-1),
                        textOrNull(i, "loadReportedAt"),
                        i.path("backgroundTasks").asLong(),
                        textOrNull(i, "started"),
                        textOrNull(i, "created"),
                        textOrNull(i, "modified")));
            }
        }
        return new ManagedModule(
                textOrNull(node, "name"),
                textOrNull(node, "executable"),
                textOrNull(node, "socketPath"),
                node.path("active").asBoolean(),
                node.path("core").asBoolean(),
                node.path("desiredStopped").asBoolean(),
                node.path("autoRestart").asBoolean(),
                node.path("maxRestarts").asLong(-1),
                node.path("minInstances").asLong(),
                node.path("maxInstances").asLong(),
                node.path("desiredMinInstances").asLong(-1),
                node.path("desiredMaxInstances").asLong(-1),
                node.path("desiredThreads").asLong(-1),
                textOrNull(node, "logLevel"),
                textOrNull(node, "created"),
                textOrNull(node, "modified"),
                textOrNull(node, "lastStartTime"),
                instances);
    }

    /**
     * Posts one of EMM's actions and parses the response body, since every one of them takes a JSON
     * request and answers with JSON.
     *
     * @param action the EMM action to post
     * @param body the JSON request body
     * @return the parsed response body
     * @throws EuclidServiceException if the server answers with a non-2xx status
     * @throws IOException if an I/O error occurs during the request
     * @throws InterruptedException if the operation is interrupted while waiting for the response
     */
    private JsonNode post(String action, String body) throws IOException, InterruptedException {
        return OBJECT_MAPPER.readTree(postForBody(action, body));
    }

    /**
     * Posts one of EMM's actions and returns the response body unparsed - for an unencrypted export,
     * which is handed back exactly as the server wrote it rather than parsed and serialized again.
     *
     * @param action the EMM action to post
     * @param body the JSON request body
     * @return the raw response body
     * @throws EuclidServiceException if the server answers with a non-2xx status
     * @throws IOException if an I/O error occurs during the request
     * @throws InterruptedException if the operation is interrupted while waiting for the response
     */
    private String postForBody(String action, String body) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.post(baseUrl + "/", body, TARGET, action,
                requestHeaders(action, body));

        if (response.statusCode() / 100 != 2) {
            throw new EuclidServiceException(TARGET, action, response.statusCode(), response.body());
        }

        return response.body();
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
     * Reads a string field from a JSON object, mapping both an absent field and a JSON null to
     * {@code null}.
     *
     * @param node the JSON object to read from
     * @param field the field name
     * @return the field's value, or {@code null} if it is absent or null
     */
    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
