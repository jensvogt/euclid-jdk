package de.jensvogt.euclid.dto.eqs;

import de.jensvogt.euclid.dto.eqs.model.Queue;

/**
 * One queue, as a listing describes each of its own.
 * <p>
 * The same {@link Queue} a listing answer carries, rather than a shape of its own, so that what a
 * listing shows and what this shows cannot drift apart.
 *
 * @param queue the queue
 */
public record GetQueueResponse(Queue queue) {

    /**
     * Creates a new instance of the Builder for constructing a GetQueueResponse object.
     *
     * @return a new Builder instance for constructing GetQueueResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link GetQueueResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The queue.
         */
        private Queue queue;

        /**
         * Sets the queue.
         *
         * @param queue the queue
         * @return the builder instance
         */
        public Builder queue(Queue queue) {
            this.queue = queue;
            return this;
        }

        /**
         * Builds and returns a new instance of GetQueueResponse using the properties set on the Builder.
         *
         * @return a new GetQueueResponse instance.
         */
        public GetQueueResponse build() {
            return new GetQueueResponse(queue);
        }
    }
}
