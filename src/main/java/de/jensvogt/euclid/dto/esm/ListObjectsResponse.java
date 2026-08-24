package de.jensvogt.euclid.dto.esm;

import de.jensvogt.euclid.dto.esm.model.EsmObject;

import java.util.List;

/**
 * Response containing a page of objects.
 *
 * @param objects the list of objects returned
 * @param total   the total number of objects matching the request, across all pages
 */
public record ListObjectsResponse(List<EsmObject> objects, long total) {

    /**
     * Creates a new instance of the Builder for constructing a ListObjectsResponse object.
     *
     * @return a new Builder instance for constructing ListObjectsResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListObjectsResponse} instances.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The list of objects returned.
         */
        private List<EsmObject> objects;

        /**
         * The total number of objects matching the request, across all pages.
         */
        private long total;

        /**
         * Sets the list of objects.
         *
         * @param objects the list of objects returned
         * @return the builder instance
         */
        public Builder objects(List<EsmObject> objects) {
            this.objects = objects;
            return this;
        }

        /**
         * Sets the total number of objects matching the request.
         *
         * @param total the total number of objects, across all pages
         * @return the builder instance
         */
        public Builder total(long total) {
            this.total = total;
            return this;
        }

        /**
         * Builds and returns a new instance of ListObjectsResponse using the properties set on the Builder.
         *
         * @return a new ListObjectsResponse instance populated with the objects and total values.
         */
        public ListObjectsResponse build() {
            return new ListObjectsResponse(objects, total);
        }
    }
}
