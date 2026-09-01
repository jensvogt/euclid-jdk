package de.jensvogt.euclid.dto.ets;

/**
 * Request naming a single transfer server, shared by get-server, delete-server, start-server
 * and stop-server - all four take nothing but the ID.
 *
 * @param serverId the ID of the transfer server the action applies to
 */
public record ServerRequest(String serverId) {

    /**
     * Creates a new instance of the Builder for constructing a ServerRequest object.
     *
     * @return a new Builder instance for constructing ServerRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ServerRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ID of the transfer server the action applies to.
         */
        private String serverId;

        /**
         * Sets the ID of the transfer server the action applies to.
         *
         * @param serverId the ID of the transfer server the action applies to
         * @return the builder instance
         */
        public Builder serverId(String serverId) {
            this.serverId = serverId;
            return this;
        }

        /**
         * Builds and returns a new instance of ServerRequest using the properties set on the Builder.
         *
         * @return a new ServerRequest instance.
         */
        public ServerRequest build() {
            return new ServerRequest(serverId);
        }
    }
}
