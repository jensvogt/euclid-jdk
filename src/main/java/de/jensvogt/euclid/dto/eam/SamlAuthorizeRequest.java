package de.jensvogt.euclid.dto.eam;

/**
 * Asks EAM to start a SAML 2.0 login and say where to send the browser.
 * <p>
 * Unlike {@link OidcAuthorizeRequest} there is no redirect URI: the identity provider posts its
 * assertion to the server's own assertion consumer service, which is registered with the provider
 * once, and only afterwards does the server hand the finished session on. So nothing about the
 * caller ever has to be registered with the provider.
 *
 * @param returnTo where the server should deliver the session once the assertion has been accepted,
 *                 or empty to have the assertion consumer answer with the login response itself.
 *                 A loopback address ({@code http://127.0.0.1:<port>/...}) is a native client
 *                 waiting on its own port and gets the session in a self-submitting form post, so
 *                 the token never sits in a URL; anywhere else is a front end and gets it in the
 *                 redirect's fragment. Only destinations the server permits
 *                 ({@code saml.return-to-prefixes}) are accepted
 */
public record SamlAuthorizeRequest(String returnTo) {

    /**
     * Creates a new instance of the Builder for constructing a SamlAuthorizeRequest object.
     *
     * @return a new Builder instance for constructing SamlAuthorizeRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link SamlAuthorizeRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * Where the server delivers the session once the assertion has been accepted.
         */
        private String returnTo = "";

        /**
         * Sets where the server should deliver the session once the assertion has been accepted.
         *
         * @param returnTo the destination, or empty for the login response itself
         * @return the builder instance
         */
        public Builder returnTo(String returnTo) {
            this.returnTo = returnTo;
            return this;
        }

        /**
         * Builds and returns a new instance of SamlAuthorizeRequest using the properties set on the Builder.
         *
         * @return a new SamlAuthorizeRequest instance.
         */
        public SamlAuthorizeRequest build() {
            return new SamlAuthorizeRequest(returnTo);
        }
    }
}
