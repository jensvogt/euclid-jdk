package de.jensvogt.euclid.dto.emm;

/**
 * Response to restart-module.
 *
 * @param name             the module
 * @param runningInstances how many instances the manager will cycle through, one per reconcile
 *                         tick, starting on the next one
 */
public record RestartModuleResponse(String name, long runningInstances) {
}
