package de.jensvogt.euclid.dto.esm;

import de.jensvogt.euclid.dto.esm.model.Bucket;

import java.util.List;

/**
 * Response containing a page of buckets.
 *
 * @param buckets the list of buckets returned
 * @param total   the total number of buckets matching the request, across all pages
 */
public record ListBucketsResponse(List<Bucket> buckets, long total) {

    /**
     * Creates a new instance of the Builder for constructing a ListBucketsResponse object.
     *
     * @return a new Builder instance for constructing ListBucketsResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListBucketsResponse} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The list of buckets returned.
         */
        private List<Bucket> buckets;

        /**
         * The total number of buckets matching the request, across all pages.
         */
        private long total;

        /**
         * Sets the list of buckets.
         *
         * @param buckets the list of buckets returned
         * @return the builder instance
         */
        public Builder buckets(List<Bucket> buckets) {
            this.buckets = buckets;
            return this;
        }

        /**
         * Sets the total number of buckets matching the request.
         *
         * @param total the total number of buckets, across all pages
         * @return the builder instance
         */
        public Builder total(long total) {
            this.total = total;
            return this;
        }

        /**
         * Builds and returns a new instance of ListBucketsResponse using the properties set on the Builder.
         *
         * @return a new ListBucketsResponse instance populated with the buckets and total values.
         */
        public ListBucketsResponse build() {
            return new ListBucketsResponse(buckets, total);
        }
    }
}
