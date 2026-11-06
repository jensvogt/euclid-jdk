package de.jensvogt.euclid.dto.emm;

/**
 * Response to set-log-level.
 *
 * @param name     the module, lower-cased if it was found only that way
 * @param logLevel the canonical level recorded, or empty for the configured default
 * @param channel  the manager's log channel the level applies to, {@code "module.<name>"}
 */
public record SetLogLevelResponse(String name, String logLevel, String channel) {
}
