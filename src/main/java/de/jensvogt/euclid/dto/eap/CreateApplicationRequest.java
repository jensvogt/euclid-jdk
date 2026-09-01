package de.jensvogt.euclid.dto.eap;

import java.util.List;
import java.util.Map;

/**
 * Request to deploy a new application: an artifact stored in an ESM bucket that euclid runs as
 * a pool of processes.
 * <p>
 * Fields left unset are omitted from the request rather than sent as null, so the server applies
 * its own default for them - which is why the optional numbers are boxed types.
 *
 * @param applicationId  name identifying the application, unique across the installation
 * @param runtime        how the artifact is executed: {@code "JAVA"}, {@code "PYTHON"}, {@code "NODEJS"} or {@code "BINARY"}
 * @param bucket         name of the ESM bucket holding the artifact
 * @param artifact       key of the artifact object within that bucket
 * @param command        command to run the artifact with, or empty for the runtime default
 * @param arguments      arguments passed to the command
 * @param environment    environment variables the processes are started with
 * @param buckets        names of the ESM buckets the application is granted access to
 * @param queues         names of the EQS queues the application is granted access to
 * @param user           existing user the application acts as; left unset, EAP mints a technical principal of its own
 * @param minInstances   smallest number of processes to keep running; the server floors this at 1
 * @param maxInstances   largest number of processes to run; the server raises it to minInstances if lower
 * @param readyTimeoutMs milliseconds to wait for a process to report ready; the server floors this at 1000
 */
public record CreateApplicationRequest(String applicationId, String runtime, String bucket, String artifact, String command,
                                       List<String> arguments, Map<String, String> environment, List<String> buckets,
                                       List<String> queues, String user, Long minInstances, Long maxInstances,
                                       Long readyTimeoutMs) {

    /**
     * Creates a new instance of the Builder for constructing a CreateApplicationRequest object.
     *
     * @return a new Builder instance for constructing CreateApplicationRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CreateApplicationRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Name identifying the application, unique across the installation.
         */
        private String applicationId;

        /**
         * How the artifact is executed: {@code "JAVA"}, {@code "PYTHON"}, {@code "NODEJS"} or {@code "BINARY"}.
         */
        private String runtime;

        /**
         * Name of the ESM bucket holding the artifact.
         */
        private String bucket;

        /**
         * Key of the artifact object within that bucket.
         */
        private String artifact;

        /**
         * Command to run the artifact with, or empty for the runtime default.
         */
        private String command;

        /**
         * Arguments passed to the command.
         */
        private List<String> arguments;

        /**
         * Environment variables the processes are started with.
         */
        private Map<String, String> environment;

        /**
         * Names of the ESM buckets the application is granted access to.
         */
        private List<String> buckets;

        /**
         * Names of the EQS queues the application is granted access to.
         */
        private List<String> queues;

        /**
         * Existing user the application acts as; left unset, EAP mints a technical principal of its own.
         */
        private String user;

        /**
         * Smallest number of processes to keep running; the server floors this at 1.
         */
        private Long minInstances;

        /**
         * Largest number of processes to run; the server raises it to minInstances if lower.
         */
        private Long maxInstances;

        /**
         * Milliseconds to wait for a process to report ready; the server floors this at 1000.
         */
        private Long readyTimeoutMs;

        /**
         * Sets name identifying the application, unique across the installation.
         *
         * @param applicationId name identifying the application, unique across the installation
         * @return the builder instance
         */
        public Builder applicationId(String applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        /**
         * Sets how the artifact is executed: {@code "JAVA"}, {@code "PYTHON"}, {@code "NODEJS"} or {@code "BINARY"}.
         *
         * @param runtime how the artifact is executed: {@code "JAVA"}, {@code "PYTHON"}, {@code "NODEJS"} or {@code "BINARY"}
         * @return the builder instance
         */
        public Builder runtime(String runtime) {
            this.runtime = runtime;
            return this;
        }

        /**
         * Sets name of the ESM bucket holding the artifact.
         *
         * @param bucket name of the ESM bucket holding the artifact
         * @return the builder instance
         */
        public Builder bucket(String bucket) {
            this.bucket = bucket;
            return this;
        }

        /**
         * Sets key of the artifact object within that bucket.
         *
         * @param artifact key of the artifact object within that bucket
         * @return the builder instance
         */
        public Builder artifact(String artifact) {
            this.artifact = artifact;
            return this;
        }

        /**
         * Sets command to run the artifact with, or empty for the runtime default.
         *
         * @param command command to run the artifact with, or empty for the runtime default
         * @return the builder instance
         */
        public Builder command(String command) {
            this.command = command;
            return this;
        }

        /**
         * Sets arguments passed to the command.
         *
         * @param arguments arguments passed to the command
         * @return the builder instance
         */
        public Builder arguments(List<String> arguments) {
            this.arguments = arguments;
            return this;
        }

        /**
         * Sets environment variables the processes are started with.
         *
         * @param environment environment variables the processes are started with
         * @return the builder instance
         */
        public Builder environment(Map<String, String> environment) {
            this.environment = environment;
            return this;
        }

        /**
         * Sets names of the ESM buckets the application is granted access to.
         *
         * @param buckets names of the ESM buckets the application is granted access to
         * @return the builder instance
         */
        public Builder buckets(List<String> buckets) {
            this.buckets = buckets;
            return this;
        }

        /**
         * Sets names of the EQS queues the application is granted access to.
         *
         * @param queues names of the EQS queues the application is granted access to
         * @return the builder instance
         */
        public Builder queues(List<String> queues) {
            this.queues = queues;
            return this;
        }

        /**
         * Sets existing user the application acts as; left unset, EAP mints a technical principal of its own.
         *
         * @param user existing user the application acts as; left unset, EAP mints a technical principal of its own
         * @return the builder instance
         */
        public Builder user(String user) {
            this.user = user;
            return this;
        }

        /**
         * Sets smallest number of processes to keep running; the server floors this at 1.
         *
         * @param minInstances smallest number of processes to keep running; the server floors this at 1
         * @return the builder instance
         */
        public Builder minInstances(Long minInstances) {
            this.minInstances = minInstances;
            return this;
        }

        /**
         * Sets largest number of processes to run; the server raises it to minInstances if lower.
         *
         * @param maxInstances largest number of processes to run; the server raises it to minInstances if lower
         * @return the builder instance
         */
        public Builder maxInstances(Long maxInstances) {
            this.maxInstances = maxInstances;
            return this;
        }

        /**
         * Sets milliseconds to wait for a process to report ready; the server floors this at 1000.
         *
         * @param readyTimeoutMs milliseconds to wait for a process to report ready; the server floors this at 1000
         * @return the builder instance
         */
        public Builder readyTimeoutMs(Long readyTimeoutMs) {
            this.readyTimeoutMs = readyTimeoutMs;
            return this;
        }

        /**
         * Builds and returns a new instance of CreateApplicationRequest using the properties set on the Builder.
         *
         * @return a new CreateApplicationRequest instance.
         */
        public CreateApplicationRequest build() {
            return new CreateApplicationRequest(applicationId, runtime, bucket, artifact, command, arguments, environment, buckets, queues, user, minInstances, maxInstances, readyTimeoutMs);
        }
    }
}
