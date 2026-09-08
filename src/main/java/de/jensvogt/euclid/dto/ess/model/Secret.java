package de.jensvogt.euclid.dto.ess.model;

import java.util.Map;

/**
 * Mirrors {@code Euclid::Dto::ESS::Secret} from the Euclid server: everything about a stored secret
 * except the secret itself.
 *
 * <p>The value is deliberately not here. Every ESS action but get-secret works on ciphertext it
 * never looks inside, and this is what those actions answer with - so a listing, a tag change or a
 * rotation reports what happened without the value travelling with it. Only
 * {@link de.jensvogt.euclid.dto.ess.GetSecretResponse} carries a plaintext.
 *
 * @param name             the secret's name, which every ESS action takes
 * @param ern              the secret's ERN
 * @param description      what the secret is for, or {@code null} if none was given
 * @param encryptionKeyErn the ERN of the EKM key the value is encrypted under
 * @param version          how many times the value has been set, starting at 1
 * @param rotated          ISO8601 timestamp the value was last changed. Moving a secret to another
 *                         key does not count: that changes how it is protected, not what it is
 * @param tags             user-defined tags on the secret
 * @param created          creation timestamp
 * @param modified         last-modified timestamp
 */
public record Secret(String name, String ern, String description, String encryptionKeyErn, long version,
                     String rotated, Map<String, String> tags, String created, String modified) {
}
