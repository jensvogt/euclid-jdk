package de.jensvogt.euclid.dto.eqs.model;

import java.util.Map;

/**
 * Mirrors {@code Euclid::Dto::EQS::Queue} from the Euclid server.
 */
public record Queue(String region, String name, String owner, String ern, Map<String, String> tags, long delay,
                     long size, long messages, long delayed, long busy, long visibility, long maxMessageLength,
                     long maxReceiveCount, String deadLetterQueueArn, String created, String modified) {
}
