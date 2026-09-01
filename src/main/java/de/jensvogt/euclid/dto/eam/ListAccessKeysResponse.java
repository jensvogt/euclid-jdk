package de.jensvogt.euclid.dto.eam;

import de.jensvogt.euclid.dto.Metadata;
import de.jensvogt.euclid.dto.eam.model.AccessKey;
import java.util.List;

/**
 * Response returned from list-access-keys.
 *
 * @param metadata   the caller identity the server resolved the request to
 * @param accessKeys the caller's access keys
 */
public record ListAccessKeysResponse(Metadata metadata, List<AccessKey> accessKeys) {

    /**
     * Creates a new instance of the Builder for constructing a ListAccessKeysResponse object.
     *
     * @return a new Builder instance for constructing ListAccessKeysResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListAccessKeysResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The caller identity the server resolved the request to.
         */
        private Metadata metadata;

        /**
         * The caller's access keys.
         */
        private List<AccessKey> accessKeys;

        /**
         * Sets the caller identity the server resolved the request to.
         *
         * @param metadata the caller identity the server resolved the request to
         * @return the builder instance
         */
        public Builder metadata(Metadata metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Sets the caller's access keys.
         *
         * @param accessKeys the caller's access keys
         * @return the builder instance
         */
        public Builder accessKeys(List<AccessKey> accessKeys) {
            this.accessKeys = accessKeys;
            return this;
        }

        /**
         * Builds and returns a new instance of ListAccessKeysResponse using the properties set on the Builder.
         *
         * @return a new ListAccessKeysResponse instance.
         */
        public ListAccessKeysResponse build() {
            return new ListAccessKeysResponse(metadata, accessKeys);
        }
    }
}
