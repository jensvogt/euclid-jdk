package de.jensvogt.euclid.dto.eqs.model;

import java.util.Map;

/**
 * Mirrors {@code Euclid::Dto::EQS::Queue} from the Euclid server.
 *
 * @param region             the region the queue lives in
 * @param name               the queue's name
 * @param owner              the user ID that owns the queue
 * @param ern                the queue's ERN
 * @param tags               user-defined tags on the queue
 * @param delay              default delivery delay, in seconds, applied to new messages
 * @param size               total size in bytes of all messages currently in the queue
 * @param messages           number of messages currently in the queue
 * @param delayed            number of messages currently delayed
 * @param busy               number of messages currently invisible (being processed)
 * @param visibility         default visibility timeout, in seconds, for received messages
 * @param maxMessageLength   maximum allowed size, in bytes, of a single message
 * @param maxReceiveCount    maximum number of receives before a message moves to the dead-letter queue
 * @param deadLetterQueueArn ERN of the dead-letter queue, or {@code null} if none is configured
 * @param created            creation timestamp
 * @param modified           last-modified timestamp
 */
public record Queue(String region, String name, String owner, String ern, Map<String, String> tags, long delay,
                     long size, long messages, long delayed, long busy, long visibility, long maxMessageLength,
                     long maxReceiveCount, String deadLetterQueueArn, String created, String modified) {
}
