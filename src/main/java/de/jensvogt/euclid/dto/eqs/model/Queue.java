package de.jensvogt.euclid.dto.eqs.model;

import java.util.Map;

/**
 * Mirrors {@code Euclid::Dto::EQS::Queue} from the Euclid server.
 *
 * @param name               the queue's name
 * @param owner              the user ID that owns the queue
 * @param ern                the queue's ERN
 * @param tags               user-defined tags on the queue
 * @param size               total size in bytes of all messages currently in the queue
 * @param delay              default delivery delay, in seconds, applied to new messages
 * @param available          number of messages currently available for receipt
 * @param delayed            number of messages currently delayed
 * @param invisible          number of messages currently invisible (being processed)
 * @param visibility         default visibility timeout, in seconds, for received messages
 * @param maxMessageLength   maximum allowed size, in bytes, of a single message
 * @param maxReceiveCount    maximum number of receives before a message moves to the dead-letter queue
 * @param deadLetterQueueArn ERN of the dead-letter queue, or {@code null} if none is configured
 * @param priority           default priority applied to the queue's messages
 * @param created            creation timestamp
 * @param modified           last-modified timestamp
 */
public record Queue(String name, String owner, String ern, Map<String, String> tags, long size, long delay,
                    long available, long delayed, long invisible, long visibility, long maxMessageLength,
                    long maxReceiveCount, String deadLetterQueueArn, String priority, String created,
                    String modified) {
}
