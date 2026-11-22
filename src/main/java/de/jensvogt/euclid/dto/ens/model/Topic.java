package de.jensvogt.euclid.dto.ens.model;

import java.util.Map;

/**
 * Mirrors {@code Euclid::Dto::ENS::Topic} from the Euclid server.
 *
 * @param name             the topic's name
 * @param owner            the user ID that owns the topic
 * @param ern              the topic's ERN
 * @param tags             user-defined tags on the topic
 * @param size             total size in bytes of all messages currently in the topic
 * @param messages         number of messages currently in the topic
 * @param maxMessageLength maximum allowed size, in bytes, of a single message
 * @param send             how many messages have ever been published to this topic. A lifetime
 *                         total rather than a current figure: unlike {@code messages} it does not
 *                         fall when the topic is purged or retention removes what it held, which
 *                         makes it the one to read when asking whether a publisher is working at all
 * @param resend           how many messages have ever been handed to the subscriptions again, by
 *                         {@link de.jensvogt.euclid.module.ens.EuclidEns#resendMessages(String)} or
 *                         by a start releasing what a stopped topic held. A lifetime total for the
 *                         same reason as {@code send}; well above zero, it says somebody has had to
 *                         replay this topic
 * @param status           whether the topic hands what is published to it to its subscribers,
 *                         {@code "RUNNING"} or {@code "STOPPED"} - see
 *                         {@link de.jensvogt.euclid.module.ens.EuclidEns#stopTopic(String)}
 * @param retentionPeriod  how long a message published to this topic is kept, in seconds. Zero
 *                         means the topic follows the installation default as that changes, rather
 *                         than having frozen a copy of whatever it was on the day the topic was
 *                         created
 * @param deleteAfterDelivery whether a message is deleted once it has been handed to every
 *                         subscription, rather than kept for the retention period - see
 *                         {@link de.jensvogt.euclid.module.ens.EuclidEns#setTopicDeleteAfterDelivery(String, boolean)}
 * @param created          creation timestamp
 * @param modified         last-modified timestamp
 */
public record Topic(String name, String owner, String ern, Map<String, String> tags, long size, long messages,
                     long maxMessageLength, long send, long resend, String status, long retentionPeriod, boolean deleteAfterDelivery,
                     String created, String modified) {
}
