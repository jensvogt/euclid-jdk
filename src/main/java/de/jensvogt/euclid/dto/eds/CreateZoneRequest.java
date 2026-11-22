package de.jensvogt.euclid.dto.eds;

/**
 * Request to create a zone the name server answers for from then on.
 *
 * <p>A zone's name is unique across the installation, not just within the account: a DNS question
 * carries no account, so two accounts each holding {@code corp.example} would leave EDS no way to
 * choose between them. Zones cannot be created inside the service domain
 * ({@code euclid.internal} by default), which belongs to the names euclid gives what it runs.
 *
 * @param name        the zone's apex, e.g. {@code "corp.example"}; a trailing dot is accepted and dropped
 * @param ttl         TTL in seconds of records that name none, and of negative answers; 0 means 300
 * @param description free text, or {@code null}
 */
public record CreateZoneRequest(String name, long ttl, String description) {

    /**
     * Creates a new Builder for constructing a CreateZoneRequest.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CreateZoneRequest} instances.
     */
    public static final class Builder {

        /**
         * The zone's apex.
         */
        private String name;

        /**
         * The zone's default TTL; 0 takes the server's default of 300 seconds.
         */
        private long ttl;

        /**
         * Free text.
         */
        private String description;

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Sets the zone's apex.
         *
         * @param name the apex, e.g. {@code "corp.example"}
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the TTL of records that name none, and of negative answers.
         *
         * @param ttl TTL in seconds; 0 means 300
         * @return this builder
         */
        public Builder ttl(long ttl) {
            this.ttl = ttl;
            return this;
        }

        /**
         * Sets the zone's description.
         *
         * @param description free text
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Builds the request.
         *
         * @return a new CreateZoneRequest
         */
        public CreateZoneRequest build() {
            return new CreateZoneRequest(name, ttl, description);
        }
    }
}
