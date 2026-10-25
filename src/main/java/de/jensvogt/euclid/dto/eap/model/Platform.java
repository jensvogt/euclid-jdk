package de.jensvogt.euclid.dto.eap.model;

import java.util.List;

/**
 * A platform declaration: the shared objects of a namespace that no application owns - the bucket
 * an FTP server writes into, a topic several services publish to. What it owns is tagged
 * {@code platform:<name>}, and applications reach it with {@code uses}.
 *
 * @param name        the platform's name, {@code "platform"} unless another was given
 * @param namespace   the namespace it belongs to
 * @param owns        ERNs of the objects it owns
 * @param declaration the declaration last applied, as JSON, or {@code null} if it could not be read
 * @param modified    when it was last applied (ISO 8601)
 */
public record Platform(String name, String namespace, List<String> owns, String declaration, String modified) {
}
