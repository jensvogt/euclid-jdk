package de.jensvogt.euclid.dto.eap;

import de.jensvogt.euclid.dto.eap.model.ApplicationType;

import java.util.List;
import java.util.Map;

/**
 * Request to change a deployed application, one field at a time if wanted.
 * <p>
 * Only the fields actually set are sent, and the server only touches the fields it receives, so
 * an update naming a single field leaves the rest of the definition alone. That is why every
 * field but {@code applicationId} is a boxed type: null means "leave alone", not "clear".
 * <p>
 * The four resource lists - {@code buckets}, {@code queues}, {@code topics} and {@code secrets} -
 * are one setting, not four: setting <em>any</em> of them replaces the application's whole
 * resource list with what the four together name, so a request that sets only {@code buckets}
 * takes away the queues, topics and secrets it had. Send all four together when changing one.
 * Doing so also rewrites the access grants on the principal the application runs as - but only
 * when that principal is one EAP minted itself. A user named at creation is the caller's to grant.
 *
 * @param applicationId  the application to change; the only required field
 * @param runtime        how the artifact is executed
 * @param artifact       key of the artifact object, which must exist in the application's bucket
 * @param version        version recorded for the application, taken as given - including one already
 *                       deployed, which is what makes this the way to rebuild a version in place
 * @param command        command to run the artifact with
 * @param arguments      arguments passed to the command, replacing the current list
 * @param environment    environment variables, replacing the current set
 * @param buckets        names of the ESM buckets the application may reach, replacing the current grants
 * @param queues         names of the EQS queues the application may reach, replacing the current grants
 * @param topics         names of the ENS topics the application may reach, replacing the current grants
 * @param secrets        names of the ESS secrets the application may reach, replacing the current grants
 * @param namespace      the namespace to move the application to, {@code ""} for the account root -
 *                       a move rather than a copy, so it stops running where it was
 * @param user           an existing user with an access key for the application to run as instead;
 *                       cannot be emptied
 * @param minInstances   smallest number of processes to keep running
 * @param maxInstances   largest number of processes to run
 * @param readyTimeoutMs milliseconds to wait for a process to report ready
 * @param memoryLimitMb  memory each instance may use, in MiB; 0 removes the limit. Restarts the pool
 * @param cpuLimit       CPU each instance may use, in cores; 0 removes the limit. Restarts the pool
 * @param nodeLabels     key/value labels a worker node has to carry to run this, e.g.
 *                       {@code os=windows}; empty or unset means any node will do
 * @param type           {@code "PROCESS"} (the server's default) for something that stays up, or
 *                       {@code "JOB"} for something that runs once and finishes - a JOB is not
 *                       restarted when it exits 0 and is not autoscaled. Case-sensitive: anything
 *                       else is refused with HTTP 400 rather than read as PROCESS
 * @param schedule       the cron expression a JOB runs on, in UTC, e.g. {@code "0 2 * * *"} or
 *                       {@code "@daily"}. Only valid on a JOB - passing it otherwise is HTTP 400 -
 *                       and an expression that does not parse is refused at the moment it is typed
 *                       rather than silently never firing
 */
public record UpdateApplicationRequest(String applicationId, String runtime, String artifact, String version,
                                       String command, List<String> arguments, Map<String, String> environment,
                                       List<String> buckets, List<String> queues, List<String> topics,
                                       List<String> secrets, String namespace, String user,
                                       Long minInstances, Long maxInstances, Long readyTimeoutMs,
                                       Long memoryLimitMb, Double cpuLimit, Map<String, String> nodeLabels,
                                       String type, String schedule) {

    /**
     * Creates a new instance of the Builder for constructing an UpdateApplicationRequest object.
     *
     * @return a new Builder instance for constructing UpdateApplicationRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link UpdateApplicationRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The application to change; the only required field.
         */
        private String applicationId;

        /**
         * How the artifact is executed.
         */
        private String runtime;

        /**
         * Key of the artifact object, which must exist in the application's bucket.
         */
        private String artifact;

        /**
         * Version recorded for the application, taken as given.
         */
        private String version;

        /**
         * Command to run the artifact with.
         */
        private String command;

        /**
         * Arguments passed to the command, replacing the current list.
         */
        private List<String> arguments;

        /**
         * Environment variables, replacing the current set.
         */
        private Map<String, String> environment;

        /**
         * Names of the ESM buckets the application may reach, replacing the current grants.
         */
        private List<String> buckets;

        /**
         * Names of the EQS queues the application may reach, replacing the current grants.
         */
        private List<String> queues;

        /**
         * Smallest number of processes to keep running.
         */
        private Long minInstances;

        /**
         * Largest number of processes to run.
         */
        private Long maxInstances;

        /**
         * Milliseconds to wait for a process to report ready.
         */
        private Long readyTimeoutMs;

        /**
         * Labels a worker node has to carry to run this; null leaves it to the server.
         */
        private Map<String, String> nodeLabels;

        /**
         * Whether this stays up or runs once and finishes; null leaves it to the server.
         */
        private String type;

        /**
         * The cron expression a job runs on; null leaves it alone.
         */
        private String schedule;

        /**
         * Names of the ENS topics the application may reach.
         */
        private List<String> topics;

        /**
         * Names of the ESS secrets the application may reach.
         */
        private List<String> secrets;

        /**
         * The namespace to move the application to; null leaves it where it is.
         */
        private String namespace;

        /**
         * The identity the application runs as; null leaves it alone.
         */
        private String user;

        /**
         * Memory each instance may use, in MiB; null leaves it alone.
         */
        private Long memoryLimitMb;

        /**
         * CPU each instance may use, in cores; null leaves it alone.
         */
        private Double cpuLimit;

        /**
         * Sets the application to change; the only required field.
         *
         * @param applicationId the application to change; the only required field
         * @return the builder instance
         */
        public Builder applicationId(String applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        /**
         * Sets how the artifact is executed.
         *
         * @param runtime how the artifact is executed
         * @return the builder instance
         */
        public Builder runtime(String runtime) {
            this.runtime = runtime;
            return this;
        }

        /**
         * Sets key of the artifact object, which must exist in the application's bucket.
         *
         * @param artifact key of the artifact object, which must exist in the application's bucket
         * @return the builder instance
         */
        public Builder artifact(String artifact) {
            this.artifact = artifact;
            return this;
        }

        /**
         * Sets the version recorded for the application.
         * <p>
         * Taken as given, and with no check on the artifact either - unlike
         * {@link de.jensvogt.euclid.module.eap.EuclidEap#redeployApplication(RedeployApplicationRequest)},
         * which refuses an artifact whose bytes are the build already deployed unless forced. This
         * is the one place a version and an artifact can be set to anything, which is what makes it
         * the way to roll back to a build already seen.
         *
         * @param version version recorded for the application
         * @return the builder instance
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * Sets command to run the artifact with.
         *
         * @param command command to run the artifact with
         * @return the builder instance
         */
        public Builder command(String command) {
            this.command = command;
            return this;
        }

        /**
         * Sets arguments passed to the command, replacing the current list.
         *
         * @param arguments arguments passed to the command, replacing the current list
         * @return the builder instance
         */
        public Builder arguments(List<String> arguments) {
            this.arguments = arguments;
            return this;
        }

        /**
         * Sets environment variables, replacing the current set.
         *
         * @param environment environment variables, replacing the current set
         * @return the builder instance
         */
        public Builder environment(Map<String, String> environment) {
            this.environment = environment;
            return this;
        }

        /**
         * Sets names of the ESM buckets the application may reach, replacing the current grants.
         *
         * @param buckets names of the ESM buckets the application may reach, replacing the current grants
         * @return the builder instance
         */
        public Builder buckets(List<String> buckets) {
            this.buckets = buckets;
            return this;
        }

        /**
         * Sets names of the EQS queues the application may reach, replacing the current grants.
         *
         * @param queues names of the EQS queues the application may reach, replacing the current grants
         * @return the builder instance
         */
        public Builder queues(List<String> queues) {
            this.queues = queues;
            return this;
        }

        /**
         * Sets names of the ENS topics the application may reach, replacing the current grants.
         * <p>
         * Like the other three resource lists, setting this replaces the application's whole
         * resource list - see the class description.
         *
         * @param topics names of the ENS topics the application may reach
         * @return the builder instance
         */
        public Builder topics(List<String> topics) {
            this.topics = topics;
            return this;
        }

        /**
         * Sets names of the ESS secrets the application may reach, replacing the current grants.
         * <p>
         * Like the other three resource lists, setting this replaces the application's whole
         * resource list - see the class description.
         *
         * @param secrets names of the ESS secrets the application may reach
         * @return the builder instance
         */
        public Builder secrets(List<String> secrets) {
            this.secrets = secrets;
            return this;
        }

        /**
         * Moves the application to another namespace.
         * <p>
         * A move, not a field change: the namespace is part of what identifies an application, so
         * its row, its ERN and the grants of a principal EAP minted for it all follow it, and it
         * stops running where it was. {@code ""} moves it back to the account root - so empty and
         * unset mean different things here. The runtime name stays as it is. Refused with HTTP 409
         * if the target namespace already has an application of this name. To leave the original
         * running, use {@link de.jensvogt.euclid.module.eap.EuclidEap#copyApplication} instead.
         *
         * @param namespace the namespace to move the application to, or {@code ""} for the account root
         * @return the builder instance
         */
        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        /**
         * Changes the identity the application runs and signs its calls as.
         * <p>
         * This is the way out of a definition whose user was deleted or renamed underneath it -
         * see {@link de.jensvogt.euclid.dto.eap.model.Application#userExists()}. The user has to
         * exist and already have an access key, or the change is refused. A technical principal EAP
         * had minted for the application is deleted along with its grants, since nothing else can
         * use it. It cannot be emptied to get a new technical principal back: that would mint a
         * principal and its key as a side effect of an empty field, so {@code ""} is HTTP 400.
         *
         * @param user an existing user with an access key
         * @return the builder instance
         */
        public Builder user(String user) {
            this.user = user;
            return this;
        }

        /**
         * Sets how much memory each instance may use, in MiB; 0 removes the limit, anything else
         * has to be at least 64.
         * <p>
         * Through this request the change restarts the pool like any other change of definition,
         * which is the one way running JVMs get a heap sized for the new limit.
         * {@link de.jensvogt.euclid.module.eap.EuclidEap#setApplicationLimits} sets it without the
         * restart.
         *
         * @param memoryLimitMb memory each instance may use, in MiB; 0 for no limit
         * @return the builder instance
         */
        public Builder memoryLimitMb(Long memoryLimitMb) {
            this.memoryLimitMb = memoryLimitMb;
            return this;
        }

        /**
         * Sets how much CPU each instance may use, in cores; 0 removes the limit, anything else has
         * to be at least 0.01. Restarts the pool, as {@link #memoryLimitMb(Long)} does.
         *
         * @param cpuLimit CPU each instance may use, in cores; 0 for no limit
         * @return the builder instance
         */
        public Builder cpuLimit(Double cpuLimit) {
            this.cpuLimit = cpuLimit;
            return this;
        }

        /**
         * Sets smallest number of processes to keep running.
         *
         * @param minInstances smallest number of processes to keep running
         * @return the builder instance
         */
        public Builder minInstances(Long minInstances) {
            this.minInstances = minInstances;
            return this;
        }

        /**
         * Sets largest number of processes to run.
         *
         * @param maxInstances largest number of processes to run
         * @return the builder instance
         */
        public Builder maxInstances(Long maxInstances) {
            this.maxInstances = maxInstances;
            return this;
        }

        /**
         * Sets milliseconds to wait for a process to report ready.
         *
         * @param readyTimeoutMs milliseconds to wait for a process to report ready
         * @return the builder instance
         */
        public Builder readyTimeoutMs(Long readyTimeoutMs) {
            this.readyTimeoutMs = readyTimeoutMs;
            return this;
        }


        /**
         * Sets the labels a worker node has to carry to run this application.
         *
         * @param nodeLabels key/value labels, e.g. {@code os=windows}; empty means any node will do
         * @return the builder instance
         */
        public Builder nodeLabels(Map<String, String> nodeLabels) {
            this.nodeLabels = nodeLabels;
            return this;
        }

        /**
         * Sets whether this stays up or runs once and finishes.
         *
         * @param type {@link ApplicationType#PROCESS} or {@link ApplicationType#JOB}
         * @return the builder instance
         */
        public Builder type(ApplicationType type) {
            this.type = type == null ? null : type.wireValue();
            return this;
        }

        /**
         * Sets the cron expression a job runs on, in UTC.
         *
         * @param schedule a cron expression, e.g. {@code "0 2 * * *"} or {@code "@daily"}; only
         *                 valid together with {@link ApplicationType#JOB}, and {@code ""}
         *                 unschedules an existing one
         * @return the builder instance
         */
        public Builder schedule(String schedule) {
            this.schedule = schedule;
            return this;
        }

        /**
         * Builds and returns a new instance of UpdateApplicationRequest using the properties set on the Builder.
         *
         * @return a new UpdateApplicationRequest instance.
         */
        public UpdateApplicationRequest build() {
            return new UpdateApplicationRequest(applicationId, runtime, artifact, version, command, arguments,
                    environment, buckets, queues, topics, secrets, namespace, user, minInstances, maxInstances,
                    readyTimeoutMs, memoryLimitMb, cpuLimit, nodeLabels, type, schedule);
        }
    }
}
