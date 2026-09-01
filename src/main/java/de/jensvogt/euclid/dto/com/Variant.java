package de.jensvogt.euclid.dto.com;

/**
 * Mirrors {@code Euclid::Dto::COM::Variant} from the Euclid server: a typed value, tagged with its
 * {@code type} ("int", "long", "double", "float", "bool", "string" or "binary", the latter
 * base64-encoded in {@code value}).
 * <p>
 * Shared rather than owned by one module - a queue message attribute, a topic message attribute and
 * a storage object attribute are all the same typed value on the wire, which is why the server moved
 * this out of {@code Euclid::Dto::EQS} and into {@code Euclid::Dto::COM}.
 *
 * @param type  the value's type tag
 * @param value the value itself
 */
public record Variant(String type, Object value) {
}
