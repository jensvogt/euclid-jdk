package de.jensvogt.euclid.dto.eap;

/**
 * Request to list applications, optionally filtered by their ID.
 * <p>
 * The field is called {@code prefix} for the reason every euclid listing calls it that: a prefix is
 * what it was before {@code matchType} existed, and an absent or empty {@code matchType} still
 * means exactly that, so a request from an older client is answered as it always was.
 *
 * @param prefix    the search text an application's ID is matched against; empty lists them all
 * @param matchType how {@code prefix} is matched: {@code "prefix"} (also what {@code null} or empty
 *                  means), {@code "endsWith"}, {@code "contains"} or {@code "regex"}. Every type but
 *                  {@code "regex"} treats the text as a literal, so a search for {@code "a.b"} does
 *                  not also find {@code "aXb"}. Anything else, or a regular expression that does not
 *                  compile, is refused with HTTP 400 rather than quietly matching everything
 */
public record ListApplicationsRequest(String prefix, String matchType) {

    /**
     * Creates a new instance of the Builder for constructing a ListApplicationsRequest object.
     *
     * @return a new Builder instance for constructing ListApplicationsRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListApplicationsRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The search text an application's ID is matched against.
         */
        private String prefix = "";

        /**
         * How the search text is matched; null leaves it to the server, which reads it as a prefix.
         */
        private String matchType;

        /**
         * Sets the search text an application's ID is matched against.
         *
         * @param prefix the search text; empty lists every application
         * @return the builder instance
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Sets how the search text is matched against an application's ID.
         *
         * @param matchType {@code "prefix"}, {@code "endsWith"}, {@code "contains"} or
         *                  {@code "regex"}; {@code null} for the server's default, a prefix
         * @return the builder instance
         */
        public Builder matchType(String matchType) {
            this.matchType = matchType;
            return this;
        }

        /**
         * Builds and returns a new instance of ListApplicationsRequest using the properties set on the Builder.
         *
         * @return a new ListApplicationsRequest instance.
         */
        public ListApplicationsRequest build() {
            return new ListApplicationsRequest(prefix, matchType);
        }
    }
}
