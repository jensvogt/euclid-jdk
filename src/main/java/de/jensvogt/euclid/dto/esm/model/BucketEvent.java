package de.jensvogt.euclid.dto.esm.model;

/**
 * The notification a bucket subscription delivers: what {@code EuclidEsm.subscribe} arranges to be
 * put into a queue or topic whenever an object lands in the watched bucket.
 * <p>
 * It arrives as the <em>body</em> of an ordinary message, so a consumer receives it with
 * {@code EuclidEqs.receiveMessages} or reads it off a topic and turns the body into one of these
 * with {@code EuclidEsm.parseBucketEvent}. The delivery itself is a normal queue or topic message -
 * nothing about receiving, acknowledging or deleting it is special.
 * <p>
 * This is deliberately narrower than the {@code esm.object.*} events on the event bus, which carry
 * the account, namespace, owner and the user who made the change and can be filtered server-side.
 * A bucket subscription is the routing-based path - "put a note in this queue" - and its
 * notification says only what was written.
 *
 * @param eventType   what happened, in the AWS-compatible spelling this notification uses -
 *                    {@code "esm:ObjectCreated:Put"} for an object being written
 * @param bucketErn   ERN of the bucket the object was written to
 * @param key         the object's key
 * @param ern         the object's ERN
 * @param size        size of the object in bytes
 * @param contentType the object's MIME content type
 * @param md5Sum      MD5 checksum of the object's content, hex-encoded
 */
public record BucketEvent(String eventType, String bucketErn, String key, String ern, long size, String contentType,
                          String md5Sum) {
}
