package de.jensvogt.euclid.dto.eqs.model;

import de.jensvogt.euclid.dto.com.Variant;

import java.util.Map;

/**
 * Mirrors {@code Euclid::Dto::EQS::Message} from the Euclid server.
 *
 * @param ern           the message's ERN
 * @param queueErn      ERN of the queue the message belongs to
 * @param messageId     the message's ID
 * @param status        current message status
 * @param priority      the message priority
 * @param body          the message body
 * @param receiptHandle receipt handle from the last receive, empty until the message is received
 * @param size          size of the message body in bytes
 * @param contentType   the message body's content type
 * @param attributes    user-defined message attributes, keyed by name
 * @param lastReceived  timestamp of the last receive, or {@code null} if never received
 * @param created       creation timestamp
 * @param modified      last-modified timestamp
 */
public record Message(String ern, String queueErn, String messageId, String status, String priority, String body,
                      String receiptHandle, long size, String contentType, Map<String, Variant> attributes,
                      String lastReceived, String created, String modified) {
}
