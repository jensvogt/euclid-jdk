package de.jensvogt.euclid.dto.eam;

/**
 * Hands EAM what came back from the OpenID Connect provider, to be turned into a euclid session.
 * <p>
 * Answered with the same {@link LoginResponse} a password login produces - the token, the access
 * key and the admin flag - because from there on a federated session is not a different kind of
 * session.
 *
 * @param code  the authorization code from the provider's callback
 * @param state the state from the provider's callback, exactly as it arrived. Not merely checked
 *              against the one that was issued: it <em>is</em> the flow's memory - the nonce and
 *              the PKCE verifier are sealed inside it - so a callback without it cannot be
 *              completed by any server instance, and one that has been altered is refused
 */
public record OidcLoginRequest(String code, String state) {

    /**
     * Creates a new instance of the Builder for constructing an OidcLoginRequest object.
     *
     * @return a new Builder instance for constructing OidcLoginRequest.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link OidcLoginRequest} instances.
     */
    public static final class Builder {

        /**
         * Creates an empty builder.
         */
        public Builder() {
        }

        /**
         * The authorization code from the provider's callback.
         */
        private String code;

        /**
         * The state from the provider's callback.
         */
        private String state;

        /**
         * Sets the authorization code from the provider's callback.
         *
         * @param code the authorization code
         * @return the builder instance
         */
        public Builder code(String code) {
            this.code = code;
            return this;
        }

        /**
         * Sets the state from the provider's callback, exactly as it arrived.
         *
         * @param state the state
         * @return the builder instance
         */
        public Builder state(String state) {
            this.state = state;
            return this;
        }

        /**
         * Builds and returns a new instance of OidcLoginRequest using the properties set on the Builder.
         *
         * @return a new OidcLoginRequest instance.
         */
        public OidcLoginRequest build() {
            return new OidcLoginRequest(code, state);
        }
    }
}
