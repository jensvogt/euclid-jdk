package de.jensvogt.euclid.dto.esm;

/**
 * Represents the response for creating a bucket.
 *
 * This class encapsulates the name and the unique identifier (ERN)
 * of the created bucket. It provides a builder for constructing
 * instances in a controlled manner.
 *
 * The builder pattern is used to allow for incremental configuration
 * of the bucket's properties.
 *
 * @param name The name of the created bucket.
 * @param ern  The unique identifier (ERN) of the created bucket.
 */
public record CreateBucketResponse(String name, String ern) {

    /**
     * Creates a new instance of the builder for constructing objects
     * of the enclosing type.
     *
     * The builder provides a flexible and readable way to set properties
     * of the object before creating an instance. This method always
     * starts with a fresh and empty builder.
     *
     * @return a new instance of the Builder class.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder class for constructing instances of {@code CreateBucketResponse}.
     *
     * The builder pattern is used to facilitate the creation of objects in a controlled
     * and flexible manner, especially when the object has multiple configurable properties.
     * This class provides methods to set individual properties before constructing
     * an immutable {@code CreateBucketResponse} instance.
     */
    public static final class Builder {
        /**
         * Creates a new instance of the Builder class.
         *
         * This constructor initializes a new, empty Builder instance for constructing
         * objects of the associated type. It provides a starting point for defining
         * parameters using the builder methods.
         *
         * The builder is used to create an instance of the associated object in a
         * controlled and configurable manner, ensuring clarity and immutability
         * in the final constructed object.
         */
        public Builder() {
        }

        /**
         * Represents the name associated with the object being constructed by the Builder.
         *
         * This field typically holds a user-defined or system-generated name identifying
         * the corresponding entity. It is set using the {@code name(String name)} method
         * during the Builder's configuration phase.
         */
        private String name;

        /**
         * Represents the ARN (Amazon Resource Name) of a resource.
         *
         * This variable typically holds a unique identifier for an AWS resource,
         * such as a bucket or an object, in the form of an ARN string. It is used
         * for identifying and interacting with resources in a structured and
         * consistent manner.
         */
        private String ern;

        /**
         * Sets the name property for the object being constructed by the Builder.
         *
         * @param name the name to associate with the object
         * @return the current Builder instance for method chaining
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the ARN (Amazon Resource Name) for the object being constructed by the Builder.
         *
         * @param ern the ARN to associate with the object
         * @return the current Builder instance for method chaining
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Constructs a new instance of {@code CreateBucketResponse} using the current state
         * of the builder. The builder must have its required fields properly set before calling
         * this method to ensure a valid and complete response object.
         *
         * @return a new {@code CreateBucketResponse} instance containing the name and ERN
         *         properties as configured in the builder.
         */
        public CreateBucketResponse build() {
            return new CreateBucketResponse(name, ern);
        }
    }
}
