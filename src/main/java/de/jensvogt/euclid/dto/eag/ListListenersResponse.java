package de.jensvogt.euclid.dto.eag;

import de.jensvogt.euclid.dto.eag.model.Listener;

import java.util.List;

/**
 * Response describing what the API gateway was configured to serve, and whether it is serving it.
 *
 * <p>{@link #serving()} is a property of the gateway rather than of any one listener, which is why
 * it is here as well as on each entry: a configured port whose bind failed, or whose certificate
 * could not be loaded, is still listed - it is the one somebody is looking for - but nothing it
 * says is being served.
 *
 * @param listeners the configured listeners
 * @param total     how many there are, which is {@code listeners.size()}
 * @param serving   whether the gateway's ports are bound at all
 */
public record ListListenersResponse(List<Listener> listeners, long total, boolean serving) {

    /**
     * Creates a new instance of the Builder for constructing a ListListenersResponse object.
     *
     * @return a new Builder instance for constructing ListListenersResponse.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link ListListenersResponse} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The configured listeners.
         */
        private List<Listener> listeners = List.of();

        /**
         * How many there are.
         */
        private long total;

        /**
         * Whether the gateway's ports are bound at all.
         */
        private boolean serving;

        /**
         * Sets the configured listeners.
         *
         * @param listeners the listeners
         * @return the builder instance
         */
        public Builder listeners(List<Listener> listeners) {
            this.listeners = listeners;
            return this;
        }

        /**
         * Sets how many listeners there are.
         *
         * @param total the total count
         * @return the builder instance
         */
        public Builder total(long total) {
            this.total = total;
            return this;
        }

        /**
         * Sets whether the gateway's ports are bound.
         *
         * @param serving whether the gateway is serving
         * @return the builder instance
         */
        public Builder serving(boolean serving) {
            this.serving = serving;
            return this;
        }

        /**
         * Builds and returns a new instance of ListListenersResponse using the properties set on the Builder.
         *
         * @return a new ListListenersResponse instance.
         */
        public ListListenersResponse build() {
            return new ListListenersResponse(listeners, total, serving);
        }
    }
}
