package de.jensvogt.euclid.dto.ens;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * What a resend handed over, and what it passed by.
 *
 * @param ern    the ERN of the topic
 * @param resent how many messages went to the topic's subscriptions again
 * @param held   how many were passed over as never having been delivered at all - published while
 *               the topic was stopped, and {@code startTopic} is what releases those. A non-zero
 *               value here means there is a backlog that a resend is not the way to clear
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ResendMessagesResponse(String ern, long resent, long held) {
}
