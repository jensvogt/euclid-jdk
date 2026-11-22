package de.jensvogt.euclid.dto.eam;

/**
 * Asks EAM to start an OpenID Connect login and say where to send the browser.
 * <p>
 * Nothing in here identifies anybody: the identity provider decides who is signing in, which is
 * the point of federating the login. What this says is only where the two legs of the flow should
 * end up.
 *
 * @param redirectUri where the provider should send the browser back to with the authorization
 *                    code, or empty for the one the server has configured ({@code oidc.redirect-uri}).
 *                    A native client names its own loopback listener here - see
 *                    {@link de.jensvogt.euclid.module.eam.EuclidEam#loginWithOidc()} - and the
 *                    provider must accept it, usually as a wildcard loopback URI. Whatever is used
 *                    is sealed into the state and presented again when the code is redeemed, since
 *                    the provider requires the two to match
 * @param returnTo    where the server should send the browser once the login has succeeded, for a
 *                    flow that ends in a browser rather than in a caller collecting the response;
 *                    empty means the caller redeems the code itself with {@code oidc-login}. Only
 *                    destinations the server permits ({@code oidc.return-to-prefixes}) are accepted,
 *                    because the token travels in that redirect's fragment
 */
public record OidcAuthorizeRequest(String redirectUri, String returnTo) {

    /**
     * Creates a new instance of the Builder for constructing an OidcAuthorizeRequest object.
     *
     * @return a new Builder instance for constructing OidcAuthorizeRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link OidcAuthorizeRequest} instances. Both fields default to empty,
     * which is the server's configured callback and no browser hand-off.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Where the provider sends the browser back to.
         */
        private String redirectUri = "";

        /**
         * Where the server sends the browser once the login has succeeded.
         */
        private String returnTo = "";

        /**
         * Sets where the provider should send the browser back to with the code.
         *
         * @param redirectUri the redirect URI, or empty for the server's configured one
         * @return the builder instance
         */
        public Builder redirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
            return this;
        }

        /**
         * Sets where the server should send the browser once the login has succeeded.
         *
         * @param returnTo the destination, or empty to redeem the code yourself
         * @return the builder instance
         */
        public Builder returnTo(String returnTo) {
            this.returnTo = returnTo;
            return this;
        }

        /**
         * Builds and returns a new instance of OidcAuthorizeRequest using the properties set on the Builder.
         *
         * @return a new OidcAuthorizeRequest instance.
         */
        public OidcAuthorizeRequest build() {
            return new OidcAuthorizeRequest(redirectUri, returnTo);
        }
    }
}
