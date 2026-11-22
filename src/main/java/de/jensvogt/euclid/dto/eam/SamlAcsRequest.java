package de.jensvogt.euclid.dto.eam;

/**
 * A signed SAML response, handed to EAM's assertion consumer service ({@code saml-acs}) as JSON.
 * <p>
 * The browser binding delivers the same two values as an HTML form post, straight from the
 * identity provider; this is the shape for a program that obtained the assertion some other way -
 * from the provider's API, as euclid-cli's {@code eam login --onelogin} does - and wants it turned
 * into a session. Either way it is verified identically: the provider's signature is what is
 * trusted, not how the assertion arrived.
 *
 * @param samlResponse the base64-encoded SAML response, as the provider issued it
 * @param relayState   the RelayState that came with it, or empty for an assertion that answers no
 *                     login request euclid made. Such an unsolicited assertion is refused unless the
 *                     server allows IdP-initiated logins ({@code saml.allow-idp-initiated})
 */
public record SamlAcsRequest(String samlResponse, String relayState) {

    /**
     * Creates a new instance of the Builder for constructing a SamlAcsRequest object.
     *
     * @return a new Builder instance for constructing SamlAcsRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SamlAcsRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The base64-encoded SAML response.
         */
        private String samlResponse;

        /**
         * The RelayState that came with it.
         */
        private String relayState = "";

        /**
         * Sets the base64-encoded SAML response.
         *
         * @param samlResponse the SAML response
         * @return the builder instance
         */
        public Builder samlResponse(String samlResponse) {
            this.samlResponse = samlResponse;
            return this;
        }

        /**
         * Sets the RelayState that came with the response.
         *
         * @param relayState the RelayState, or empty for an unsolicited assertion
         * @return the builder instance
         */
        public Builder relayState(String relayState) {
            this.relayState = relayState;
            return this;
        }

        /**
         * Builds and returns a new instance of SamlAcsRequest using the properties set on the Builder.
         *
         * @return a new SamlAcsRequest instance.
         */
        public SamlAcsRequest build() {
            return new SamlAcsRequest(samlResponse, relayState);
        }
    }
}
