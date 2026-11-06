package de.jensvogt.euclid.dto.emm;

/**
 * Request naming a single module, shared by stop-module, start-module and restart-module - each
 * takes nothing but the name.
 *
 * @param name the module the action applies to, e.g. {@code "esm"}
 */
public record ModuleNameRequest(String name) {
}
