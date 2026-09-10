package de.jensvogt.euclid.dto.ekv;

import java.util.Map;

/**
 * Request body for the two item actions that name nothing but a key: get-item and delete-item.
 *
 * <p>The key names the table's key attributes and only those - the partition key alone where the
 * table has no sort key, both where it has one. An extra attribute is refused rather than ignored,
 * because a caller who names one thinks the table is keyed on something it is not, and answering
 * them with the wrong item would be worse than telling them so.
 *
 * @param table the table the action applies to
 * @param key   the key attributes and their values
 */
public record ItemKeyRequest(String table, Map<String, Object> key) {

    /**
     * Creates a new instance of the Builder for constructing an ItemKeyRequest object.
     *
     * @return a new Builder instance for constructing ItemKeyRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ItemKeyRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The table the action applies to.
         */
        private String table = "";

        /**
         * The key attributes and their values.
         */
        private Map<String, Object> key = Map.of();

        /**
         * Sets the table the action applies to.
         *
         * @param table the table name
         * @return the builder instance
         */
        public Builder table(String table) {
            this.table = table;
            return this;
        }

        /**
         * Sets the key attributes and their values.
         *
         * @param key the key
         * @return the builder instance
         */
        public Builder key(Map<String, Object> key) {
            this.key = key;
            return this;
        }

        /**
         * Builds and returns a new instance of ItemKeyRequest using the properties set on the Builder.
         *
         * @return a new ItemKeyRequest instance.
         */
        public ItemKeyRequest build() {
            return new ItemKeyRequest(table, key);
        }
    }
}
