package de.jensvogt.euclid.dto.ekv;

import de.jensvogt.euclid.dto.ekv.model.TableDescription;

import java.util.List;

/**
 * Response carrying a page of tables and how many the account has.
 *
 * <p>Each description carries an item count, which the server counts rather than looks up, so a
 * listing of many tables is not free.
 *
 * @param tables the tables on the requested page
 * @param total  how many tables the account has, across every page
 */
public record ListTablesResponse(List<TableDescription> tables, long total) {

    /**
     * Creates a new instance of the Builder for constructing a ListTablesResponse object.
     *
     * @return a new Builder instance for constructing ListTablesResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListTablesResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The tables on the requested page.
         */
        private List<TableDescription> tables = List.of();

        /**
         * How many tables the account has, across every page.
         */
        private long total;

        /**
         * Sets the tables on the requested page.
         *
         * @param tables the tables
         * @return the builder instance
         */
        public Builder tables(List<TableDescription> tables) {
            this.tables = tables;
            return this;
        }

        /**
         * Sets how many tables the account has in total.
         *
         * @param total the total count
         * @return the builder instance
         */
        public Builder total(long total) {
            this.total = total;
            return this;
        }

        /**
         * Builds and returns a new instance of ListTablesResponse using the properties set on the Builder.
         *
         * @return a new ListTablesResponse instance.
         */
        public ListTablesResponse build() {
            return new ListTablesResponse(tables, total);
        }
    }
}
