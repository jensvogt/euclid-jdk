package de.jensvogt.euclid.dto.ets;

/**
 * Request to list transfer servers, optionally filtered by server ID prefix.
 *
 * @param prefix only servers whose ID starts with this prefix are returned
 */
public record ListServersRequest(String prefix) {

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
         * Builds and returns a new instance of ListServersRequest using the properties set on the Builder.
         *
         * @return a new ListServersRequest instance.
         */
        public ListServersRequest build() {
            return new ListServersRequest(prefix);
        }
    }
}
