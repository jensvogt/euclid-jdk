package de.jensvogt.euclid.dto.ets;

import java.util.List;

/**
 * Request to define a new FTP or SFTP transfer server fronting an ESM bucket.
 * <p>
 * Fields left unset are omitted from the request rather than sent as null, so the server applies
 * its own default for them - which is why the optional ones are boxed types.
 *
 * @param serverId   name identifying the server, unique across the installation
 * @param protocol   transfer protocol, {@code "FTP"} or {@code "SFTP"}
 * @param port       TCP port to listen on; must be 1-65535 and not used by another transfer server
 * @param bucket     name of the ESM bucket this server's clients read and write
 * @param address    address to bind to
 * @param userIds    EAM user IDs allowed to log in
 * @param userGroups EAM user groups whose members may log in
 * @param hostKey    SFTP only: private SSH host key file, generated on first start if absent
 * @param pasvMin    FTP only: lowest passive data port; the server defaults to 6000
 * @param pasvMax    FTP only: highest passive data port; the server defaults to 6100
 */
public record CreateServerRequest(String serverId, String protocol, Long port, String bucket, String address,
                                  List<String> userIds, List<String> userGroups, String hostKey, Long pasvMin, Long pasvMax) {

    /**
     * Creates a new instance of the Builder for constructing a CreateServerRequest object.
     *
     * @return a new Builder instance for constructing CreateServerRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CreateServerRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Name identifying the server, unique across the installation.
         */
        private String serverId;

        /**
         * Transfer protocol, {@code "FTP"} or {@code "SFTP"}.
         */
        private String protocol = "SFTP";

        /**
         * TCP port to listen on; must be 1-65535 and not used by another transfer server.
         */
        private Long port;

        /**
         * Name of the ESM bucket this server's clients read and write.
         */
        private String bucket;

        /**
         * Address to bind to.
         */
        private String address = "0.0.0.0";

        /**
         * EAM user IDs allowed to log in.
         */
        private List<String> userIds;

        /**
         * EAM user groups whose members may log in.
         */
        private List<String> userGroups;

        /**
         * SFTP only: private SSH host key file, generated on first start if absent.
         */
        private String hostKey;

        /**
         * FTP only: lowest passive data port; the server defaults to 6000.
         */
        private Long pasvMin;

        /**
         * FTP only: highest passive data port; the server defaults to 6100.
         */
        private Long pasvMax;

        /**
         * Sets name identifying the server, unique across the installation.
         *
         * @param serverId name identifying the server, unique across the installation
         * @return the builder instance
         */
        public Builder serverId(String serverId) {
            this.serverId = serverId;
            return this;
        }

        /**
         * Sets transfer protocol, {@code "FTP"} or {@code "SFTP"}.
         *
         * @param protocol transfer protocol, {@code "FTP"} or {@code "SFTP"}
         * @return the builder instance
         */
        public Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        /**
         * Sets TCP port to listen on; must be 1-65535 and not used by another transfer server.
         *
         * @param port TCP port to listen on; must be 1-65535 and not used by another transfer server
         * @return the builder instance
         */
        public Builder port(Long port) {
            this.port = port;
            return this;
        }

        /**
         * Sets name of the ESM bucket this server's clients read and write.
         *
         * @param bucket name of the ESM bucket this server's clients read and write
         * @return the builder instance
         */
        public Builder bucket(String bucket) {
            this.bucket = bucket;
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
         * Sets EAM user IDs allowed to log in.
         *
         * @param userIds EAM user IDs allowed to log in
         * @return the builder instance
         */
        public Builder userIds(List<String> userIds) {
            this.userIds = userIds;
            return this;
        }

        /**
         * Sets EAM user groups whose members may log in.
         *
         * @param userGroups EAM user groups whose members may log in
         * @return the builder instance
         */
        public Builder userGroups(List<String> userGroups) {
            this.userGroups = userGroups;
            return this;
        }

        /**
         * Sets SFTP only: private SSH host key file, generated on first start if absent.
         *
         * @param hostKey SFTP only: private SSH host key file, generated on first start if absent
         * @return the builder instance
         */
        public Builder hostKey(String hostKey) {
            this.hostKey = hostKey;
            return this;
        }

        /**
         * Sets FTP only: lowest passive data port; the server defaults to 6000.
         *
         * @param pasvMin FTP only: lowest passive data port; the server defaults to 6000
         * @return the builder instance
         */
        public Builder pasvMin(Long pasvMin) {
            this.pasvMin = pasvMin;
            return this;
        }

        /**
         * Sets FTP only: highest passive data port; the server defaults to 6100.
         *
         * @param pasvMax FTP only: highest passive data port; the server defaults to 6100
         * @return the builder instance
         */
        public Builder pasvMax(Long pasvMax) {
            this.pasvMax = pasvMax;
            return this;
        }

        /**
         * Builds and returns a new instance of CreateServerRequest using the properties set on the Builder.
         *
         * @return a new CreateServerRequest instance.
         */
        public CreateServerRequest build() {
            return new CreateServerRequest(serverId, protocol, port, bucket, address, userIds, userGroups, hostKey, pasvMin, pasvMax);
        }
    }
}
