package de.jensvogt.euclid.dto.eqs.model;

/**
 * Mirrors {@code Euclid::Dto::EQS::Variant} from the Euclid server: a typed message
 * attribute value, tagged with its {@code type} ("int", "long", "double", "float",
 * "bool", "string" or "binary", the latter base64-encoded in {@code value}).
 *
 * @param type  the value's type tag
 * @param value the value itself
 */
public record Variant(String type, Object value) {
}
