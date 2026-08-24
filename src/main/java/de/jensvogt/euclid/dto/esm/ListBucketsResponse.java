package de.jensvogt.euclid.dto.esm;

import de.jensvogt.euclid.dto.esm.model.Bucket;

import java.util.List;

public record ListBucketsResponse(List<Bucket> buckets, long total) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private List<Bucket> buckets;
        private long total;

        public Builder buckets(List<Bucket> buckets) {
            this.buckets = buckets;
            return this;
        }

        public Builder total(long total) {
            this.total = total;
            return this;
        }

        public ListBucketsResponse build() {
            return new ListBucketsResponse(buckets, total);
        }
    }
}
