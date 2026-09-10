package de.jensvogt.euclid.dto.eag.model;

/**
 * One port the API gateway was configured to serve, and what it is serving there.
 *
 * <p>An installation serving more than one environment configures a listener per namespace, so that
 * development and integration can both publish {@code /api/produktmeldungen} on their own ports
 * without either being ambiguous. A listener carries the routes of its own namespace, plus every
 * route that names none.
 *
 * <p>This reports what was <em>configured</em>, and {@link #serving()} says separately whether the
 * ports actually came up. A listener whose port was taken, or whose certificate could not be
 * loaded, is still listed - it is the one somebody is looking for - but nothing it says is being
 * served.
 *
 * @param namespace                the namespace this listener serves, or empty for the single
 *                                 unscoped listener an installation that configures none gets
 * @param port                     the port it was configured on
 * @param protocol                 what it speaks
 * @param serving                  whether the gateway's ports are bound at all. A property of the
 *                                 gateway rather than of this listener, and the same on every entry
 * @param certificateName          the certificate an HTTPS listener serves, which is the one it
 *                                 named or the conventional one for its namespace where it named
 *                                 none. Empty for a plain HTTP listener
 * @param configuredCertificate    what the configuration wrote, so that "this listener names its
 *                                 certificate" and "this listener takes the conventional one" can
 *                                 be told apart
 * @param certificate              the certificate itself, or {@code null} where there is none to
 *                                 report - a plain HTTP listener, or an HTTPS one whose certificate
 *                                 was not found, which is what a port that never came up looks like
 */
public record Listener(String namespace, int port, Protocol protocol, boolean serving, String certificateName,
                       String configuredCertificate, ListenerCertificate certificate) {

    /**
     * Whether this listener names its own certificate rather than taking the conventional one for
     * its namespace.
     *
     * @return {@code true} if the configuration named a certificate
     */
    public boolean namesCertificate() {
        return configuredCertificate != null && !configuredCertificate.isEmpty();
    }
}
