package de.jensvogt.euclid.dto.eam;

import de.jensvogt.euclid.dto.eam.model.AccessKey;

import java.util.List;

/**
 * Response returned from list-access-keys.
 *
 * @param accessKeys the caller's access keys
 */
public record ListAccessKeysResponse(List<AccessKey> accessKeys) {

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
         * The caller's access keys.
         */
        private List<AccessKey> accessKeys;

        /**
         * Sets the caller's access keys.
         *
         * @param accessKeys the access keys
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
            return new ListAccessKeysResponse(accessKeys);
        }
    }
}
