package de.jensvogt.euclid.dto.eqs;

import java.util.List;

/**
 * What a send-message-batch did.
 * <p>
 * Counts, the ids of what was sent, and the failures named individually. The counts follow the
 * shape every other multi-item action in euclid uses; the failure list does not, and is there
 * because the two cases differ. A delete that skipped a key removed something already gone. A send
 * that skipped a message dropped it, and a producer holding "97 of 100" cannot act on that without
 * knowing which three to send again.
 * <p>
 * {@code asked} always equals {@code sent} plus the size of {@code failed}.
 *
 * @param ern        the queue the messages were sent to, as a full ERN
 * @param asked      how many messages the request carried
 * @param sent       how many were stored
 * @param messageIds the ids of the messages that were sent, in request order
 * @param failed     the messages that were not sent, each against its position in the request
 */
public record SendMessageBatchResponse(String ern, long asked, long sent,
                                       List<String> messageIds, List<Failure> failed) {

    /**
     * One message the batch would not send, and why.
     *
     * @param index  where the message sat in the request's own list - the only thing the two sides
     *               share, since the server minted nothing for a message it did not accept
     * @param reason why it was refused, in the same words a single send would have used
     */
    public record Failure(long index, String reason) {
    }
}
