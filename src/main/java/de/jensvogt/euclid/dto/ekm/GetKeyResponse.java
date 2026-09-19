package de.jensvogt.euclid.dto.ekm;

import de.jensvogt.euclid.dto.ekm.model.Key;

/**
 * One key, as a listing describes each of its own.
 * <p>
 * The same {@link Key} a {@code listKeys} answer carries, rather than a shape of its own, so that
 * what a listing shows and what this shows cannot drift apart. As there, it is the key's
 * description and never its material - that never leaves the module, which is the point of a key
 * management service.
 *
 * @param key the key
 */
public record GetKeyResponse(Key key) {

    /**
     * Creates a new instance of the Builder for constructing a GetKeyResponse object.
     *
     * @return a new Builder instance for constructing GetKeyResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetKeyResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The key.
         */
        private Key key;

        /**
         * Sets the key.
         *
         * @param key the key
         * @return the builder instance
         */
        public Builder key(Key key) {
            this.key = key;
            return this;
        }

        /**
         * Builds and returns a new instance of GetKeyResponse using the properties set on the Builder.
         *
         * @return a new GetKeyResponse instance.
         */
        public GetKeyResponse build() {
            return new GetKeyResponse(key);
        }
    }
}
