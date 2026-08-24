package de.jensvogt.euclid.dto.eqs;

/**
 * Response returned after successfully creating a queue.
 *
 * @param name the name of the created queue
 * @param ern  the ARN (Amazon Resource Name) uniquely identifying the created queue
 */
public record CreateQueueResponse(String name, String ern) {

    /**
     * Creates a new instance of the Builder for constructing a CreateQueueResponse object.
     *
     * @return a new Builder instance for constructing CreateQueueResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link CreateQueueResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Specifies the name of the queue to be created.
         * This value is assigned during the builder construction process.
         */
        private String name;

        /**
         * The ARN (Amazon Resource Name) of the queue. This uniquely identifies the queue
         * within the AWS environment and is used to perform actions or operations on the queue.
         */
        private String ern;

        /**
         * Sets the name of the queue.
         *
         * @param name the name of the queue
         * @return the builder instance
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the ARN (Amazon Resource Name) of the queue.
         *
         * @param ern the ARN of the queue, which uniquely identifies the queue within the AWS environment
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Builds and returns a new instance of CreateQueueResponse using the properties set on the Builder.
         *
         * @return a new CreateQueueResponse instance populated with the name and ARN (Amazon Resource Name) values.
         */
        public CreateQueueResponse build() {
            return new CreateQueueResponse(name, ern);
        }
    }
}
