package de.jensvogt.euclid.dto.eqs.model;

import java.util.Map;

/**
 * Mirrors {@code Euclid::Dto::EQS::Message} from the Euclid server.
 *
 * @param ern            the message's ERN
 * @param queueErn       ERN of the queue the message belongs to
 * @param messageId      the message's ID
 * @param status         current message status
 * @param priority       message priority
 * @param body           the message body
 * @param md5Body        MD5 checksum of {@code body}
 * @param receiptHandle  handle to use for deleting or changing the visibility of this message
 * @param attributes     typed, user-defined message attributes
 * @param md5Attributes  MD5 checksum of {@code attributes}
 * @param lastReceived   timestamp the message was last received, or {@code null} if never received
 * @param created        creation timestamp
 * @param modified       last-modified timestamp
 */
public record Message(String ern, String queueErn, String messageId, String status, String priority, String body,
                       String md5Body, String receiptHandle, Map<String, Variant> attributes, String md5Attributes,
                       String lastReceived, String created, String modified) {
}
