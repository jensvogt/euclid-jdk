package de.jensvogt.euclid.dto.eag.model;

/**
 * What a route requires of a caller before the API gateway forwards a request.
 * <p>
 * A property of the resource rather than of the gateway, because the two kinds of resource sit
 * behind the same listener: a public read a browser makes without credentials, and an operation
 * only a euclid principal may perform. Deciding it per route is what lets one application serve
 * both.
 */
public enum RouteAuthentication {

    /**
     * Proxied as it arrives. Whatever the application requires, it enforces itself.
     */
    NONE,

    /**
     * A euclid credential is required and verified before anything is forwarded - whatever the
     * euclid gateway accepts, which today is a bearer token, an RFC 9421 message signature or
     * SigV4.
     */
    EUCLID,

    /**
     * HTTP Basic authentication against a euclid user's password, for the callers a euclid
     * credential does not suit: a browser, which prompts for a username and password when it is
     * answered with {@code WWW-Authenticate}, and a script with nothing but curl. A technical
     * principal is refused here as it is at login.
     */
    BASIC,

    /**
     * A stored value this client does not recognize. Only ever read back, never sent - the server
     * refuses it with HTTP 400, which is the point: a caller who asked for {@link #EUCLID} and
     * silently got {@link #NONE} would have been handed a public route they believe is protected.
     */
    UNKNOWN;

    /**
     * The wire value, as the {@code eag} module reads and reports it.
     *
     * @return "NONE", "EUCLID", "BASIC" or "UNKNOWN"
     */
    public String wireValue() {
        return name();
    }

    /**
     * Reads a wire value back, case-insensitively.
     * <p>
     * Answers {@link #UNKNOWN} for anything unrecognized rather than falling back to {@link #NONE}:
     * reporting a route as public when the server may think otherwise is the one mistake here that
     * would not announce itself.
     *
     * @param value the wire value, e.g. "EUCLID"
     * @return the kind it names, or {@link #UNKNOWN} if it names none
     */
    public static RouteAuthentication fromWireValue(String value) {
        if (value == null || value.isEmpty()) {
            return UNKNOWN;
        }
        for (RouteAuthentication authentication : values()) {
            if (authentication.name().equalsIgnoreCase(value)) {
                return authentication;
            }
        }
        return UNKNOWN;
    }
}
