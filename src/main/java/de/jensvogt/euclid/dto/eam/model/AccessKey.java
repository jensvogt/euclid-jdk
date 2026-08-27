package de.jensvogt.euclid.dto.eam.model;

/**
 * Mirrors {@code Euclid::Dto::EAM::AccessKey} from the Euclid server: an access key summary, as
 * returned by list-access-keys. Deliberately omits the secret - it is only ever returned once,
 * from create-access-key, and is never stored or echoed back afterward.
 *
 * @param accessKeyId public identifier, e.g. "AKIA..."
 * @param active      whether this key can currently be used to sign requests
 * @param createdAt   creation timestamp, ISO8601
 */
public record AccessKey(String accessKeyId, boolean active, String createdAt) {
}
