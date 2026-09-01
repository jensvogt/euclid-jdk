package de.jensvogt.euclid.dto.eqs;

import de.jensvogt.euclid.dto.eqs.model.Queue;
import java.util.List;

/**
 * The queues a list-queues action returned, along with how many exist in total.
 *
 * @param queues the queues on this page
 * @param total  the total number of queues matching the request
 */
public record ListQueueResponse(List<Queue> queues, long total) {

    /**
     * Creates a new instance of the Builder for constructing a ListQueueResponse object.
     *
     * @return a new Builder instance for constructing ListQueueResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListQueueResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The queues on this page.
         */
        private List<Queue> queues;

        /**
         * The total number of queues matching the request.
         */
        private long total = 0;

        /**
         * Sets the queues on this page.
         *
         * @param queues the queues on this page
         * @return the builder instance
         */
        public Builder queues(List<Queue> queues) {
            this.queues = queues;
            return this;
        }

        /**
         * Sets the total number of queues matching the request.
         *
         * @param total the total number of queues matching the request
         * @return the builder instance
         */
        public Builder total(long total) {
            this.total = total;
            return this;
        }

        /**
         * Builds and returns a new instance of ListQueueResponse using the properties set on the Builder.
         *
         * @return a new ListQueueResponse instance.
         */
        public ListQueueResponse build() {
            return new ListQueueResponse(queues, total);
        }
    }
}
