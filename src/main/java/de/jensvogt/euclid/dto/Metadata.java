package de.jensvogt.euclid.dto;

/**
 * Mirrors {@code Euclid::Dto::BaseDto} from the Euclid server: the caller's identity, resolved
 * server-side from the authenticated request and returned as a nested {@code "metadata"} object so
 * a response DTO's own fields stay at the top level of the JSON.
 * <p>
 * Only ever on responses. The server resolves the caller from the bearer token rather than trusting
 * client-supplied values, so no request carries it. The request ID that correlates a request with
 * its response travels as the {@code x-euclid-request-id} response header rather than in here,
 * since - unlike region/account/user - it isn't part of the caller's identity.
 *
 * @param region    the region the request and response apply to
 * @param accountId the account the caller belongs to
 * @param user      the ID of the user the request was made on behalf of
 */
public record Metadata(String region, String accountId, String user) {
}
