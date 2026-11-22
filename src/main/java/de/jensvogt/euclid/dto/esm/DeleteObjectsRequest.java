package de.jensvogt.euclid.dto.esm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request to delete several objects from a bucket in one call: the ones named by key, or
 * everything under a prefix.
 *
 * <p>Keys or a prefix, never both - the server refuses a request naming the two, since they ask
 * two different questions and answering both would delete more than either. And neither is not
 * "nothing": a request with no keys and no prefix empties the whole bucket, which is why
 * {@link de.jensvogt.euclid.module.esm.EuclidEsm#deleteObjects(String, List)} does not send one for
 * an empty key list.
 *
 * @param ern    the ERN (Entity Resource Name) of the bucket the objects are in
 * @param keys   the keys of the objects to delete
 * @param prefix the key prefix everything beneath which is deleted, or null when naming keys
 * @param async  whether the server answers as soon as it has started rather than when it has finished
 * @param notifySubscribers whether the objects' deletion is announced to the bucket's subscribers; null to
 *               announce, as a delete always has. Asking for silence is for an operator who knows
 *               what is listening - a clean-up a downstream consumer would otherwise act on object
 *               by object. Honoured for the prefix form only: deleting by key always announces.
 *               Sent as {@code "notify"}; named longer here because a record cannot have an
 *               accessor called {@code notify()}, which {@link Object} already declares
 */
// prefix and notify are left out when they say nothing, so a delete by key sends exactly what it
// always sent and an older installation is not handed fields it has no meaning for.
public record DeleteObjectsRequest(String ern, List<String> keys,
                                   @JsonInclude(JsonInclude.Include.NON_NULL) String prefix, boolean async,
                                   @JsonProperty("notify") @JsonInclude(JsonInclude.Include.NON_NULL)
                                   Boolean notifySubscribers) {

    /**
     * Creates a new instance of the Builder for constructing a DeleteObjectsRequest object.
     *
     * @return a new Builder instance for constructing DeleteObjectsRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link DeleteObjectsRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The ERN (Entity Resource Name) of the bucket the objects are in.
         */
        private String ern;

        /**
         * The keys of the objects to delete.
         */
        private List<String> keys = List.of();

        /**
         * The key prefix everything beneath which is deleted, or null when naming keys.
         */
        private String prefix;

        /**
         * Whether the server answers as soon as it has started rather than when it has finished.
         */
        private boolean async;

        /**
         * Whether the deletion is announced to the bucket's subscribers; null to announce.
         */
        private Boolean notifySubscribers;

        /**
         * Sets the ERN of the bucket the objects are in.
         *
         * @param ern the ERN of the bucket
         * @return the builder instance
         */
        public Builder ern(String ern) {
            this.ern = ern;
            return this;
        }

        /**
         * Sets the keys of the objects to delete.
         *
         * @param keys the object keys
         * @return the builder instance
         */
        public Builder keys(List<String> keys) {
            this.keys = keys;
            return this;
        }

        /**
         * Sets the key prefix everything beneath which is deleted, instead of naming keys.
         *
         * @param prefix the key prefix, or null when naming keys
         * @return the builder instance
         */
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Sets whether the server answers as soon as it has started.
         *
         * @param async true to have the server work in the background
         * @return the builder instance
         */
        public Builder async(boolean async) {
            this.async = async;
            return this;
        }

        /**
         * Sets whether the deletion is announced to the bucket's subscribers.
         *
         * @param notifySubscribers false to delete silently; null or true to announce
         * @return the builder instance
         */
        public Builder notifySubscribers(Boolean notifySubscribers) {
            this.notifySubscribers = notifySubscribers;
            return this;
        }

        /**
         * Builds and returns a new instance of DeleteObjectsRequest using the properties set on the Builder.
         *
         * @return a new DeleteObjectsRequest instance.
         */
        public DeleteObjectsRequest build() {
            return new DeleteObjectsRequest(ern, keys, prefix, async, notifySubscribers);
        }
    }
}
