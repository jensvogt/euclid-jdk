package de.jensvogt.euclid.dto.ess;

import de.jensvogt.euclid.dto.ess.model.Secret;

import java.util.List;

/**
 * Response carrying a page of secrets and how many exist in total. No value travels with it - see
 * {@link Secret}.
 *
 * @param secrets the secrets on the requested page, already filtered to those the caller may read
 * @param total   how many secrets match, across every page, counted before that filtering - so this
 *                can exceed the number of secrets a restricted principal actually gets back
 */
public record ListSecretsResponse(List<Secret> secrets, long total) {

    /**
     * Creates a new instance of the Builder for constructing a ListSecretsResponse object.
     *
     * @return a new Builder instance for constructing ListSecretsResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListSecretsResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The secrets on the requested page.
         */
        private List<Secret> secrets = List.of();

        /**
         * How many secrets match, across every page.
         */
        private long total;

        /**
         * Sets the secrets on the requested page.
         *
         * @param secrets the secrets
         * @return the builder instance
         */
        public Builder secrets(List<Secret> secrets) {
            this.secrets = secrets;
            return this;
        }

        /**
         * Sets how many secrets match in total.
         *
         * @param total the total count
         * @return the builder instance
         */
        public Builder total(long total) {
            this.total = total;
            return this;
        }

        /**
         * Builds and returns a new instance of ListSecretsResponse using the properties set on the Builder.
         *
         * @return a new ListSecretsResponse instance.
         */
        public ListSecretsResponse build() {
            return new ListSecretsResponse(secrets, total);
        }
    }
}
