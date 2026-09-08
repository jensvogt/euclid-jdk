package de.jensvogt.euclid.dto.ess;

/**
 * Request to set a tag on a secret. A tag key that is already there is overwritten.
 *
 * @param name  the name of the secret to tag
 * @param key   the tag key
 * @param value the tag value
 */
public record AddSecretTagRequest(String name, String key, String value) {

    /**
     * Creates a new instance of the Builder for constructing an AddSecretTagRequest object.
     *
     * @return a new Builder instance for constructing AddSecretTagRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link AddSecretTagRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The name of the secret to tag.
         */
        private String name;

        /**
         * The tag key.
         */
        private String key;

        /**
         * The tag value.
         */
        private String value = "";

        /**
         * Sets the name of the secret to tag.
         *
         * @param name the secret name
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the tag key.
         *
         * @param key the tag key
         * @return the builder instance
         */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        /**
         * Sets the tag value.
         *
         * @param value the tag value
         * @return the builder instance
         */
        public Builder value(String value) {
            this.value = value;
            return this;
        }

        /**
         * Builds and returns a new instance of AddSecretTagRequest using the properties set on the Builder.
         *
         * @return a new AddSecretTagRequest instance.
         */
        public AddSecretTagRequest build() {
            return new AddSecretTagRequest(name, key, value);
        }
    }
}
