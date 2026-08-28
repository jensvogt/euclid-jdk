package de.jensvogt.euclid.dto.ens.model;

import de.jensvogt.euclid.dto.eqs.model.Variant;

import java.util.Map;

/**
 * Mirrors {@code Euclid::Dto::ENS::Message} from the Euclid server. Message attributes use the
 * same typed {@link Variant} shape as EQS, since both mirror the same shared
 * {@code Euclid::Dto::COM::Variant} type server-side.
 *
 * @param ern           the message's ERN
 * @param topicErn      ERN of the topic the message belongs to
 * @param messageId     the message's ID
 * @param status        current message status, e.g. "AVAILABLE", "DELAYED", "INVISIBLE" or "UNKNOWN"
 * @param body          the message body
 * @param md5Body       MD5 checksum of {@code body}
 * @param attributes    typed, user-defined message attributes
 * @param md5Attributes MD5 checksum of {@code attributes}
 * @param lastReceived  timestamp the message was last received, or {@code null} if never received
 * @param created       creation timestamp
 * @param modified      last-modified timestamp
 */
public record Message(String ern, String topicErn, String messageId, String status, String body, String md5Body,
                       Map<String, Variant> attributes, String md5Attributes, String lastReceived, String created,
                       String modified) {
}
