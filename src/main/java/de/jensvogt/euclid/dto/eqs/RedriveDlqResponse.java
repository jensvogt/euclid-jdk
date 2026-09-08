package de.jensvogt.euclid.dto.eqs;

import de.jensvogt.euclid.dto.eqs.model.RedriveTarget;

import java.util.List;

/**
 * Response returned after a dead letter queue has been redriven.
 *
 * <p>{@code remaining} being greater than zero is not a failure: a message with no recorded origin
 * is left where it is rather than guessed at, and {@code note} says so. Name a target queue to move
 * those deliberately.
 *
 * @param ern       the ERN (Entity Resource Name) of the dead letter queue that was drained
 * @param messages  the total number of messages moved
 * @param remaining the number of messages still on the dead letter queue afterwards
 * @param targets   one entry per queue messages were moved to, with the count for each
 * @param note      an explanation of why messages remain, or {@code null} when none do
 */
public record RedriveDlqResponse(String ern, long messages, long remaining, List<RedriveTarget> targets, String note) {

    /**
     * Creates a new instance of the Builder for constructing a RedriveDlqResponse object.
     *
     * @return a new Builder instance for constructing RedriveDlqResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link RedriveDlqResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN (Entity Resource Name) of the dead letter queue that was drained.
         */
        private String ern;

        /**
         * The total number of messages moved.
         */
        private long messages;

        /**
         * The number of messages still on the dead letter queue afterwards.
         */
        private long remaining;

        /**
         * One entry per queue messages were moved to.
         */
        private List<RedriveTarget> targets = List.of();

        /**
         * An explanation of why messages remain, null when none do.
         */
        private String note;

        /**
         * Sets the ERN of the dead letter queue that was drained.
         *
         * @param ern the ERN of the dead letter queue
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the total number of messages moved.
         *
         * @param messages the number of messages moved
         * @return the builder instance
         */
        public Builder messages(long messages) {
            this.messages = messages;
            return this;
        }

        /**
         * Sets the number of messages still on the dead letter queue afterwards.
         *
         * @param remaining the number of messages left behind
         * @return the builder instance
         */
        public Builder remaining(long remaining) {
            this.remaining = remaining;
            return this;
        }

        /**
         * Sets the per-queue breakdown of where the messages went.
         *
         * @param targets one entry per queue messages were moved to
         * @return the builder instance
         */
        public Builder targets(List<RedriveTarget> targets) {
            this.targets = targets;
            return this;
        }

        /**
         * Sets the explanation of why messages remain.
         *
         * @param note the explanation, or null when no messages remain
         * @return the builder instance
         */
        public Builder note(String note) {
            this.note = note;
            return this;
        }

        /**
         * Builds and returns a new instance of RedriveDlqResponse using the properties set on the Builder.
         *
         * @return a new RedriveDlqResponse instance.
         */
        public RedriveDlqResponse build() {
            return new RedriveDlqResponse(ern, messages, remaining, targets, note);
        }
    }
}
