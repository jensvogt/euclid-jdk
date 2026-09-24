package de.jensvogt.euclid.dto.esm;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a request to create a new bucket with a specified name.
 * This class is immutable and provides a builder to simplify the construction process.
 *
 * Fields:
 * - name: The name of the bucket to be created.
 *
 * Provides functionality to construct a {@code CreateBucketRequest} instance using
 * the static {@code builder()} method, which returns a {@code Builder}.
 *
 * The {@code Builder} class simplifies the construction of {@code CreateBucketRequest}
 * instances by allowing the caller to specify the required parameters in a fluent manner.
 *
 * Example usage of the builder pattern is encouraged for creating instances of this
 * class to ensure clarity and immutability.
 *
 * @param name     The name of the bucket to be created.
 * @param priority The priority the notifications this bucket sends are given, or null for none. The
 *                 bucket does nothing with it - see
 *                 {@link de.jensvogt.euclid.module.esm.EuclidEsm#setBucketPriority}.
 */
// Nulls are left out rather than sent, so a create with no priority says exactly what a create has
// always said - and an older installation is not handed a field it has no meaning for. Scoped to this
// record rather than set on the module's ObjectMapper, which every other ESM request shares.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateBucketRequest(String name, String priority) {

    /**
     * Provides a static method to obtain a new instance of the Builder.
     * The Builder simplifies the creation of an instance of this record
     * using a fluent API.
     *
     * @return a new instance of the Builder for constructing an instance of the containing record
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder class for constructing instances of {@code CreateBucketRequest}.
     * This class provides a fluent API to configure and create immutable
     * {@code CreateBucketRequest} objects.
     *
     * The builder allows setting a single parameter:
     * - {@code name}: The name of the bucket to be created.
     *
     * Instances of the builder can be created using the {@code builder()} method
     * of the enclosing {@code CreateBucketRequest} class.
     *
     * The {@code build()} method finalizes the configuration and returns an
     * immutable {@code CreateBucketRequest} instance.
     */
    public static final class Builder {
        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The name of the bucket to be created.
         * This field is mandatory and is used to specify the unique name
         * for the bucket that will be created using the {@code CreateBucketRequest.Builder}.
         */
        private String name;

        /**
         * The priority the notifications this bucket sends are given, or null for none.
         */
        private String priority;

        /**
         * Sets the name of the bucket to be created.
         *
         * @param name the name of the bucket
         * @return the updated builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the priority the notifications this bucket sends are given.
         *
         * @param priority {@code "LOW"}, {@code "MEDIUM"} or {@code "HIGH"}, or null for none
         * @return the updated builder instance
         */
        public Builder priority(String priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Constructs and returns a new instance of {@code CreateBucketRequest}
         * based on the current state of the builder.
         *
         * @return a new immutable {@code CreateBucketRequest} instance containing
         *         the configured bucket name
         */
        public CreateBucketRequest build() {
            return new CreateBucketRequest(name, priority);
        }
    }
}
