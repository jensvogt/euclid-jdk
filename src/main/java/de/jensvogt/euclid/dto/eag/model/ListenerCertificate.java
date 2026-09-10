package de.jensvogt.euclid.dto.eag.model;

import java.util.List;

/**
 * The EKM certificate an HTTPS listener serves, as {@code list-listeners} reports it.
 *
 * <p>Only present when the listener speaks {@link Protocol#HTTPS} and the certificate was found. A
 * plain HTTP port has none to be missing, and reporting one as absent would read as a fault rather
 * than a setting - so {@link Listener#certificate()} is {@code null} in both cases and
 * {@link Listener#certificateName()} says which name was looked for.
 *
 * @param ern              the certificate's ERN
 * @param subject          the certificate's subject
 * @param issuer           the certificate's issuer
 * @param serialNumber     the certificate's serial number
 * @param fingerprint      the certificate's fingerprint
 * @param subjectAltNames  the subject alternative names the certificate is valid for
 * @param generated        whether euclid minted this itself because the listener needed something
 *                         to start with. Callers reject a self-signed certificate until they are
 *                         given it, so this decides whether the port actually works for anybody who
 *                         has not been told about it
 * @param notBefore        ISO8601 timestamp the certificate becomes valid
 * @param notAfter         ISO8601 timestamp the certificate stops being valid
 * @param expired          whether {@code notAfter} is already past, as the server saw the clock
 */
public record ListenerCertificate(String ern, String subject, String issuer, String serialNumber, String fingerprint,
                                  List<String> subjectAltNames, boolean generated, String notBefore, String notAfter,
                                  boolean expired) {
}
