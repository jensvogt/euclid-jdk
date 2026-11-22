package de.jensvogt.euclid.dto.eam;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * What {@code saml-authorize} answers: where to send the person to authenticate with the SAML
 * identity provider.
 *
 * @param authenticationUrl the provider's single sign-on URL, with the deflated, encoded
 *                          AuthnRequest and the RelayState already on it. Open it in a browser;
 *                          nothing else needs to be added
 * @param relayState        the opaque RelayState this attempt travels under, also present in
 *                          {@code authenticationUrl}. Returned separately so a caller can tell
 *                          whether what comes back belongs to the login it started. It carries the
 *                          request ID the assertion has to answer, sealed - there is nothing in it
 *                          to read
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SamlAuthorizeResponse(String authenticationUrl, String relayState) {
}
