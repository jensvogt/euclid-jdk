package de.jensvogt.euclid.dto.eap;

import java.util.List;
import java.util.Map;

/**
 * Request to change a deployed application, one field at a time if wanted.
 * <p>
 * Only the fields actually set are sent, and the server only touches the fields it receives, so
 * an update naming a single field leaves the rest of the definition alone. That is why every
 * field but {@code applicationId} is a boxed type: null means "leave alone", not "clear".
 * <p>
 * Setting {@code buckets} or {@code queues} also rewrites the access grants on the principal the
 * application runs as - but only when that principal is one EAP minted itself. A user named at
 * creation is the caller's to grant.
 *
 * @param applicationId  the application to change; the only required field
 * @param runtime        how the artifact is executed
 * @param artifact       key of the artifact object, which must exist in the application's bucket
 * @param command        command to run the artifact with
 * @param arguments      arguments passed to the command, replacing the current list
 * @param environment    environment variables, replacing the current set
 * @param buckets        names of the ESM buckets the application may reach, replacing the current grants
 * @param queues         names of the EQS queues the application may reach, replacing the current grants
 * @param minInstances   smallest number of processes to keep running
 * @param maxInstances   largest number of processes to run
 * @param readyTimeoutMs milliseconds to wait for a process to report ready
 */
public record UpdateApplicationRequest(String applicationId, String runtime, String artifact, String command,
                                       List<String> arguments, Map<String, String> environment, List<String> buckets,
                                       List<String> queues, Long minInstances, Long maxInstances, Long readyTimeoutMs) {

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
         * Builds and returns a new instance of UpdateApplicationRequest using the properties set on the Builder.
         *
         * @return a new UpdateApplicationRequest instance.
         */
        public UpdateApplicationRequest build() {
            return new UpdateApplicationRequest(applicationId, runtime, artifact, command, arguments, environment, buckets, queues, minInstances, maxInstances, readyTimeoutMs);
        }
    }
}
