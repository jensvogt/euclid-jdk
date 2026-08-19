package de.jensvogt.euclid.dto.sqs.model;

import java.util.Map;

/**
 * Mirrors {@code Euclid::Dto::SQS::Message} from the Euclid server.
 */
public record Message(String ern, String body, String md5Body, String receiptHandle,
                       Map<String, Variant> attributes, String lastReceived, String created, String modified) {
}
