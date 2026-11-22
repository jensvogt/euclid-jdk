package de.jensvogt.euclid.dto.ekv;

import de.jensvogt.euclid.dto.ekv.model.Item;

import java.util.List;

/**
 * Response carrying the items one query matched, in sort-key order, along with how many items that
 * query matches across every page.
 *
 * <p>{@link #total()} counts what the same partition and sort-key condition take, not the whole
 * table - so it is what a caller paging through a partition needs to know how far it has to go. It
 * costs the server what the query itself costs and no more, the condition being a prefix of the
 * index the items are stored under.
 *
 * @param items the matching items, ordered by sort key
 * @param count how many items came back, which is {@code items.size()}
 * @param total how many items the query matches, across every page. Zero from a server that
 *              predates the field, so a caller that pages until it gets back less than it asked
 *              for keeps working against either
 */
public record QueryResponse(List<Item> items, long count, long total) {

    /**
     * Creates a response without a total - the shape this record had before the server counted
     * what a query matches, so code written against it still compiles.
     *
     * @param items the matching items, ordered by sort key
     * @param count how many items came back
     */
    public QueryResponse(List<Item> items, long count) {
        this(items, count, 0);
    }

    /**
     * Creates a new instance of the Builder for constructing a QueryResponse object.
     *
     * @return a new Builder instance for constructing QueryResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link QueryResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The matching items.
         */
        private List<Item> items = List.of();

        /**
         * How many items came back.
         */
        private long count;

        /**
         * How many items the query matches, across every page.
         */
        private long total;

        /**
         * Sets the matching items.
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
         * Sets how many items the query matches in total.
         *
         * @param total the total count
         * @return the builder instance
         */
        public Builder total(long total) {
            this.total = total;
            return this;
        }

        /**
         * Builds and returns a new instance of QueryResponse using the properties set on the Builder.
         *
         * @return a new QueryResponse instance.
         */
        public QueryResponse build() {
            return new QueryResponse(items, count, total);
        }
    }
}
