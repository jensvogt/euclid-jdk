package de.jensvogt.euclid.dto.ekv;

import de.jensvogt.euclid.dto.ekv.model.KeyType;

/**
 * Request to create a table with the key its items are identified by.
 *
 * <p>The key is the one thing a table declares, and it cannot be changed afterwards. A table with a
 * partition key alone holds records looked up one at a time by name; adding a sort key orders the
 * items sharing a partition key, which is what makes a range query over them possible and the only
 * reason to declare one.
 *
 * <p>Key attribute names may not be empty, start with {@code $} or contain {@code .}, and the sort
 * key has to be a different attribute from the partition key.
 *
 * @param name             the table name, unique within the account
 * @param partitionKey     the attribute every item is identified by
 * @param partitionKeyType the type that attribute has, as a {@link KeyType#wireValue()}
 * @param sortKey          the attribute items within a partition are ordered by, or empty for none
 * @param sortKeyType      the type that attribute has, ignored when there is no sort key
 */
public record CreateTableRequest(String name, String partitionKey, String partitionKeyType, String sortKey,
                                 String sortKeyType) {

    /**
     * Creates a new instance of the Builder for constructing a CreateTableRequest object.
     *
     * @return a new Builder instance for constructing CreateTableRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CreateTableRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The table name.
         */
        private String name = "";

        /**
         * The attribute every item is identified by.
         */
        private String partitionKey = "";

        /**
         * The type the partition key attribute has.
         */
        private String partitionKeyType = KeyType.STRING.wireValue();

        /**
         * The attribute items within a partition are ordered by, empty for none.
         */
        private String sortKey = "";

        /**
         * The type the sort key attribute has.
         */
        private String sortKeyType = KeyType.STRING.wireValue();

        /**
         * Sets the table name.
         *
         * @param name the table name
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the attribute every item is identified by.
         *
         * @param partitionKey the partition key attribute name
         * @return the builder instance
         */
        public Builder partitionKey(String partitionKey) {
            this.partitionKey = partitionKey;
            return this;
        }

        /**
         * Sets the type the partition key attribute has, defaulting to {@link KeyType#STRING}.
         *
         * @param partitionKeyType the partition key type
         * @return the builder instance
         */
        public Builder partitionKeyType(KeyType partitionKeyType) {
            this.partitionKeyType = partitionKeyType == null ? "" : partitionKeyType.wireValue();
            return this;
        }

        /**
         * Sets the attribute items within a partition are ordered by.
         *
         * @param sortKey the sort key attribute name
         * @return the builder instance
         */
        public Builder sortKey(String sortKey) {
            this.sortKey = sortKey;
            return this;
        }

        /**
         * Sets the type the sort key attribute has, defaulting to {@link KeyType#STRING}. Ignored
         * by the server when the table has no sort key.
         *
         * @param sortKeyType the sort key type
         * @return the builder instance
         */
        public Builder sortKeyType(KeyType sortKeyType) {
            this.sortKeyType = sortKeyType == null ? "" : sortKeyType.wireValue();
            return this;
        }

        /**
         * Builds and returns a new instance of CreateTableRequest using the properties set on the Builder.
         *
         * @return a new CreateTableRequest instance.
         */
        public CreateTableRequest build() {
            return new CreateTableRequest(name, partitionKey, partitionKeyType, sortKey, sortKeyType);
        }
    }
}
