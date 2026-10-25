package de.jensvogt.euclid.dto.esm;

/**
 * The answer to stop-bucket and start-bucket.
 *
 * @param ern      the bucket's ERN
 * @param name     the bucket's name
 * @param stopped  whether the bucket's events are held now
 * @param held     after a stop, how many deliveries are held for the bucket; 0 after a start
 * @param released after a start, how many held events were let go; 0 after a stop
 */
public record BucketStoppedResponse(String ern, String name, boolean stopped, long held, long released) {
}
