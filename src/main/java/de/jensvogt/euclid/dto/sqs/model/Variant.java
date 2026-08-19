package de.jensvogt.euclid.dto.sqs.model;

/**
 * Mirrors {@code Euclid::Dto::SQS::Variant} from the Euclid server: a typed message
 * attribute value, tagged with its {@code type} ("int", "long", "double", "float",
 * "bool", "string" or "binary", the latter base64-encoded in {@code value}).
 */
public record Variant(String type, Object value) {
}
