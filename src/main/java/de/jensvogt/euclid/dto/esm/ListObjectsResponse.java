package de.jensvogt.euclid.dto.esm;

import de.jensvogt.euclid.dto.esm.model.EsmObject;

import java.util.List;

public record ListObjectsResponse(List<EsmObject> objects, long total) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        private List<EsmObject> objects;
        private long total;

        public Builder objects(List<EsmObject> objects) {
            this.objects = objects;
            return this;
        }

        public Builder total(long total) {
            this.total = total;
            return this;
        }

        public ListObjectsResponse build() {
            return new ListObjectsResponse(objects, total);
        }
    }
}
