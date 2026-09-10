package de.jensvogt.euclid.dto.ekv;

import de.jensvogt.euclid.dto.ekv.model.Item;

import java.util.List;

/**
 * Response carrying the items one query matched, in sort-key order.
 *
 * <p>There is no total here, unlike {@link ScanResponse}: counting how many items a condition
 * matches across every page would mean running the query twice, and a caller paging through a
 * partition can tell it has reached the end by getting back less than it asked for.
 *
 * @param items the matching items, ordered by sort key
 * @param count how many items came back, which is {@code items.size()}
 */
public record QueryResponse(List<Item> items, long count) {

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
         * Builds and returns a new instance of QueryResponse using the properties set on the Builder.
         *
         * @return a new QueryResponse instance.
         */
        public QueryResponse build() {
            return new QueryResponse(items, count);
        }
    }
}
