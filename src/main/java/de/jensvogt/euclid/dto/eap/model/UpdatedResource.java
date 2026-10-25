package de.jensvogt.euclid.dto.eap.model;

import java.util.List;

/**
 * An object that already existed and had a declared setting brought into line by an apply.
 *
 * @param ern     the object's ERN
 * @param changes one line per setting changed, e.g. {@code visibility 30 -> 60}
 */
public record UpdatedResource(String ern, List<String> changes) {
}
