package de.jensvogt.euclid.dto.ekm.model;

import java.util.Map;

/**
 * Mirrors {@code Euclid::Dto::EKM::Key} from the Euclid server: an encryption key's metadata. The
 * key material itself never leaves the server - encrypt and decrypt are actions, not key exports.
 *
 * @param name         the key's ID, a server-generated UUID that encrypt/decrypt and delete-key take
 * @param ern          the key's ERN, which revoke-key and the tag actions take
 * @param description  what the key is for, as given when it was created, or {@code null} if none
 *                     was. The ID says nothing about what a key protects and a key outlives the
 *                     reason it was made, so this is what answers "can this one be deleted?"
 * @param algorithm    the key algorithm, e.g. {@code "AES"}
 * @param length       the key length in bits
 * @param status       lifecycle status: {@code "AVAILABLE"} can encrypt and decrypt,
 *                     {@code "REVOKED"} can only decrypt, {@code "PENDING_DELETION"} can only
 *                     decrypt and disappears once {@code deletionDate} passes
 * @param tags         user-defined tags on the key
 * @param deletionDate ISO8601 timestamp the key becomes unrecoverable, or {@code null} unless the
 *                     key is scheduled for deletion - the server omits the field otherwise
 * @param created      creation timestamp
 * @param modified     last-modified timestamp
 */
public record Key(String name, String ern, String description, String algorithm, long length, String status,
                  Map<String, String> tags, String deletionDate, String created, String modified) {
}
