package de.jensvogt.euclid.dto.ekv;

import java.util.Map;

/**
 * Request to write an item, replacing whatever was stored under its key.
 *
 * <p>It replaces rather than merges: writing {@code {"supplierId":"4711","name":"x"}} over a fuller
 * record leaves that record with two attributes. Read, change and write back until the server grows
 * an update-item action.
 *
 * <p>The item has to carry the table's key attributes, with the types the table declared for them,
 * and no attribute name may be empty, start with {@code $} or contain {@code .}.
 *
 * @param table the table to write to
 * @param item  the item, whose values may be scalars, lists or nested maps
 */
public record PutItemRequest(String table, Map<String, Object> item) {

    /**
     * Creates a new instance of the Builder for constructing a PutItemRequest object.
     *
     * @return a new Builder instance for constructing PutItemRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link PutItemRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The table to write to.
         */
        private String table = "";

        /**
         * The item to write.
         */
        private Map<String, Object> item = Map.of();

        /**
         * Sets the table to write to.
         *
         * @param table the table name
         * @return the builder instance
         */
        public Builder table(String table) {
            this.table = table;
            return this;
        }

        /**
         * Sets the item to write.
         *
         * @param item the item
         * @return the builder instance
         */
        public Builder item(Map<String, Object> item) {
            this.item = item;
            return this;
        }

        /**
         * Builds and returns a new instance of PutItemRequest using the properties set on the Builder.
         *
         * @return a new PutItemRequest instance.
         */
        public PutItemRequest build() {
            return new PutItemRequest(table, item);
        }
    }
}
