package de.jensvogt.euclid.dto.eqs;

/**
 * Request to move messages out of a dead letter queue and back onto the queues they came from.
 *
 * <p>{@code ern} has to name a queue some other queue points at as its dead letter queue; an
 * ordinary queue is refused rather than redriven into itself. {@code targetErn} is optional and,
 * when given, has to be one of the queues that feed it - anything else would be a move rather than
 * a redrive. Left empty, every message goes back where it came from.
 *
 * @param ern       the ERN (Entity Resource Name) of the dead letter queue to drain
 * @param targetErn the ERN of the single queue to move every message to, or {@code null}/empty to
 *                  return each message to the queue it originally came from
 */
public record RedriveDlqRequest(String ern, String targetErn) {

    /**
     * Creates a new instance of the Builder for constructing a RedriveDlqRequest object.
     *
     * @return a new Builder instance for constructing RedriveDlqRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link RedriveDlqRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN (Entity Resource Name) of the dead letter queue to drain.
         */
        private String ern;

        /**
         * The ERN of the single queue to move every message to, empty to return each message to
         * the queue it came from.
         */
        private String targetErn = "";

        /**
         * Sets the ERN of the dead letter queue to drain.
         *
         * @param ern the ERN of the dead letter queue
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the ERN of the single queue to move every message to.
         *
         * @param targetErn the ERN of the target queue, or {@code null}/empty for per-message origin
         * @return the builder instance
         */
        public Builder targetErn(String targetErn) {
            this.targetErn = targetErn;
            return this;
        }

        /**
         * Builds and returns a new instance of RedriveDlqRequest using the properties set on the Builder.
         *
         * @return a new RedriveDlqRequest instance populated with the ERN and target ERN values.
         */
        public RedriveDlqRequest build() {
            return new RedriveDlqRequest(ern, targetErn);
        }
    }
}
