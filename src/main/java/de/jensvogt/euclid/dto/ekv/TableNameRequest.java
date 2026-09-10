package de.jensvogt.euclid.dto.ekv;

/**
 * Request body for the two table actions that name nothing but a table: describe-table and
 * delete-table.
 *
 * @param name the table the action applies to
 */
public record TableNameRequest(String name) {

    /**
     * Creates a new instance of the Builder for constructing a TableNameRequest object.
     *
     * @return a new Builder instance for constructing TableNameRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link TableNameRequest} instances.
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
        private String name = "";

        /**
         * Sets the table the action applies to.
         *
         * @param name the table name
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Builds and returns a new instance of TableNameRequest using the properties set on the Builder.
         *
         * @return a new TableNameRequest instance.
         */
        public TableNameRequest build() {
            return new TableNameRequest(name);
        }
    }
}
