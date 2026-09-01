package de.jensvogt.euclid.dto.ets;

import java.util.List;

/**
 * Request to change an existing transfer server, one field at a time if wanted.
 * <p>
 * Only the fields actually set are sent, and the server only touches the fields it receives, so
 * a caller can flip one setting without resending the whole definition. That is why every field
 * but {@code serverId} is a boxed type: null means "leave alone", not "clear".
 *
 * @param serverId   the server to change; the only required field
 * @param address    address to bind to
 * @param port       TCP port to listen on
 * @param bucket     name of the ESM bucket this server fronts
 * @param userIds    EAM user IDs allowed to log in, replacing the current list
 * @param userGroups EAM user groups whose members may log in, replacing the current list
 * @param hostKey    SFTP only: private SSH host key file
 * @param pasvMin    FTP only: lowest passive data port
 * @param pasvMax    FTP only: highest passive data port
 */
public record UpdateServerRequest(String serverId, String address, Long port, String bucket, List<String> userIds,
                                  List<String> userGroups, String hostKey, Long pasvMin, Long pasvMax) {

    /**
     * Creates a new instance of the Builder for constructing an UpdateServerRequest object.
     *
     * @return a new Builder instance for constructing UpdateServerRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link UpdateServerRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The server to change; the only required field.
         */
        private String serverId;

        /**
         * Address to bind to.
         */
        private String address;

        /**
         * TCP port to listen on.
         */
        private Long port;

        /**
         * Name of the ESM bucket this server fronts.
         */
        private String bucket;

        /**
         * EAM user IDs allowed to log in, replacing the current list.
         */
        private List<String> userIds;

        /**
         * EAM user groups whose members may log in, replacing the current list.
         */
        private List<String> userGroups;

        /**
         * SFTP only: private SSH host key file.
         */
        private String hostKey;

        /**
         * FTP only: lowest passive data port.
         */
        private Long pasvMin;

        /**
         * FTP only: highest passive data port.
         */
        private Long pasvMax;

        /**
         * Sets the server to change; the only required field.
         *
         * @param serverId the server to change; the only required field
         * @return the builder instance
         */
        public Builder serverId(String serverId) {
            this.serverId = serverId;
            return this;
        }

        /**
         * Sets address to bind to.
         *
         * @param address address to bind to
         * @return the builder instance
         */
        public Builder address(String address) {
            this.address = address;
            return this;
        }

        /**
         * Sets TCP port to listen on.
         *
         * @param port TCP port to listen on
         * @return the builder instance
         */
        public Builder port(Long port) {
            this.port = port;
            return this;
        }

        /**
         * Sets name of the ESM bucket this server fronts.
         *
         * @param bucket name of the ESM bucket this server fronts
         * @return the builder instance
         */
        public Builder bucket(String bucket) {
            this.bucket = bucket;
            return this;
        }

        /**
         * Sets EAM user IDs allowed to log in, replacing the current list.
         *
         * @param userIds EAM user IDs allowed to log in, replacing the current list
         * @return the builder instance
         */
        public Builder userIds(List<String> userIds) {
            this.userIds = userIds;
            return this;
        }

        /**
         * Sets EAM user groups whose members may log in, replacing the current list.
         *
         * @param userGroups EAM user groups whose members may log in, replacing the current list
         * @return the builder instance
         */
        public Builder userGroups(List<String> userGroups) {
            this.userGroups = userGroups;
            return this;
        }

        /**
         * Sets SFTP only: private SSH host key file.
         *
         * @param hostKey SFTP only: private SSH host key file
         * @return the builder instance
         */
        public Builder hostKey(String hostKey) {
            this.hostKey = hostKey;
            return this;
        }

        /**
         * Sets FTP only: lowest passive data port.
         *
         * @param pasvMin FTP only: lowest passive data port
         * @return the builder instance
         */
        public Builder pasvMin(Long pasvMin) {
            this.pasvMin = pasvMin;
            return this;
        }

        /**
         * Sets FTP only: highest passive data port.
         *
         * @param pasvMax FTP only: highest passive data port
         * @return the builder instance
         */
        public Builder pasvMax(Long pasvMax) {
            this.pasvMax = pasvMax;
            return this;
        }

        /**
         * Builds and returns a new instance of UpdateServerRequest using the properties set on the Builder.
         *
         * @return a new UpdateServerRequest instance.
         */
        public UpdateServerRequest build() {
            return new UpdateServerRequest(serverId, address, port, bucket, userIds, userGroups, hostKey, pasvMin, pasvMax);
        }
    }
}
