package de.jensvogt.euclid.dto.esm;

import de.jensvogt.euclid.dto.com.Variant;
import java.util.Map;

/**
 * The user-defined attributes an object carries, keyed by attribute name.
 *
 * @param ern        the ERN of the object the attributes belong to
 * @param attributes the attributes, keyed by name
 * @param total      the number of attributes returned
 */
public record ListObjectAttributesResponse(String ern, Map<String, Variant> attributes, long total) {

    /**
     * Creates a new instance of the Builder for constructing a ListObjectAttributesResponse object.
     *
     * @return a new Builder instance for constructing ListObjectAttributesResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListObjectAttributesResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN of the object the attributes belong to.
         */
        private String ern;

        /**
         * The attributes, keyed by name.
         */
        private Map<String, Variant> attributes;

        /**
         * The number of attributes returned.
         */
        private long total = 0;

        /**
         * Sets the ERN of the object the attributes belong to.
         *
         * @param ern the ERN of the object the attributes belong to
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the attributes, keyed by name.
         *
         * @param attributes the attributes, keyed by name
         * @return the builder instance
         */
        public Builder attributes(Map<String, Variant> attributes) {
            this.attributes = attributes;
            return this;
        }

        /**
         * Sets the number of attributes returned.
         *
         * @param total the number of attributes returned
         * @return the builder instance
         */
        public Builder total(long total) {
            this.total = total;
            return this;
        }

        /**
         * Builds and returns a new instance of ListObjectAttributesResponse using the properties set on the Builder.
         *
         * @return a new ListObjectAttributesResponse instance.
         */
        public ListObjectAttributesResponse build() {
            return new ListObjectAttributesResponse(ern, attributes, total);
        }
    }
}
