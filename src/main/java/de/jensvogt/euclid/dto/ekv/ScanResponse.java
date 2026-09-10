package de.jensvogt.euclid.dto.ekv;

import de.jensvogt.euclid.dto.ekv.model.Item;

import java.util.List;

/**
 * Response carrying a page of a table's items, along with how many the table holds.
 *
 * <p>A scan reads the table rather than an index, so {@link #total()} is what a caller paging
 * through one needs to know how far it has to go - and what tells it, before it starts, that the
 * table is larger than it wanted to read.
 *
 * @param items the items on the requested page
 * @param count how many items came back, which is {@code items.size()}
 * @param total how many items the table holds, across every page
 */
public record ScanResponse(List<Item> items, long count, long total) {

    /**
     * Creates a new instance of the Builder for constructing a ScanResponse object.
     *
     * @return a new Builder instance for constructing ScanResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ScanResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The items on the requested page.
         */
        private List<Item> items = List.of();

        /**
         * How many items came back.
         */
        private long count;

        /**
         * How many items the table holds, across every page.
         */
        private long total;

        /**
         * Sets the items on the requested page.
         *
         * @param items the items
         * @return the builder instance
         */
        public Builder items(List<Item> items) {
            this.items = items;
            return this;
        }

        /**
         * Sets how many items came back.
         *
         * @param count the count
         * @return the builder instance
         */
        public Builder count(long count) {
            this.count = count;
            return this;
        }

        /**
         * Sets how many items the table holds in total.
         *
         * @param total the total count
         * @return the builder instance
         */
        public Builder total(long total) {
            this.total = total;
            return this;
        }

        /**
         * Builds and returns a new instance of ScanResponse using the properties set on the Builder.
         *
         * @return a new ScanResponse instance.
         */
        public ScanResponse build() {
            return new ScanResponse(items, count, total);
        }
    }
}
