package de.jensvogt.euclid.dto.ens;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * What a resend handed over, and what it passed by.
 *
 * <p>When the request asked for it to run in the background, {@code async} is true and the server
 * answered before doing any of it: {@code messages} is how many the topic held when the resend
 * started, and {@code resent} and {@code held} are both zero because nothing had happened yet.
 * The module log carries the finished figures.
 *
 * @param ern      the ERN of the topic
 * @param resent   how many messages went to the topic's subscriptions again, zero when {@code async}
 * @param held     how many were passed over as never having been delivered at all - published while
 *                 the topic was stopped, and {@code startTopic} is what releases those. A non-zero
 *                 value here means there is a backlog that a resend is not the way to clear
 * @param messages how many the topic held when a background resend started, zero otherwise
 * @param async    whether the server is still working through them in the background
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ResendMessagesResponse(String ern, long resent, long held, long messages, boolean async) {
}
