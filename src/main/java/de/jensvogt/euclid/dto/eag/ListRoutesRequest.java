package de.jensvogt.euclid.dto.eag;

/**
 * Request to list the gateway's routes.
 *
 * <p>The prefix filters on the route's <em>path</em>, not on its ID - which is what somebody asking
 * "what is published under {@code /api}" wants. It is matched literally rather than as a pattern,
 * so {@code /api/v1.0} does not also match {@code /api/v1X0} - unless {@code matchType} asks for a
 * regular expression, which is the one way to have the text read as a pattern.
 *
 * <p>There is no paging here: the route table is a piece of configuration somebody wrote, not a
 * data set, and an installation with enough routes for a page boundary to matter would be one
 * nobody could reason about anyway.
 *
 * @param prefix    the text routes' paths are matched against; empty returns all of them. Despite
 *                  the name it is only a prefix under the default {@code matchType}
 * @param matchType how {@code prefix} is matched: {@code "prefix"}, {@code "endsWith"},
 *                  {@code "contains"} or {@code "regex"}, or {@code null} for {@code "prefix"},
 *                  which is what every listing did before the field existed. Anything else - and a
 *                  regular expression that does not compile - is refused with 400 rather than
 *                  reaching the database as a 500
 */
public record ListRoutesRequest(String prefix, String matchType) {

    /**
     * Creates a new instance of the Builder for constructing a ListRoutesRequest object.
     *
     * @return a new Builder instance for constructing ListRoutesRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListRoutesRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Only routes whose path starts with this are returned.
         */
        private String prefix = "";

        /**
         * How the prefix is matched; null for a plain prefix match.
         */
        private String matchType;

        /**
         * Sets the path prefix to filter by.
         *
         * @param prefix the path prefix
         * @return the builder instance
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Sets how the prefix text is matched against each path.
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
         * Builds and returns a new instance of ListRoutesRequest using the properties set on the Builder.
         *
         * @return a new ListRoutesRequest instance.
         */
        public ListRoutesRequest build() {
            return new ListRoutesRequest(prefix, matchType);
        }
    }
}
