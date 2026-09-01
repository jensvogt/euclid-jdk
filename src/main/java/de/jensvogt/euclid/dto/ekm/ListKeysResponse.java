package de.jensvogt.euclid.dto.ekm;

import de.jensvogt.euclid.dto.ekm.model.Key;
import java.util.List;

/**
 * The keys a list-keys action returned, along with how many exist in total.
 *
 * @param keys  the keys on this page
 * @param total the total number of keys matching the request
 */
public record ListKeysResponse(List<Key> keys, long total) {

    /**
     * Creates a new instance of the Builder for constructing a ListKeysResponse object.
     *
     * @return a new Builder instance for constructing ListKeysResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListKeysResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The keys on this page.
         */
        private List<Key> keys;

        /**
         * The total number of keys matching the request.
         */
        private long total = 0;

        /**
         * Sets the keys on this page.
         *
         * @param keys the keys on this page
         * @return the builder instance
         */
        public Builder keys(List<Key> keys) {
            this.keys = keys;
            return this;
        }

        /**
         * Sets the total number of keys matching the request.
         *
         * @param total the total number of keys matching the request
         * @return the builder instance
         */
        public Builder total(long total) {
            this.total = total;
            return this;
        }

        /**
         * Builds and returns a new instance of ListKeysResponse using the properties set on the Builder.
         *
         * @return a new ListKeysResponse instance.
         */
        public ListKeysResponse build() {
            return new ListKeysResponse(keys, total);
        }
    }
}
