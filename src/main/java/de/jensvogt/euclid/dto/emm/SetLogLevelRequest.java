package de.jensvogt.euclid.dto.emm;

/**
 * Request to set the level the manager logs a module's own output at.
 *
 * @param name  the module, e.g. {@code "esm"}
 * @param level {@code "trace"}, {@code "debug"}, {@code "info"}, {@code "warning"}, {@code "error"},
 *              {@code "fatal"} or {@code "off"}; empty to go back to the configured level
 */
public record SetLogLevelRequest(String name, String level) {
}
