package de.jensvogt.euclid.dto.ets;

/**
 * Request to list transfer servers, optionally filtered by server ID.
 *
 * <p>The text is matched literally under every match type but {@code "regex"} - it is quoted before
 * it reaches the database - so a search for {@code "a.b"} does not also find {@code "aXb"}.
 *
 * @param prefix    the text server IDs are matched against; empty returns every server in the
 *                  caller's account and namespace. Despite the name it is only a prefix under the
 *                  default {@code matchType}
 * @param matchType how {@code prefix} is matched: {@code "prefix"}, {@code "endsWith"},
 *                  {@code "contains"} or {@code "regex"}, or {@code null} for {@code "prefix"},
 *                  which is what every listing did before the field existed. Anything else - and a
 *                  regular expression that does not compile - is refused with 400 rather than
 *                  reaching the database as a 500
 */
public record ListServersRequest(String prefix, String matchType) {

    /**
     * Creates a new instance of the Builder for constructing a ListServersRequest object.
     *
     * @return a new Builder instance for constructing ListServersRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListServersRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Only servers whose ID starts with this prefix are returned.
         */
        private String prefix = "";

        /**
         * How the prefix is matched; null for a plain prefix match.
         */
        private String matchType;

        /**
         * Sets only servers whose ID starts with this prefix are returned.
         *
         * @param prefix only servers whose ID starts with this prefix are returned
         * @return the builder instance
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Sets how the prefix text is matched against each server ID.
         *
         * @param matchType {@code "prefix"}, {@code "endsWith"}, {@code "contains"} or
         *                  {@code "regex"}; {@code null} for {@code "prefix"}. Every type but
         *                  {@code "regex"} treats the text as a literal
         * @return the builder instance
         */
        public Builder matchType(String matchType) {
            this.matchType = matchType;
            return this;
        }

        /**
         * Builds and returns a new instance of ListServersRequest using the properties set on the Builder.
         *
         * @return a new ListServersRequest instance.
         */
        public ListServersRequest build() {
            return new ListServersRequest(prefix, matchType);
        }
    }
}
