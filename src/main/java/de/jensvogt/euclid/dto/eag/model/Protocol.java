package de.jensvogt.euclid.dto.eag.model;

/**
 * What an API gateway listener speaks to the callers that reach it.
 * <p>
 * A listener's protocol is configuration rather than something a route decides, so this only ever
 * comes back from {@link de.jensvogt.euclid.module.eag.EuclidEag#listListeners()}.
 */
public enum Protocol {

    /**
     * Plain HTTP. What a gateway behind a load balancer that has already terminated TLS wants, and
     * what an installation reached only over a private network can live with.
     */
    HTTP,

    /**
     * HTTPS, terminated by the gateway itself with a certificate from the key management module.
     */
    HTTPS;

    /**
     * The wire value, as the {@code eag} module reports it.
     *
     * @return "http" or "https"
     */
    public String wireValue() {
        return name().toLowerCase();
    }

    /**
     * Reads a wire value back, defaulting to {@link #HTTP} for an absent or unrecognized one -
     * which is what the server itself reports for anything that is not "https".
     *
     * @param value the wire value, e.g. "https"
     * @return the protocol it names
     */
    public static Protocol fromWireValue(String value) {
        return HTTPS.wireValue().equalsIgnoreCase(value) ? HTTPS : HTTP;
    }
}
