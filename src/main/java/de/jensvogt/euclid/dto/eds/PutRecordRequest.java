package de.jensvogt.euclid.dto.eds;

import java.util.List;

/**
 * Request to write a record set: every value of one type at one name, replacing the set there.
 *
 * <p>Because the whole set is replaced in one write, there is never a moment when half the old
 * values and half the new ones are served. Values are given in zone-file presentation form without
 * the quoting, and are checked when written, so one the server could not serve is refused with
 * HTTP 400:
 * <ul>
 *     <li>{@code A} - {@code 192.0.2.1}</li>
 *     <li>{@code AAAA} - {@code 2001:db8::1}</li>
 *     <li>{@code CNAME}, {@code NS}, {@code PTR} - {@code target.example.com}</li>
 *     <li>{@code MX} - {@code 10 mail.example.com}</li>
 *     <li>{@code SRV} - {@code 10 5 8080 host.example.com} (priority, weight, port, target)</li>
 *     <li>{@code TXT} - any text, up to 4000 bytes</li>
 * </ul>
 * A CNAME has exactly one value, must be the only set at its name (HTTP 409 otherwise) and can never
 * be at the apex (HTTP 400).
 *
 * @param zone   the zone's apex
 * @param name   the owner name: relative ({@code "www"}), fully qualified ({@code "www.corp.example"}),
 *               with a trailing dot, {@code "@"} or {@code null} for the apex, or a wildcard ({@code "*.dev"})
 * @param type   the type mnemonic: A, AAAA, CNAME, MX, NS, PTR, SRV or TXT
 * @param ttl    TTL in seconds; 0 takes the zone's
 * @param values the values, at least one
 */
public record PutRecordRequest(String zone, String name, String type, long ttl, List<String> values) {

    /**
     * Copies {@code values} so a list the caller changes afterwards does not change the request.
     */
    public PutRecordRequest {
        values = values == null ? List.of() : List.copyOf(values);
    }

    /**
     * Creates a new Builder for constructing a PutRecordRequest.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link PutRecordRequest} instances.
     */
    public static final class Builder {

        /**
         * The zone's apex.
         */
        private String zone;

        /**
         * The owner name; the apex unless set.
         */
        private String name = "@";

        /**
         * The type mnemonic.
         */
        private String type;

        /**
         * TTL in seconds; 0 takes the zone's.
         */
        private long ttl;

        /**
         * The values.
         */
        private List<String> values = List.of();

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Sets the zone the set is written into.
         *
         * @param zone the zone's apex
         * @return this builder
         */
        public Builder zone(String zone) {
            this.zone = zone;
            return this;
        }

        /**
         * Sets the owner name; defaults to {@code "@"}, the apex.
         *
         * @param name the owner name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the record type.
         *
         * @param type the type mnemonic, e.g. {@code "A"}
         * @return this builder
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the TTL of the set.
         *
         * @param ttl TTL in seconds; 0 takes the zone's
         * @return this builder
         */
        public Builder ttl(long ttl) {
            this.ttl = ttl;
            return this;
        }

        /**
         * Sets the values of the set.
         *
         * @param values the values, in zone-file presentation form
         * @return this builder
         */
        public Builder values(List<String> values) {
            this.values = values;
            return this;
        }

        /**
         * Sets the values of the set.
         *
         * @param values the values, in zone-file presentation form
         * @return this builder
         */
        public Builder values(String... values) {
            this.values = List.of(values);
            return this;
        }

        /**
         * Builds the request.
         *
         * @return a new PutRecordRequest
         */
        public PutRecordRequest build() {
            return new PutRecordRequest(zone, name, type, ttl, values);
        }
    }
}
