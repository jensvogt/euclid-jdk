package de.jensvogt.euclid.dto.emm;

/**
 * Response to stop-module and start-module.
 *
 * @param name             the module
 * @param stopped          the desired state now recorded: {@code true} after stop-module,
 *                         {@code false} after start-module
 * @param runningInstances how many instances run right now - the count before the manager has
 *                         acted on its next reconcile, not after
 */
public record ModuleStoppedResponse(String name, boolean stopped, long runningInstances) {
}
