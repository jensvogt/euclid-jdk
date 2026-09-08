package de.jensvogt.euclid.dto.ekm.model;

import java.util.List;
import java.util.Map;

/**
 * Mirrors {@code Euclid::Dto::EKM::Certificate} from the Euclid server: an X.509 certificate EKM
 * stores, either imported or generated.
 *
 * <p>The private key is never part of this. EKM holds it, and imports it alongside the certificate,
 * but no action hands it back - a certificate is read by whoever serves it, and the key that proves
 * it stays where it was put. Which is why {@link #certificate()} carries a PEM and there is no
 * matching field for the other half.
 *
 * @param name            the certificate's name, which get-certificate and delete-certificate take
 * @param ern             the certificate's ERN
 * @param description     what the certificate is for, or {@code null} if none was given
 * @param certificate     the PEM-encoded X.509 certificate
 * @param subject         the certificate's subject distinguished name
 * @param issuer          the issuer's distinguished name, which for a generated certificate is the
 *                        subject again
 * @param serialNumber    the certificate's serial number
 * @param fingerprint     the certificate's fingerprint
 * @param subjectAltNames the subject alternative names the certificate is valid for
 * @param generated       whether EKM generated this certificate itself, in which case it is
 *                        self-signed and nobody has vouched for it - a client still has to be told
 *                        to trust it
 * @param notBefore       ISO8601 timestamp the certificate becomes valid
 * @param notAfter        ISO8601 timestamp the certificate expires
 * @param tags            user-defined tags on the certificate
 * @param created         creation timestamp
 * @param modified        last-modified timestamp
 */
public record Certificate(String name, String ern, String description, String certificate, String subject,
                          String issuer, String serialNumber, String fingerprint, List<String> subjectAltNames,
                          boolean generated, String notBefore, String notAfter, Map<String, String> tags,
                          String created, String modified) {
}
