package de.jensvogt.euclid.dto.ekm;

/**
 * Request to add a tag to a key. The tag is upserted, so a key that already carries this tag
 * has its value replaced - there is no separate set-key-tag action.
 *
 * @param ern   the ERN of the key to tag
 * @param key   the tag key
 * @param value the tag value
 */
public record AddKeyTagRequest(String ern, String key, String value) {

    /**
     * Creates a new instance of the Builder for constructing an AddKeyTagRequest object.
     *
     * @return a new Builder instance for constructing AddKeyTagRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link AddKeyTagRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN of the key to tag.
         */
        private String ern;

        /**
         * The tag key.
         */
        private String key;

        /**
         * The tag value.
         */
        private String value;

        /**
         * Sets the ERN of the key to tag.
         *
         * @param ern the ERN of the key to tag
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
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
         * Builds and returns a new instance of AddKeyTagRequest using the properties set on the Builder.
         *
         * @return a new AddKeyTagRequest instance.
         */
        public AddKeyTagRequest build() {
            return new AddKeyTagRequest(ern, key, value);
        }
    }
}
