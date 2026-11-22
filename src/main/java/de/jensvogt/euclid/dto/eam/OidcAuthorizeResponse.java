package de.jensvogt.euclid.dto.eam;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * What {@code oidc-authorize} answers: where to send the person to authenticate, and the state
 * that belongs to that particular attempt.
 *
 * @param authorizationUrl the provider's authorization endpoint with every parameter already on
 *                         it. Open it in a browser; nothing else needs to be added
 * @param state            the opaque state this attempt travels under, also present in
 *                         {@code authorizationUrl}. Returned separately so a caller can check that
 *                         the callback it receives belongs to the authorization it started. It
 *                         carries the flow's nonce and PKCE verifier, sealed by the server - there
 *                         is nothing in it to read, and it is the flow's only memory, so the
 *                         {@code oidc-login} that redeems the code has to present it unchanged
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OidcAuthorizeResponse(String authorizationUrl, String state) {
}
