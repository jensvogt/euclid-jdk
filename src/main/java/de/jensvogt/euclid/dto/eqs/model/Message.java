package de.jensvogt.euclid.dto.eqs.model;

import java.util.Map;

/**
 * Mirrors {@code Euclid::Dto::EQS::Message} from the Euclid server.
 */
public record Message(String ern, String queueErn, String messageId, String status, String priority, String body,
                       String md5Body, String receiptHandle, Map<String, Variant> attributes, String md5Attributes,
                       String lastReceived, String created, String modified) {
}
