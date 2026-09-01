package de.jensvogt.euclid.dto.esm;

import de.jensvogt.euclid.dto.com.Variant;

/**
 * Request to add or set a user-defined attribute on an object. The same request serves both
 * actions: add-object-attribute leaves an existing attribute of that name untouched, while
 * set-object-attribute overwrites it.
 *
 * @param ern   the ERN of the object carrying the attribute
 * @param name  the attribute name
 * @param value the typed attribute value
 */
public record ObjectAttributeRequest(String ern, String name, Variant value) {

    /**
     * Creates a new instance of the Builder for constructing an ObjectAttributeRequest object.
     *
     * @return a new Builder instance for constructing ObjectAttributeRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ObjectAttributeRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN of the object carrying the attribute.
         */
        private String ern;

        /**
         * The attribute name.
         */
        private String name;

        /**
         * The typed attribute value.
         */
        private Variant value;

        /**
         * Sets the ERN of the object carrying the attribute.
         *
         * @param ern the ERN of the object carrying the attribute
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the attribute name.
         *
         * @param name the attribute name
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the typed attribute value.
         *
         * @param value the typed attribute value
         * @return the builder instance
         */
        public Builder value(Variant value) {
            this.value = value;
            return this;
        }

        /**
         * Builds and returns a new instance of ObjectAttributeRequest using the properties set on the Builder.
         *
         * @return a new ObjectAttributeRequest instance.
         */
        public ObjectAttributeRequest build() {
            return new ObjectAttributeRequest(ern, name, value);
        }
    }
}
