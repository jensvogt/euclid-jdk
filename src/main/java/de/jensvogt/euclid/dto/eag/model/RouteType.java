package de.jensvogt.euclid.dto.eag.model;

/**
 * What the API gateway does with a request a route matches.
 * <p>
 * Until there was something to upload to, every route did the same thing - read a request and
 * forward it - and the only question was where to. That is {@link #PROXY}, and it is still what
 * almost every route is. {@link #UPLOAD} is the one that does not forward: the gateway is the
 * endpoint, and what it does with the body is write it into a bucket.
 * <p>
 * The distinction is not "which backend" but "who reads the body". A proxied request is buffered
 * whole and handed on; an upload is streamed straight through to ESM in parts and is never held in
 * memory. That is why this is a type on the route rather than another field on the one behaviour -
 * the two cannot share a request path.
 */
public enum RouteType {

    /**
     * Forward the request to an application or to a euclid module. What a route is unless it says
     * otherwise, and what every route written before uploads existed means.
     */
    PROXY,

    /**
     * Terminate the request at the gateway and stream its body into a bucket - see
     * {@link RouteUpload}.
     */
    UPLOAD,

    /**
     * A stored value this client does not recognize. Only ever read back, never sent - the server
     * refuses it with HTTP 400 rather than guessing which of the two was meant.
     */
    UNKNOWN;

    /**
     * The wire value, as the {@code eag} module reads and reports it.
     *
     * @return "PROXY", "UPLOAD" or "UNKNOWN"
     */
    public String wireValue() {
        return name();
    }

    /**
     * Reads a wire value back, case-insensitively.
     * <p>
     * An absent value reads as {@link #PROXY}, since that is what a route stored before the type
     * existed is; anything present but unrecognized reads as {@link #UNKNOWN} rather than being
     * passed off as a proxy route, which would hide that the gateway is doing something this client
     * does not understand.
     *
     * @param value the wire value, e.g. "UPLOAD"
     * @return the type it names
     */
    public static RouteType fromWireValue(String value) {
        if (value == null || value.isEmpty()) {
            return PROXY;
        }
        for (RouteType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
