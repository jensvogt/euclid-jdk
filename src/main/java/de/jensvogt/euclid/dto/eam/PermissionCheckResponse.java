package de.jensvogt.euclid.dto.eam;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * What {@code check-permission} answers: the verdict, and what decided it.
 *
 * <p>The reason is part of the answer rather than a server-side log line because "no" is the
 * useful case to explain - a caller showing a refusal wants to say whether the role was missing,
 * the namespace was wrong, or the resource did not match.
 *
 * @param allowed whether the user would be allowed to do this
 * @param reason  why, in either direction
 * @param role    the role whose grant allowed it, when one did; empty on a refusal
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PermissionCheckResponse(boolean allowed, String reason, String role) {
}
