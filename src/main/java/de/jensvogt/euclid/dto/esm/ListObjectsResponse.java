package de.jensvogt.euclid.dto.esm;

import de.jensvogt.euclid.dto.esm.model.EsmObject;

import java.util.List;

/**
 * Response containing a page of objects.
 *
 * @param objects        the list of objects returned
 * @param total          the total number of objects matching the request, across all pages
 * @param commonPrefixes when the listing was asked for one level (a delimiter of {@code "/"}), the
 *                       directories one step below that level, each ending in {@code "/"}; empty
 *                       otherwise. A bucket is flat, so these are derived from the keys rather than
 *                       stored - a directory here is a path objects lie under, whether or not
 *                       anything was ever written to mark it. Not paged: {@code total} counts only
 *                       the objects
 */
public record ListObjectsResponse(List<EsmObject> objects, long total, List<String> commonPrefixes) {

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
         * The directories one step below the listed level, empty for a whole-subtree listing.
         */
        private List<String> commonPrefixes = List.of();

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
         * Sets the directories one step below the listed level.
         *
         * @param commonPrefixes the directories, each ending in {@code "/"}
         * @return the builder instance
         */
        public Builder commonPrefixes(List<String> commonPrefixes) {
            this.commonPrefixes = commonPrefixes;
            return this;
        }

        /**
         * Builds and returns a new instance of ListObjectsResponse using the properties set on the Builder.
         *
         * @return a new ListObjectsResponse instance populated with the objects and total values.
         */
        public ListObjectsResponse build() {
            return new ListObjectsResponse(objects, total, commonPrefixes);
        }
    }
}
