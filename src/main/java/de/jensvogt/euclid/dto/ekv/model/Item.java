package de.jensvogt.euclid.dto.ekv.model;

import java.util.Map;

/**
 * One stored record: an ordinary JSON object, nested as deeply as you like, together with when it
 * was written.
 *
 * <p>There are no type annotations to write and none to read back, and the types survive the round
 * trip - a number comes back a number, {@code 3} does not become {@code 3.0}, and an empty object
 * stays an empty object. Values are what Jackson maps JSON to: {@link String}, {@link Integer} or
 * {@link Long}, {@link Double}, {@link Boolean}, {@link java.util.List} and nested {@link Map}, and
 * {@code null} for an attribute that is present and empty - which the store keeps distinct from one
 * that is absent.
 *
 * <p>{@link #created()} and {@link #modified()} are not in {@link #attributes()}. The server adds
 * them to every item it answers with, under the names {@code _created} and {@code _modified}, and
 * they are lifted out here so that an item read, changed and written back does not acquire two
 * attributes it never had. An attribute genuinely called {@code _created} is shadowed by this, on
 * the server as much as here - the leading underscore is a convention rather than a guarantee,
 * since {@code $} and {@code .} are refused in attribute names but an underscore is not.
 *
 * @param attributes the item as it was written, without the two timestamps
 * @param created    ISO8601 timestamp the item was first written
 * @param modified   ISO8601 timestamp the item was last written. {@code put-item} replaces rather
 *                   than merges, so this moves whenever any part of the item does
 */
public record Item(Map<String, Object> attributes, String created, String modified) {

    /**
     * Reads one attribute.
     *
     * @param name the attribute name
     * @return its value, or {@code null} if the item has no such attribute - or has it and it is
     *         null, which this does not distinguish; use {@code attributes().containsKey(name)} for
     *         that
     */
    public Object get(String name) {
        return attributes == null ? null : attributes.get(name);
    }
}
