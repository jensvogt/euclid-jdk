package de.jensvogt.euclid.dto.esm;

import de.jensvogt.euclid.dto.com.Variant;

/**
 * The attribute an object carries after an add-object-attribute or set-object-attribute action.
 *
 * @param ern   the ERN of the object carrying the attribute
 * @param name  the attribute name
 * @param value the typed attribute value
 */
public record ObjectAttributeResponse(String ern, String name, Variant value) {

    /**
     * Creates a new instance of the Builder for constructing an ObjectAttributeResponse object.
     *
     * @return a new Builder instance for constructing ObjectAttributeResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ObjectAttributeResponse} instances.
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
         * Builds and returns a new instance of ObjectAttributeResponse using the properties set on the Builder.
         *
         * @return a new ObjectAttributeResponse instance.
         */
        public ObjectAttributeResponse build() {
            return new ObjectAttributeResponse(ern, name, value);
        }
    }
}
